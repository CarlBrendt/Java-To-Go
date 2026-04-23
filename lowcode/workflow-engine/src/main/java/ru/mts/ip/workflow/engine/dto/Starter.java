package ru.mts.ip.workflow.engine.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.dto.KafkaConnection.KafkaAuth;
import ru.mts.ip.workflow.engine.dto.KafkaConnection.SaslAuth;
import ru.mts.ip.workflow.engine.dto.KafkaConnection.Ssl;
import ru.mts.ip.workflow.engine.dto.KafkaConnection.Tls;
import ru.mts.ip.workflow.engine.service.dto.WorkerDto;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
public class Starter {
  private UUID id;
  private List<String> tags;
  private String type;
  private String name;
  private String tenantId;
  private String description;
  private String desiredStatus;
  private String actualStatus;
  private OffsetDateTime createTime;
  private OffsetDateTime changeTime;
  private OffsetDateTime startDateTime;
  private OffsetDateTime endDateTime;
  private UUID workflowDefinitionToStartId;
  private JsonNode workflowInputValidateSchema;
  private SapInboundStarter sapInbound;
  private KafkaConsumer kafkaConsumer;
  private SchedulerStarterConfig scheduler;
  private RabbitmqConsumer rabbitmqConsumer;
  private MailConsumer mailConsumer;
  private IbmmqConsumer ibmmqConsumer;
  private WorkerDto worker;

  public void removeCredentials() {
    Optional.ofNullable(kafkaConsumer).ifPresent(KafkaConsumer::removeCredentials);
    Optional.ofNullable(sapInbound).ifPresent(SapInboundStarter::removeCredentials);
    Optional.ofNullable(rabbitmqConsumer).ifPresent(RabbitmqConsumer::removeCredentials);
    Optional.ofNullable(mailConsumer).ifPresent(MailConsumer::removeCredentials);
    Optional.ofNullable(ibmmqConsumer).ifPresent(IbmmqConsumer::removeCredentials);
  }
  
  public Set<String> findSecrets(){
    Set<String> res = new HashSet<>();
    var props = Optional.ofNullable(sapInbound)
        .map(SapInboundStarter::getInboundDef)
        .map(SapInbound::getConnectionDef)
        .map(SapConnection::getProps);
    props.map(p -> p.get("jco.client.user")).filter(JsonNode::isTextual).map(JsonNode::asText).ifPresent(res::add);
    props.map(p -> p.get("jco.client.passwd")).filter(JsonNode::isTextual).map(JsonNode::asText).ifPresent(res::add);
    //kafka
    var kafkaConnection = Optional.ofNullable(kafkaConsumer)
        .map(KafkaConsumer::getConnectionDef);
    kafkaConnection.map(KafkaConnection::getBootstrapServers).ifPresent(res::add);
    var sasl = kafkaConnection.map(KafkaConnection::getAuthDef).map(KafkaAuth::getSasl);
    var tls = kafkaConnection.map(KafkaConnection::getAuthDef).map(KafkaAuth::getTls);
    sasl.map(SaslAuth::getPassword).ifPresent(res::add);
    sasl.map(SaslAuth::getUsername).ifPresent(res::add);
    sasl.map(SaslAuth::getSslDef).map(Ssl::getTrustStoreCertificates).ifPresent(res::add);
    tls.map(Tls::getTrustStoreCertificates).ifPresent(res::add);
    tls.map(Tls::getKeyStoreCertificates).ifPresent(res::add);
    tls.map(Tls::getKeyStoreKey).ifPresent(res::add);

    //rabbitmq
    var rabbitmqConnection = Optional.ofNullable(rabbitmqConsumer)
        .map(RabbitmqConsumer::getConnectionDef);
    rabbitmqConnection.map(RabbitmqConnection::getAddresses).ifPresent(res::addAll);
    rabbitmqConnection.map(RabbitmqConnection::getUserName).ifPresent(res::add);
    rabbitmqConnection.map(RabbitmqConnection::getUserPass).ifPresent(res::add);
    rabbitmqConnection.map(RabbitmqConnection::getVirtualHost).ifPresent(res::add);

    //mail
    var mailConnection = Optional.ofNullable(mailConsumer)
        .map(MailConsumer::getConnectionDef);
    var mailAuth = mailConnection.map(MailConsumer.MailConnection::getMailAuth);
    mailConnection.map(MailConsumer.MailConnection::getHost).ifPresent(res::add);
    mailAuth.map(MailConsumer.MailAuth::getUsername).ifPresent(res::add);
    mailAuth.map(MailConsumer.MailAuth::getPassword).ifPresent(res::add);

    var cert = mailAuth.map(MailConsumer.MailAuth::getCertificate);
    cert.map(MailConsumer.MailCertificate::getTrustStoreCertificates).ifPresent(res::add);

    return res;
  }

}
