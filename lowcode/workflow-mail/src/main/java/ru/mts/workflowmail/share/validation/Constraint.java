package ru.mts.workflowmail.share.validation;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.mts.workflowmail.service.Const;

import static ru.mts.workflowmail.share.validation.ValidationHelper.anyOf;
import static ru.mts.workflowmail.share.validation.ValidationHelper.durationNotNegative;
import static ru.mts.workflowmail.share.validation.ValidationHelper.executableScript;
import static ru.mts.workflowmail.share.validation.ValidationHelper.filled;
import static ru.mts.workflowmail.share.validation.ValidationHelper.maximum;
import static ru.mts.workflowmail.share.validation.ValidationHelper.notBlank;
import static ru.mts.workflowmail.share.validation.ValidationHelper.notEmpty;
import static ru.mts.workflowmail.share.validation.ValidationHelper.notNegative;
import static ru.mts.workflowmail.share.validation.ValidationHelper.notNull;
import static ru.mts.workflowmail.share.validation.ValidationHelper.typeOf;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validCurl;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validDuration;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validEmail;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validJsonSchema;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validOffsetDateTime;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validOutputTemplateValue;
import static ru.mts.workflowmail.share.validation.ValidationHelper.validUUID;

@Getter
@RequiredArgsConstructor
public enum Constraint {

  TYPE_ARRAY(typeOf(JsonNodeType.ARRAY)),
  TYPE_OBJECT(typeOf(JsonNodeType.OBJECT)),
  TYPE_STRING(typeOf(JsonNodeType.STRING)),
  TYPE_BOOLEAN(typeOf(JsonNodeType.BOOLEAN)),
  TYPE_NUMBER(typeOf(JsonNodeType.NUMBER)),
  FILLED(filled()),
  NOT_NULL(notNull()),
  NOT_BLANK(notBlank()),
  NOT_EMPTY_ARRAY(notEmpty()),
  VALID_DURATION(validDuration()),
  DURATION_NOT_NEGATIVE(durationNotNegative()),
  VALID_UUID(validUUID()),
  VALID_JSON_SCHEMA(validJsonSchema()),
  VALID_OUTPUT_TEMPLATE_VALUE(validOutputTemplateValue()),
  EXECUTABLE_SCRIPT(executableScript()),
  ACCEPTABLE_REST_CALL_METHOD(anyOf(Const.RestCallMethod.POSSIBLE_VALUES)),
  ACCEPTABLE_AUTH_TYPE(anyOf(Const.AuthType.POSSIBLE_VALUES)),
  ACCEPTABLE_AUTH_OAUTH2_GRANT_TYPE(anyOf(Const.GrantType.POSSIBLE_VALUES)),
  ACCEPTABLE_SSL_TRUST_STORE_TYPE(anyOf(Const.SslTrustStoreType.POSSIBLE_VALUES)),
  ACCEPTABLE_MAIL_AUTH_TYPE(anyOf(Const.MailAuthType.POSSIBLE_VALUES)),
  ACCEPTABLE_SASL_MECHANISM(anyOf(Const.SaslMechanism.POSSIBLE_VALUES)),
  ACCEPTABLE_SASL_PROTOCOL(anyOf(Const.SaslProtocol.POSSIBLE_VALUES)),
  ACCEPTABLE_SORTING_DIRECTION(anyOf(Const.SortingDirection.POSSIBLE_VALUES)),
  ACCEPTABLE_STARTER_SORTING_FIELD(anyOf(Const.StarterSortingFields.POSSIBLE_VALUES)),
  ACCEPTABLE_STARTER_STATUSES(anyOf(Const.MailStarterStatus.POSSIBLE_VALUES)),
  ACCEPTABLE_WORKER_STATUSES(anyOf(Const.MailWorkerStatus.POSSIBLE_VALUES)),
  ACCEPTABLE_MAIL_PROTOCOL(anyOf(Const.MailConnectionProtocol.POSSIBLE_VALUES)),
  VALID_OFFSET_DATE_TIME(validOffsetDateTime()),
  VALID_EMAIL(validEmail()),
  VALID_CURL(validCurl()),
  NOT_NEGATIVE(notNegative()),
  MAX_100(maximum(100L)),
  ;

  private final Validation validation;

}
