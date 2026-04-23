package ru.mts.ip.workflow.engine;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface Const {

  String NEW_LINE = "\r\n";
  String DEFAULT_TENANT_ID = "default";
  String DEFAULT_DEFINTION_OWNER_LOGIN = "system";
  
  String DEFAULT_WORKFLOW_QUEUE = "dsl_workflow";
  String DEFAULT_WORKFLOW_QUEUE_V2 = "dsl_workflow_v2";
  String DEFAULT_REST_CALL_ACTIVITY_QUEUE = "rest_call";
  String DEFAULT_TRANSFORM_ACTIVITY_QUEUE = "transform";
  String DEFAULT_DB_CALL_ACTIVITY_QUEUE = "db_call";
  String DEFAULT_XSLT_TRANSFORM_ACTIVITY_QUEUE = "xslt_transform";
  String DEFAULT_SEND_TO_KAFKA_ACTIVITY_QUEUE = "send_to_kafka";
  String DEFAULT_SEND_TO_S3_ACTIVITY_QUEUE = "send_to_s3";
  String DEFAULT_SEND_TO_SAP_ACTIVITY_QUEUE = "send_to_sap";
  String DEFAULT_SEND_TO_RABBITMQ_ACTIVITY_QUEUE = "send_to_rabbitmq";
  String DEFAULT_FIND_WORKFLOW_DEFINITION_ACTIVITY_QUEUE = "find_workflow_definition";
  String DEFAULT_RESOLVE_EXTERNAL_PROPERTIES_ACTIVITY_QUEUE = "resolve_external_properties";


  enum CsvOutputFormat {
    OBJECTS, ARRAYS, ROWS;

    public static final List<String> POSSIBLE_VALUES = List.of(OBJECTS.name(), ARRAYS.name(), ROWS.name());
    }


  interface WorkflowInstanceStatus {
    
    String RUNNING = "RUNNING";
    String COMPLETED = "COMPLETED";
    String FAILED = "FAILED";
    String CANCELED = "CANCELED";
    String TERMINATED = "TERMINATED";
    String CONTINUED_AS_NEW = "CONTINUED_AS_NEW";
    String TIMED_OUT = "TIMED_OUT";
    List<String> POSIBLE_VALUES = List.of(RUNNING, COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMED_OUT);
    
    Map<String, String> temporalInternalToStatus = Map.of(
        "WORKFLOW_EXECUTION_STATUS_RUNNING", RUNNING,
        "WORKFLOW_EXECUTION_STATUS_COMPLETED", COMPLETED,
        "WORKFLOW_EXECUTION_STATUS_FAILED", FAILED,
        "WORKFLOW_EXECUTION_STATUS_CANCELED", CANCELED,
        "WORKFLOW_EXECUTION_STATUS_TERMINATED", TERMINATED,
        "WORKFLOW_EXECUTION_STATUS_CONTINUED_AS_NEW", CONTINUED_AS_NEW,
        "WORKFLOW_EXECUTION_STATUS_TIMED_OUT", TIMED_OUT
    );

    Map<String, String> statusToTemporalQueryField = Map.of(
        RUNNING, "Running",
        COMPLETED, "Completed",
        FAILED, "Failed",
        CANCELED, "Canceled",
        TERMINATED, "Terminated",
        CONTINUED_AS_NEW, "ContinuedAsNew",
        TIMED_OUT, "TimedOut"
    );
    
    static Optional<String> ofTemporalInternal(String value) {
      return Optional.ofNullable(temporalInternalToStatus.get(value));
    }

    static Optional<String> toTemporalQueryField(String value) {
      return Optional.ofNullable(statusToTemporalQueryField.get(value));
    }
    
  }

  interface TemporalExecutionStatus {
    String RUNNING = "Running";
    String COMPLETED = "Completed";
    String FAILED = "Failed";
    String CANCELED = "Canceled";
    String TERMINATED = "Terminated";
    String CONTINUED_AS_NEW = "ContinuedAsNew";
    String TIMEDOUT = "TimedOut";
    List<String> POSIBLE_VALUES = List.of(RUNNING, COMPLETED, FAILED, CANCELED, TERMINATED, CONTINUED_AS_NEW, TIMEDOUT);
  }

  interface KafkaAuthType {
    String SASL = "SASL";
    String TLS = "TLS";
    List<String> POSSIBLE_VALUES = List.of(SASL, TLS);
  }

  interface SslTrustStoreType {
    String PEM = "PEM";
    List<String> POSIBLE_VALUES = List.of(PEM);
  }

  interface MailConnectionProtocol {
    String IMAP = "imap";
    String EWS = "ews";
    List<String> POSSIBLE_VALUES = List.of(IMAP, EWS);
  }

  interface SaslMechanism {
    String SCRAM_SHA_512 = "SCRAM-SHA-512";
    String OAUTHBEARER = "OAUTHBEARER";
    List<String> POSIBLE_VALUES = List.of(SCRAM_SHA_512, OAUTHBEARER);
  }

  interface SaslProtocol {
    String SASL_SSL = "SASL_SSL";
    String SASL_PLAINTEXT = "SASL_PLAINTEXT";
    List<String> POSIBLE_VALUES = List.of(SASL_SSL, SASL_PLAINTEXT);
  }

  interface ScriptType {
    String JP = "jp";
    String SPEL = "spel";
    List<String> POSIBLE_VALUES = List.of(JP, SPEL);
  }

  interface IntegrationDirection {
    String INPUT = "input";
    String OUTPUT = "output";
    List<String> POSIBLE_VALUES = List.of(INPUT, OUTPUT);
  }
  
  interface IntegrationKind {
    String SAP = "sap";
    String DATABASE = "database";
    String REST = "rest";
    String RABBITMQ = "rabbitmq";
    List<String> POSIBLE_VALUES = List.of(SAP, DATABASE, REST, RABBITMQ);
  }
  
  interface DefinitionStatus {
    String DRAFT = "draft";
    String DEPLOYED = "deployed";
    List<String> POSIBLE_VALUES = List.of(DRAFT, DEPLOYED);
  }

  interface DefinitionAvailabilityStatus {
    String ACTIVE = "active";
    String DECOMMISSIONED = "decommissioned";
  }
  
  interface RetryActivityState {
    String RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED = "RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED";
    String RETRY_STATE_NON_RETRYABLE_FAILURE = "RETRY_STATE_NON_RETRYABLE_FAILURE";
    String RETRY_STATE_TIMEOUT = "RETRY_STATE_TIMEOUT";
    List<String> POSSIBLE_VALUES = List.of(RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED);
  }
  
  interface ActivityType {
    String WORKFLOW_CALL = "workflow_call";
    String INJECT = "inject";
    String SWITCH = "switch";
    String PARALLEL = "parallel";
    String TIMER = "timer";
    List<String> POSIBLE_VALUES = List.of(WORKFLOW_CALL, INJECT, SWITCH, PARALLEL, TIMER);
  }
  
  interface ErrorLevel{
    String CRITICAL = "CRITICAL";
    String WARNING = "WARNING";
  }
  
  interface TransformType {
    String XML_TO_JSON = "xml_to_json";
    String JSON_TO_XML = "json_to_xml";
    List<String> POSIBLE_VALUES = List.of(XML_TO_JSON, JSON_TO_XML);
  }
  
  interface CompletionType {
    String ALLOF = "allOf";
    String ANYOF = "anyOf";
    List<String> POSIBLE_VALUES = List.of(ALLOF, ANYOF);
  }
  
  interface StarterType {
    String KAFKA_CONSUMER = "kafka_consumer";
    String RABBITMQ_CONSUMER = "rabbitmq_consumer";
    String SAP_INBOUND = "sap_inbound";
    String SCHEDULER = "scheduler";
    String REST_CALL = "rest_call";
    String MAIL_CONSUMER = "mail_consumer";
    String IBMMQ_CONSUMER = "ibmmq_consumer";
    List<String> POSSIBLE_VALUES = List.of(SAP_INBOUND, SCHEDULER, REST_CALL, KAFKA_CONSUMER,
        RABBITMQ_CONSUMER, MAIL_CONSUMER, IBMMQ_CONSUMER);
  }

  interface ContextCompilerBeans {
    String KAFKA = "kafkaScriptContextCompiler";
    String RABBITMQ = "rabbitmqScriptContextCompiler";
    String SAP = "sapScriptContextCompiler";
    String SCHEDULER = "schedulerScriptContextCompiler";
    String REST_CALL = "restScriptContextCompiler";
    String MAIL = "mailScriptContextCompiler";
    String IBMMQ = "ibmmqScriptContextCompiler";
    String EMPTY = "emptyScriptContextCompiler";
  }

  @Getter
  enum StarterTypeBeanValidator {
    KAFKA(ContextCompilerBeans.KAFKA, StarterType.KAFKA_CONSUMER),
    RABBITMQ(ContextCompilerBeans.RABBITMQ, StarterType.RABBITMQ_CONSUMER),
    SAP(ContextCompilerBeans.SAP, StarterType.SAP_INBOUND),
    SCHEDULER(ContextCompilerBeans.SCHEDULER, StarterType.SCHEDULER),
    REST_CALL(ContextCompilerBeans.REST_CALL, StarterType.REST_CALL),
    MAIL(ContextCompilerBeans.MAIL, StarterType.MAIL_CONSUMER),
    IBMMQ(ContextCompilerBeans.IBMMQ, StarterType.IBMMQ_CONSUMER),
    ;

    private final String beanName;
    private final String starterType;

    StarterTypeBeanValidator(String beanName, String starterType) {
      this.beanName = beanName;
      this.starterType = starterType;
    }

    public static Optional<StarterTypeBeanValidator> fromStarterTypeName(String starterType) {
      return Arrays.stream(values())
          .filter(type -> type.getStarterType().equals(starterType))
          .findFirst();
    }
  }
  
  interface StarterJsonSchema {
    String SAP_INBOUND = "{\"type\":\"object\",\"properties\":{\"idoc\":{\"type\":\"string\",\"stringFormat\":\"xml\"}},\"required\":[\"idoc\"]}";
    String KAFKA_CONSUMER = "{\"type\":\"object\",\"required\":[\"headers\",\"key\",\"payload\",\"topic\"],\"properties\":{\"topic\":{\"type\":\"string\"},\"key\":{\"type\":[\"string\",\"object\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]},\"headers\":{\"type\":\"object\"},\"payload\":{\"type\":[\"object\",\"string\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]}}}";
    String RABBIT_MQ_CONSUMER = "{\"type\":\"object\",\"required\":[\"payload\",\"queue\",\"headers\",\"properties\"],\"properties\":{\"payload\":{\"type\":[\"object\",\"string\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]},\"queue\":{\"type\":\"string\"},\"headers\":{\"type\":\"object\"},\"properties\":{\"type\":\"object\",\"properties\":{\"content_type\":{\"type\":\"string\"},\"content_encoding\":{\"type\":\"string\"},\"priority\":{\"type\":\"integer\"},\"correlation_id\":{\"type\":\"string\"},\"reply_to\":{\"type\":\"string\"},\"expiration\":{\"type\":\"string\"},\"message_id\":{\"type\":\"string\"},\"timestamp\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"},\"user_id\":{\"type\":\"string\"},\"app_id\":{\"type\":\"string\"},\"cluster_id\":{\"type\":\"string\"}}}}}";
    String IBMMQ_CONSUMER = "{\"type\":\"object\",\"required\":[\"payload\",\"properties\"],\"properties\":{\"payload\":{\"type\":[\"object\",\"string\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]},\"queue\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\",\"required\":[\"priority\",\"expiration\",\"timestamp\"],\"properties\":{\"priority\":{\"type\":\"integer\"},\"correlation_id\":{\"type\":\"string\"},\"reply_to\":{\"type\":\"string\"},\"expiration\":{\"type\":\"string\"},\"message_id\":{\"type\":\"string\"},\"timestamp\":{\"type\":\"string\"},\"type\":{\"type\":\"string\"}}}}}";
    String MAIL_CONSUMER = "{\"type\":\"object\",\"required\":[\"content\",\"contentType\",\"sentDate\",\"receiveDate\",\"senders\",\"recipients\",\"recipientsCopy\",\"subject\"],\"properties\":{\"content\":{\"type\":[\"object\",\"string\",\"number\",\"integer\",\"array\",\"boolean\",\"null\"]},\"contentType\":{\"type\":\"string\"},\"sentDate\":{\"type\":\"string\"},\"receiveDate\":{\"type\":\"string\"},\"senders\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"recipients\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"recipientsCopy\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"subject\":{\"type\":\"string\"}}}";
    String SCHEDULER_CONSUMER = "{}";
    String REST_CALL = "{}";
  }
  
  interface SchedulerType {
    String CRON = "cron";
    String SIMPLE = "simple";
    List<String> POSSIBLE_VALUES = List.of(CRON, SIMPLE);
  }
  
  interface WorkflowType {
    String COMPLEX = "complex";
    String AWAIT_FOR_MESSAGE = "await_for_message";
    String REST_CALL = "rest_call";
    String TRANSFORM = "transform";
    String DB_CALL = "db_call";
    String XSLT_TRANSFORM = "xslt_transform";
    String SEND_TO_KAFKA = "send_to_kafka";
    String SEND_TO_S3 = "send_to_s3";
    String SEND_TO_SAP = "send_to_sap";
    String SEND_TO_RABBITMQ = "send_to_rabbitmq";
    String FIND_WORKFLOW_DEFINITION = "find_workflow_definition";
    String RESOLVE_EXTERNAL_PROPERTIES = "resolve_external_properties";
    String PREPARE_MULTI_INSTANCE_COLLECTION = "prepare_multi_instance_collection";
    String EMPTY = "empty";
    
    final List<String> POSIBLE_VALUES = List.of(
      COMPLEX, 
      AWAIT_FOR_MESSAGE, 
      REST_CALL, 
      TRANSFORM, 
      DB_CALL,
      XSLT_TRANSFORM,
      SEND_TO_SAP,
      SEND_TO_RABBITMQ,
      SEND_TO_KAFKA,
      SEND_TO_S3
    );
  }
  
  
  interface Plant {
    String DOC_START = "@startuml";
    String DOC_END = "@enduml";
    String STOP = "stop";
    String START = "start";
    String LQ_COMMENT = "/'";
    String RQ_COMMENT = "'/";
    String REPEAT = "repeat";
  }
  
  interface Stand {
    String DEV = "dev";
    String TEST = "test";
    String PROD = "prod"; 
  }

  interface QueryType {
    String HISTORY = "hist";
    String ACTIVITY_HISTORY = "activity_hist";
    String WORKFLOW_INITS = "workflow_inits";
    String WORKFLOW_CONSUMED_MESSAGES = "workflow_consumed_messages";
  }

  interface StarterStatus {
    String STARTED = "started";
    String EXPIRED = "expired";
    String TERMINATED = "terminated";
    String ERROR = "error";
    String UNKNOWN = "unknown";
    String DELETED = "deleted";
    List<String> POSSIBLE_VALUES = List.of(STARTED, TERMINATED, ERROR, DELETED, UNKNOWN);
  }

  interface WorkerStatus {
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

  interface IbmmqAuthType {
    String BASIC = "basic";
    List<String> POSSIBLE_VALUES = List.of(BASIC);
  }

  interface StarterSortingFields {
    String CREATE_TIME = "createTime";
    String DESIRED_STATUS = "desiredStatus";
    List<String> POSSIBLE_VALUES = List.of(CREATE_TIME, DESIRED_STATUS);
  }

  interface SortingDirection {
    String ASC = "asc";
    String DESC = "desc";
    List<String> POSSIBLE_VALUES = List.of(ASC, DESC);
  }

  interface Task {
    int INITIAL_RETRY_COUNT = 1000;
    int RETRY_DELAY_MINUTES = 3;
    double RETRY_DELAY_FACTOR = 1.3;
    int MAX_DELAY_MINUTES = 60;
  }
  

  interface EsqlTaskStatus {
    String WAITING = "waiting";
    String IN_PROGRESS = "in_progress";
    String ERROR = "error";
    String COMPLETED = "completed";
    String STARTED = "started";
    List<String> POSIBLE_VALUES = List.of(STARTED, WAITING, IN_PROGRESS, COMPLETED, ERROR);
  }

  interface EsqlTaskEvent {
    String STARTED = "task_started";
    String LUA_SCRIPT_GENERATION_STARTED = "lua_script_generation_started";
    String LUA_SCRIPT_GENERATION_COMPLETED = "lua_script_generation_completed";
    String LUA_SCRIPT_TESTING_STARTED = "lua_script_testing_started";
    String LUA_SCRIPT_TESTING_COMPLETED = "lua_script_testing_completed";
    String LUA_SCRIPT_TUNING_STARTED = "lua_script_tuning_started";
    String LUA_SCRIPT_TUNING_COMPLETED = "lua_script_tuning_completed";
    String COMPLETED = "task_completed";
    String ERROR = "error";
  }

  interface FileExtension {
    String TXT = "txt";
    String XML = "xml";
    String JSON = "json";
    String CSV = "csv";
    String YAML = "yaml";
    String YML = "yml";
    String BINARY = "binary";
    Set<String> TEXT_EXTENSIONS = Set.of(TXT, XML, JSON, CSV, YAML, YML);
  }

  @Getter
  public enum Errors2{
    
    UNEXPECTED("{unexpected}", 777),
    INVALID_BODY_JSON("{invalid.body}", 1),
    FIELD_WRONG_TYPE("{field.wrong.type}", 2),
    FIELD_NULL("{field.null}", 3),
    FIELD_EMPTY("{field.empty}", 4),
    FIELD_BLANK("{field.blank}", 5),
    FIELD_NOT_FILED("{field.not.filed}", 6),
    FIELD_WRONG_VALUE("{field.wrong.value}", 7),
    FILED_NOTHING("{field.nothing}", 8),
    UNKNOWN_FIELD(ErrorLevel.WARNING, "{unused.field}", 9),
    NOT_UNIQUE_ACTIVITY_ID("{not.unique.activity.id}", 10),
    SCRIPT_IS_NOT_EXECUTABLE("{script.is.not.executable}", 11),
    TRANSITION_IS_NOT_FOUND("{transition.is.not.found}", 12),
    DURATION_INVALID_FORMET("{duration.invalid.format}", 13),
    DURATION_NEGATIVE("{duration.negative}", 14),
    INVALID_UUID("{invalid.uuid}", 15),
    
    NEGATIVE_NOT_ALLOWED("{negative.not.allowed}", 16),
    NUMBER_MORE_THAN_ALLOWED("{number.more.than.allowed}", 17),
    
    INVALID_SCRIPT_PLACEHOLDER("{invalid.script.placeholder}", 18),
    EMPTY_SCRIPT("{empty.script}", 19),
    VERSION_WRONG_VALUE("{version.wrong.value}", 20),
    WRONG_HTTP_STATUS_CODE("{wrong.http.status.code}", 21),
    INVALID_ISO_OFFSET_DATE_TIME("{invalid.iso.offset.date.time}", 22),
    INVALID_XML_VALUE("{invalid.xml.value}", 23),
    INVALID_JSON_VALUE("{invalid.json.value}", 24),
    INVALID_OFFSET_DATE_TIME("{invalid.offset.date.time}", 25),
    EXECUTION_TIMEOUT_OVERLIMITED("{execution.timeout.overlimited}", 26),
    SYNC_EXECUTION_TIMEOUT("{sync.execution.timeout}", 27),
    
    SPEL_VARIABLE_IS_NOT_FOUND("{spel.variable.is.not.found}", 101),
    JSON_PATH_NOT_FOUND("{json.path.not.found}", 102),
    SCRIPT_EXECUTION_ERROR("{script.execution.error}", 103),
    UNKNOWN_SCRIPT_SYNTAX("{unknown.script.syntax}", 104),
    INVALID_SECRET_PLACEHOLDER("{invalid.secret.placeholder}", 105),
    INVALID_JSON_SCHEMA("{invalid.json.schema}", 106),
    SPEL_IS_NOT_SUPPORTED("{spel.is.not.supported}", 107),
    INVALID_BASE64_VALUE("{invalid.base64.value}", 120),

    DRAFT_DEFINITION_ALREADY_EXISTS("{draft.definition.already.exists}", 1000),
    WORKFLOW_IS_NOT_FOUND_BY_REF("{workflow.is.not.found.by.ref}", 1001),
    WORKFLOW_IS_NOT_FOUND_BY_ID("{workflow.is.not.found.by.id}", 1002),
    WORKFLOW_RUNNING_NOT_FOUND_BY_BK("{workflow.running.not.found.by.bk}", 1002),
    WORKFLOW_INSTANCE_IS_NOT_FOUND_BY_BK("{workflow.is.not.found.by.bk}", 1003),
    WORKFLOW_ALREADY_STARTED("{workflow.already.started}", 1004),
    DRAFT_IS_NOT_FOUND_BY_ID("{draft.is.not.found.by.id}", 1005),
    INVALID_PLANT("{invalid.plant}", 1006),
    PRIMITIVE_PRECONDITION_ERROR("{primitive.precondition.error}", 1010),
    INFINITY_CYCLES("{infinity.cycles}", 1011),
    RUNTIME_INFINITY_CYCLES("{runtime.infinity.cycles}", 1012),
    WORKFLOW_ALREADY_EXISTS("{workflow.already.exists}", 1013),
    MOCK_IS_NOT_FOUND("{mock.is.not.found}", 1014),
    ACCESS_DENIED_TO_START_WORKFLOW("{access.denied.to.start.workflow}", 1015),
    ACCESS_DENIED_TO_START_DECOMMISSIONED_WORKFLOW("{access.denied.to.start.decommissioned.workflow}", 1016),
    JWT_NOT_FOUND("{access.denied.to.start.jwt.not.found}", 1017),
    JWT_CLIENTID_IS_NULL("{access.denied.to.start.jwt.is.null}", 1018),
    JWT_CLIENTID_NOT_IN_ACCESS_LIST("{access.denied.to.start.jwt.not.in.access.list}", 1019),
    SECRET_FIELD_IS_NOT_FOUND("{secret.field.is.not.found}", 1020),
    SECRET_IS_NOT_FOUND("{secret.is.not.found}", 1020),
    SECRET_PERMISSION_DENIED("{secret.permission.denied}", 1021),
    SECRET_INVALID_PATH("{secret.invalid.path}", 1022),
    VARIABLE_HAS_NAME_AS_ACTIVITY("{variable.has.name.as.activity}", 1023),
    INVALID_PAGE_TOKEN("{invalid.page.token}", 1024),
    WORKFLOW_INSTANCE_IS_NOT_FOUND("{workflow.instance.is.not.found}", 1025),
    OUTPUT_FILTER_IS_NOT_REDEFINED_IN_FAIL("{output.filter.is.not.redefined.in.fail}", 1026),
    WORKFLOW_CHANGE_AVAILABILITY_CONFLICT("{workflow.status.already.changed}", 1027),
    INVALID_OUTPUT_SCHEMA_VALIDATION("{invalid.output.schema.validation}", 1028),
    WORKER_IS_NOT_FOUND_BY_ID("{worker.is.not.found.by.id}", 1029),
    STARTER_ALREADY_EXISTS("{starter.already.exists}", 1030),
    STARTER_IS_NOT_FOUND_BY_ID("{starter.is.not.found.by.id}", 1031),
    STARTER_IS_NOT_FOUND_BY_STOP("{starter.is.not.found.by.stop}", 1032),
    STARTER_TASK_IS_NOT_FOUND_BY_ID("{starter.task.is.not.found.by.id}", 1033),
    STARTER_ALREADY_DELETED("{starter.already.deleted}", 1034),
    INVALID_OUTPUT_TEMPLATE("{invalid.output.template}", 1035),
    INVALID_HOST_PORT("{invalid.hostport}", 1036),
    INVALID_EMAIL("{invalid.email}", 1037),
    INVALID_CRON_MINUTES("{invalid.cron.minutes}", 1038),
    INVALID_CRON_HOUR("{invalid.cron.hour}", 1039),
    INVALID_CRON_DAY_OF_MONTH("{invalid.cron.dayofmonth}", 1040),
    INVALID_CRON_DAY_OF_WEEK("{invalid.cron.dayofweek}", 1041),
    INVALID_CRON_MONTH("{invalid.cron.month}", 1042),
    INVALID_XSD_SCHEMA("{invalid.xsd.schema}", 1043),
    XSD_VALIDATION_FAILURE("{xsd.validation.failure}", 1044),
    ESQL_COMPILED_LUA_FAILED("{esql.compiled.lua.failed}", 1050),
    INVALID_START_VARIABLES("{invalid.start.variables}", 1051),
    WORKER_NOT_FOUND_BY_ID_AND_EXECUTION_ID("{worker.not.found.by.id.and.executionid}", 1052),
    YAML_VALIDATION_FAILURE("{yaml.validation.failure}", 1053),
    INVALID_YAML_STRUCTURE("{invalid.yaml.structure}", 1054),
    ;

    Errors2(String level, String errorAlias, int number) {
      this.level = level;
      code = "LC-%04d".formatted(number);
      errorMessageAlias = errorAlias;
      solvingMessageAlias = "{%s.solving.advice}".formatted(errorAlias.substring(1, errorAlias.length() - 1));
    }
    
    Errors2(String errorAlias, int number) {
      this(ErrorLevel.CRITICAL, errorAlias, number);
    }
    
    public boolean isCritical() {
      return ErrorLevel.CRITICAL.equals(level);
    }
    
    final String code;
    final String level;
    final String errorMessageAlias;
    final String solvingMessageAlias;
    
    public static Errors2 findByMessageAlias(String alias) {
      return  Stream.of(Errors2.values()).filter(e -> e.errorMessageAlias.equals(alias)).findFirst().orElse(UNEXPECTED);
    } 
    
    public static Errors2 findByCode(String code) {
      return Stream.of(Errors2.values()).filter(e -> e.code.equals(code)).findFirst().orElse(UNEXPECTED);
    }
    
  }
}
