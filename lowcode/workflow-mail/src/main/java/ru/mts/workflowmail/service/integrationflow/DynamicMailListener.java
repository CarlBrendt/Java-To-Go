package ru.mts.workflowmail.service.integrationflow;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Authenticator;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import microsoft.exchange.webservices.data.core.ExchangeService;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mail.ImapMailReceiver;
import org.springframework.integration.mail.dsl.Mail;
import ru.mts.workflowmail.config.EWSConfig;
import ru.mts.workflowmail.config.EngineConfigurationProperties;
import ru.mts.workflowmail.controller.dto.Worker;
import ru.mts.workflowmail.engine.WorkflowEngine;
import ru.mts.workflowmail.exception.MailConnectionException;
import ru.mts.workflowmail.service.Const;
import ru.mts.workflowmail.service.CustomSSLSocketFactory;
import ru.mts.workflowmail.service.StarterScriptValidationService;
import ru.mts.workflowmail.service.blobstorage.BlobStorage;
import ru.mts.workflowmail.service.dto.MailConnection;
import ru.mts.workflowmail.service.dto.MailConnection.MailAuth;
import ru.mts.workflowmail.service.dto.MailPollConfig;
import ru.mts.workflowmail.starter.MailListenerContainerWrapper;

import java.util.Optional;
import java.util.Properties;

