package ru.mts.ip.workflow.engine.validation;

import static ru.mts.ip.workflow.engine.validation.ValidationHelper.*;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Const;

@RequiredArgsConstructor
public enum Constraint {

  TYPE_ARRAY(typeOf(JsonNodeType.ARRAY)),
  TYPE_OBJECT(typeOf(JsonNodeType.OBJECT)),
  TYPE_STRING(typeOf(JsonNodeType.STRING)),
  TYPE_NUMBER(typeOf(JsonNodeType.NUMBER)),
  TYPE_BOOLEAN(typeOf(JsonNodeType.BOOLEAN)),
  FILLED(filed()),
  NOT_NULL(notNull()),
  NOT_BLANK(notBlank()),
  ACCEPTABLE_IBMMQ_AUTH_TYPE(anyOf(Const.IbmmqAuthType.POSSIBLE_VALUES)),
  NOT_EMPTY_ARRAY(notEmpty()),
  TRANSITION_EXISTS(transitionExists()),
  UNIQUE_ACTIVITY_ID(uniqueActivityId()),
  ACCEPTABLE_ACTIVITY_TYPE(anyOf(Const.ActivityType.POSIBLE_VALUES)),
  ACCEPTABLE_WORKFLOW_ISNANCE_STATUS(anyOf(Const.WorkflowInstanceStatus.POSIBLE_VALUES)),
  ACCEPTABLE_COMPLETION_TYPE(anyOf(Const.CompletionType.POSIBLE_VALUES)),
  ACCEPTABLE_WORKFLOW_TYPE(anyOf(Const.WorkflowType.POSIBLE_VALUES)),
  ACCEPTABLE_STARTER_TYPE(anyOf(Const.StarterType.POSSIBLE_VALUES)),
  ACCEPTABLE_TRANSFORM_TYPE(anyOf(Const.TransformType.POSIBLE_VALUES)),
  ACCEPTABLE_DEFINITION_STATUS(anyOf(Const.DefinitionStatus.POSIBLE_VALUES)),
  ACCEPTABLE_RETRY_STATES(anyOf(Const.RetryActivityState.POSSIBLE_VALUES)),
  ACCEPTABLE_VERSION(acceptableVersion()),
  ACCEPTABLE_HTTP_STATUS_CODE(acceptableStatusCode()),
  KEYS_NOT_EQUAL_ACTIVITY_ID(keysNotEqualActivityId()),
  VALID_DURATION(validDuration()),
  DURATION_NOT_NEGATIVE(durationNotNegative()),
  DURATION_SYNC_START_TIMIOUT_LIMIT(syncStartTimioutLimit()),
  VALID_UUID(validUUID()),
  WORKFLOW_EXISTS_BY_REF(workflowExistsByRef()),
  VALID_JSON_SCHEMA(validJsonSchema()),
  VALID_XSD_SCHEMA(validXsdSchema()),
  VALID_BASE64_VALUE(validBase64Value()),
  VALID_ACTIVITY(validActivity()),
  VALID_WORKFLOW_EXPRESSION(validWorkflowExpression()),
  VALID_WORKFLOW_EXPRESSION_FOR_ESQL_COMPILATION(validWorkflowExpressionForEsqlCompilation()),
  STARTER_COMPATIBLE_WITH_DEFINITION(starterCompatibleWithDefinition()),
  VALID_BOOTSTRAP_ADDRESS(validBootstrapAddress()),
  VALID_OUTPUT_TEMPLATE_VALUE(validOutputTemplateValue()),

  ACCEPTABLE_SCHEDULER_TYPE(anyOf(Const.SchedulerType.POSSIBLE_VALUES)),
  VALID_OFFSET_DATE_TIME(validOffsetDateTime()),
  ACCEPTABLE_KAFKA_AUTH_TYPE(anyOf(Const.KafkaAuthType.POSSIBLE_VALUES)),
  ACCEPTABLE_SASL_MECHANISM(anyOf(Const.SaslMechanism.POSIBLE_VALUES)),
  ACCEPTABLE_SASL_PROTOCOL(anyOf(Const.SaslProtocol.POSIBLE_VALUES)),
  ACCEPTABLE_SSL_TRUST_STORE_TYPE(anyOf(Const.SslTrustStoreType.POSIBLE_VALUES)),
  ACCEPTABLE_MAIL_PROTOCOL(anyOf(Const.MailConnectionProtocol.POSSIBLE_VALUES)),

  NOT_NEGATIVE(notNegative()),
  MAX_100(maximum(100L)),
  VALID_ISO_OFFSET_DATE_TIME(validIsoOffsetDateTime()),
  VALID_PAGE_TOKEN(validNextPageToken()),
  ACCEPTABLE_WORKER_STATUSES(anyOf(Const.WorkerStatus.POSSIBLE_VALUES)),
  ACCEPTABLE_STARTER_SORTING_FIELD(anyOf(Const.StarterSortingFields.POSSIBLE_VALUES)),
  ACCEPTABLE_SORTING_DIRECTION(anyOf(Const.SortingDirection.POSSIBLE_VALUES)),
  ACCEPTABLE_STARTER_STATUSES(anyOf(Const.StarterStatus.POSSIBLE_VALUES)),

  VALID_EMAIL(validEmail()),

  CRON_MIN(validCronMin()),
  CRON_HOUR(validCronHour()),
  CRON_MONTH(validCronMonth()),
  CRON_DAY_OF_MONTH(validCronDayOfMonth()),
  CRON_DAY_OF_WEEK(validCronDayOfWeek())
  ;

  @Getter
  private final Validation validation;
  
}
