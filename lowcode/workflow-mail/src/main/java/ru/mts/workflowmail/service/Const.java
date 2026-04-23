package ru.mts.workflowmail.service;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

public interface Const {

  @RequiredArgsConstructor
  enum ApplicationInstance {
    ID(UUID.randomUUID());
    private final UUID value;
  }

  static UUID getApplicationInstanceId() {
    return ApplicationInstance.ID.value;
  }

  String NEW_LINE = "\r\n";
  String DEFAULT_TENANT_ID = "default";

  interface IntegrationDirection {
     String INPUT = "input";
     String OUTPUT = "output";
     List<String> POSSIBLE_VALUES = List.of(INPUT, OUTPUT);
  }

  interface StarterJsonSchema {
    //TODO
    String MAIL_JSON = "{\"type\":\"object\",\"required\":[\"headers\",\"key\",\"payload\",\"topic\"],\"properties\":{\"topic\":{\"type\":\"string\"},\"key\":{\"type\":[\"string\",\"object\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]},\"headers\":{\"type\":\"object\"},\"payload\":{\"type\":[\"object\",\"string\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]}}}";
  }

  public interface SortingDirection {
     String ASC = "asc";
     String DESC = "desc";
     List<String> POSSIBLE_VALUES = List.of(ASC, DESC);
  }

  public interface StarterSortingFields {
    String CREATE_TIME = "createTime";
    String DESIRED_STATUS = "desiredStatus";
    List<String> POSSIBLE_VALUES = List.of(CREATE_TIME, DESIRED_STATUS);
  }

  public interface MailWorkerStatus {
     String SCHEDULED_TO_START = "scheduled_to_start";
     String STARTED = "started";
     String ERROR = "error";
     String SCHEDULED_TO_RESTART = "scheduled_to_restart";
     String SCHEDULED_TO_TERMINATE = "scheduled_to_terminate";
     String TERMINATED = "terminated";
     String SCHEDULED_TO_DELETE = "scheduled_to_delete";
     String DELETED = "deleted";
     List<String> POSSIBLE_VALUES = List.of(SCHEDULED_TO_START, STARTED, ERROR, SCHEDULED_TO_RESTART, SCHEDULED_TO_TERMINATE, TERMINATED, SCHEDULED_TO_DELETE);
  }

  interface MailConnectionProtocol {
    String IMAP = "imap";
    String EWS = "ews";
    List<String> POSSIBLE_VALUES = List.of(IMAP, EWS);
  }

  public interface MailStarterStatus {
    String STARTED = "started";
    String TERMINATED = "terminated";
    String ERROR = "error";
    String UNKNOWN = "unknown";
    String DELETED = "deleted";
    List<String> POSSIBLE_VALUES = List.of(STARTED, TERMINATED, ERROR, DELETED, UNKNOWN);
  }

  public interface IntegrationKind {
     String SAP = "sap";
     String DATABASE = "database";
     String REST = "rest";
     String RABBIT = "rest";
     String KAFKA = "kafka";
     List<String> POSSIBLE_VALUES = List.of(SAP, DATABASE, REST, RABBIT, KAFKA);
  }

  public interface ExecutionType {
     String SEQUENTIAL = "sequantial";
  }

  public interface ErrorLevel{
     String CRITICAL = "CRITICAL";
     String WARNING = "WARNING";
  }

  public interface ScriptType {
     String JP = "jp";
     String SPEL = "spel";
     List<String> POSSIBLE_VALUES = List.of(JP, SPEL);
  }

  public interface RestCallMethod {
     String GET = "GET";
     String POST = "POST";
     String PATCH = "PATCH";
     String PUT = "PUT";
     String HEAD = "HEAD";
     String DELETE = "DELETE";
     String OPTIONS = "OPTIONS";
     String TRACE = "TRACE";
     List<String> POSSIBLE_VALUES = List.of(GET, POST, PATCH, PUT, HEAD, DELETE, OPTIONS, TRACE);
  }

  public interface AuthType {
     String OAUTH2 = "oauth2";
     String BASIC = "basic";
     List<String> POSSIBLE_VALUES = List.of(OAUTH2, BASIC);
  }

  public interface URLProtocol {
     String HTTP = "http";
     String HTTPS = "https";
     List<String> POSSIBLE_VALUES = List.of(HTTP, HTTPS);
  }

  public interface GrantType {
     String CLIENT_CREDENTIALS = "client_credentials";
     List<String> POSSIBLE_VALUES = List.of(CLIENT_CREDENTIALS);
  }

  public interface SslTrustStoreType {
     String PEM = "PEM";
     List<String> POSSIBLE_VALUES = List.of(PEM);
  }

  interface MailAuthType {
     String SASL = "SASL";
     String TLS = "TLS";
     List<String> POSSIBLE_VALUES = List.of(SASL, TLS);
  }

  public interface SaslProtocol {
     String SASL_SSL = "SASL_SSL";
     String SASL_PLAINTEXT = "SASL_PLAINTEXT";
     List<String> POSSIBLE_VALUES = List.of(SASL_SSL, SASL_PLAINTEXT);
  }

  public interface SaslMechanism {
     String SCRAM_SHA_512 = "SCRAM-SHA-512";
     String OAUTHBEARER = "OAUTHBEARER";
     List<String> POSSIBLE_VALUES = List.of(SCRAM_SHA_512, OAUTHBEARER);
  }

  public interface WorkflowType {
     String SEND_TO_KAFKA = "send_to_kafka";
  }


  interface StarterType {
    String KAFKA_CONSUMER = "kafka_consumer";
    String RABBITMQ_CONSUMER = "rabbitmq_consumer";
    String SAP_INBOUND = "sap_inbound";
    String SCHEDULER = "scheduler";
    String REST_CALL = "rest_call";
    String MAIL_CONSUMER = "mail_consumer";
    List<String> POSSIBLE_VALUES = List.of(SAP_INBOUND, SCHEDULER, REST_CALL, KAFKA_CONSUMER,
        RABBITMQ_CONSUMER, MAIL_CONSUMER);
  }

}