import static ru.mts.workflowmail.service.Const.MailConnectionProtocol;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DynamicMailListener {
  private final WorkflowEngine workflowEngine;
  private final BlobStorage blobStorage;
  private final EngineConfigurationProperties props;
  private final StarterScriptValidationService starterScriptValidationService;
  private final static String IMAPS = "imaps";
  private final static String IMAP = "imap";

  public MailListenerContainerWrapper addMailListener(Worker worker) {
    var starter = worker.getStarter();
    var details = starter.getMailConsumer();
    var con = details.getConnectionDef();
    return switch (con.getProtocol()) {
      case MailConnectionProtocol.IMAP -> getImapFlow(worker, IMAPS);
      case MailConnectionProtocol.EWS -> getEwsFlow(worker);
      default -> throw new IllegalArgumentException("Unknown protocol: " + con.getProtocol());
    };
  }

  private MailListenerContainerWrapper getEwsFlow(Worker worker) {
    var starterDetails = worker.getStarter().getMailConsumer();
    var connection = starterDetails.getConnectionDef();
    var pollConfig = starterDetails.getPollConfig();
    var host = connection.getHost();
    var mailAuth = Optional.ofNullable(connection.getMailAuth()).orElse(new MailAuth());
    ExchangeService exchangeService = new EWSConfig().exchangeService(mailAuth, host);

    var maxFetchSize = Optional.ofNullable(pollConfig).map(MailPollConfig::getMaxFetchSize).orElse(props.getMaxMessageFetchSize());
    var fixedDelay = 1000 * Optional.ofNullable(pollConfig).map(MailPollConfig::getPollDelaySeconds).orElse(props.getPollMessageDelaySeconds());


    ExceptionHolder exceptionHolder = new ExceptionHolder();
    var source = new ExchangeMessageSource(worker, exchangeService, maxFetchSize, exceptionHolder);

    var exchangeMessageProcessor = new ExchangeMessageProcessor(worker, props, starterScriptValidationService,
        workflowEngine, blobStorage, exceptionHolder);
    var flow = IntegrationFlow.from(source,e -> e.poller(Pollers.fixedDelay(fixedDelay)))
        .split()
        .handle(exchangeMessageProcessor)
        .get();

    return new MailListenerContainerWrapper(worker, flow, exchangeService, exceptionHolder);
  }

  private String createUrl(MailConnection connection, String storeProtocol) {
    return storeProtocol + "://" + connection.getHost() + ":" + connection.getPort() + "/INBOX";
  }

  public MailListenerContainerWrapper getImapFlow(Worker worker, String storeProtocol) {
    try {
      var starter = worker.getStarter();
      var details = starter.getMailConsumer();
      var connection = details.getConnectionDef();
      var mailAuth = connection.getMailAuth();
      var mailFilter = details.getMailFilter();
      var maxFetchSize = Optional.ofNullable(details.getPollConfig()).map(MailPollConfig::getMaxFetchSize).orElse(props.getMaxMessageFetchSize());
      String url = createUrl(connection, storeProtocol);

      ImapMailReceiver mailReceiver = new ImapMailReceiver(url);
      mailReceiver.setMaxFetchSize(maxFetchSize);
      mailReceiver.setShouldMarkMessagesAsRead(false);
      //загружать простое содержимое (text/plain или text/html) без ленивой загрузки
      mailReceiver.setSimpleContent(true);
      mailReceiver.setSearchTermStrategy(new SearchTermStrategyImpl(mailFilter));

      Properties properties = toMailProperties(connection, storeProtocol);
      Session session = Session.getInstance(properties, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(mailAuth.getUsername(), mailAuth.getPassword());
        }
      });

      testConnection(session, storeProtocol);
      mailReceiver.setSession(session);

      var channel = new DirectChannel();
      var channelAdapterSpec =
          Mail.imapIdleAdapter(mailReceiver).autoStartup(true).outputChannel(channel);
      var exceptionHolder =  new ExceptionHolder();
      var imapMessageProcessor = new ImapMessageProcessor(worker, props, starterScriptValidationService,
          workflowEngine, blobStorage, exceptionHolder);
      var flow = IntegrationFlow.from(channelAdapterSpec)
          .handle(imapMessageProcessor)
          .get();
      return new MailListenerContainerWrapper(worker, flow, exceptionHolder);
    } catch (MailConnectionException e) {
      if ("imaps".equalsIgnoreCase(storeProtocol)) {
        return getImapFlow(worker, IMAP);
      }
      throw new RuntimeException(e);
    } catch (AuthenticationFailedException ae){
      throw new RuntimeException("Authentication failed", ae);
    }
  }

  private void testConnection(Session session, String storeProtocol) throws MailConnectionException, AuthenticationFailedException {
    try(var store = session.getStore(storeProtocol)) {
      store.connect();
      if (!store.isConnected()) {
        throw new MailConnectionException("Failed to connect to mail server");
      }
    }catch (AuthenticationFailedException ae){
      throw ae;
    } catch (MessagingException e) {
      throw new MailConnectionException(e);
    }
  }

  @SneakyThrows
  private Properties toMailProperties(MailConnection connection, String storeProtocol) {
    Properties properties = new Properties();

    properties.put("mail." + storeProtocol + ".host", connection.getHost());
    properties.put("mail." + storeProtocol + ".port", String.valueOf(connection.getPort()));
    properties.put("mail.store.protocol", storeProtocol);
    properties.put("mail." + storeProtocol + ".ssl.enable", IMAPS.equalsIgnoreCase(storeProtocol)? "true" : "false");
    properties.put("mail." + storeProtocol + ".ssl.starttls.enable", IMAP.equalsIgnoreCase(storeProtocol)? "true" : "false");
    properties.put("mail." + storeProtocol + ".connectiontimeout", "10000");
    properties.put("mail." + storeProtocol + ".timeout", "15000");
    properties.put("mail.debug", "false");

    Optional.ofNullable(connection)
        .map(MailConnection::getMailAuth)
        .map(MailAuth::getCertificate)
        .map(cert -> toMailProperties(cert, storeProtocol))
        .ifPresent(properties::putAll);
    return properties;
  }


  private Properties toMailProperties(MailConnection.MailCertificate auth, String storeProtocol) {
    Properties properties = new Properties();
    if (Const.SslTrustStoreType.PEM.equals(auth.getTrustStoreType())) {
      var socketFactory = CustomSSLSocketFactory.create(auth.getTrustStoreCertificates());
      properties.put("mail." + storeProtocol + ".ssl.socketFactory", socketFactory);
    }
    return properties;
  }

}
