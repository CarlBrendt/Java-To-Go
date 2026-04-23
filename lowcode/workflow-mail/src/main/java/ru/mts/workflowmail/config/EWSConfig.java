package ru.mts.workflowmail.config;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.request.HttpClientWebRequest;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import org.springframework.context.annotation.Configuration;
import ru.mts.workflowmail.service.dto.MailConnection;

import java.net.URI;
import java.util.Optional;

@Configuration
public class EWSConfig {

  public ExchangeService exchangeService(MailConnection.MailAuth mailAuth) {
    return exchangeService(mailAuth, (URI) null);
  }

  public ExchangeService exchangeService(MailConnection.MailAuth mailAuth, String uriString) {
    try {
      if (uriString == null || uriString.isEmpty()) {
        return exchangeService(mailAuth);
      }
      var uri = URI.create(uriString);
      return exchangeService(mailAuth, uri);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при настройке ExchangeService", e);
    }
  }

  public ExchangeService exchangeService(MailConnection.MailAuth mailAuth, URI uri) {
    ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
    var username = Optional.ofNullable(mailAuth).map(MailConnection.MailAuth::getUsername).orElseThrow(() -> new IllegalArgumentException("username is null"));
    var password = Optional.of(mailAuth).map(MailConnection.MailAuth::getPassword).orElseThrow(() ->new IllegalArgumentException("password is null"));
    service.setCredentials(new WebCredentials(username, password));
    if (uri != null) {
      service.setUrl(uri);
    } else {
      try {
        service.autodiscoverUrl(username, new RedirectionUrlCallback());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return service;
  }

  // Обработка перенаправлений
  private static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
    @Override
    public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
      // Разрешить перенаправление на указанный URL
      return redirectionUrl.startsWith("https://");
    }
  }
}
