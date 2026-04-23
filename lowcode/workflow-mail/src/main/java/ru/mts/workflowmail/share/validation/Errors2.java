package ru.mts.workflowmail.share.validation;

import lombok.Getter;
import ru.mts.workflowmail.service.Const;

import java.util.stream.Stream;

@Getter
public enum Errors2 {

  UNEXPECTED("{unexpected}", 777),
  INVALID_BODY_JSON("{invalid.body}", 1),
  FIELD_WRONG_TYPE("{field.wrong.type}", 2),
  FIELD_NULL("{field.null}", 3),
  FIELD_EMPTY("{field.empty}", 4),
  FIELD_BLANK("{field.blank}", 5),
  FIELD_NOT_FILED("{field.not.filed}", 6),
  FIELD_WRONG_VALUE("{field.wrong.value}", 7),
  FILED_NOTHING("{field.nothing}", 8),
  UNKNOWN_FIELD(Const.ErrorLevel.WARNING, "{unused.field}", 9),
  NOT_UNIQUE_ACTIVITY_ID("{not.unique.activity.id}", 10),
  SCRIPT_IS_NOT_EXECUTABLE("{script.is.not.executable}", 11),
  TRANSITION_IS_NOT_FOUND("{transition.is.not.found}", 12),
  DURATION_INVALID_FORMET("{duration.invalid.format}", 13),
  DURATION_NEGATIVE("{duration.negative}", 14),
  INVALID_UUID("{invalid.uuid}", 15),

  INVALI_SCRIPT_PLACEHOLDER("{invalid.script.placeholder}", 18),
  EMPTY_SCRIPT("{empty.script}", 19),

  NEGATIVE_NOT_ALLOWED("{negative.not.allowed}", 20),
  NUMBER_MORE_THAN_ALLOWED("{number.more.than.allowed}", 21),

  INVALID_OFFSET_DATE_TIME("{invalid.offset.date.time}", 25),

  SPEL_VARIABLE_IS_NOT_FOUND("{spel.variable.is.not.found}", 101),
  JSON_PATH_NOT_FOUND("{json.path.not.found}", 102),
  SCRIPT_EXECUTION_ERROR("{script.execution.error}", 103),
  UNKNOWN_SCRIPT_SYNTAX("{unknown.script.syntax}", 104),

  INVALID_HOST_PORT("{invalid.hostport}", 7000),
  MAIL_STARTER_IS_NOT_FOUND_BY_ID("{mail.starter.is.not.found.by.id}", 7404),
  MAIL_STARTER_ALREADY_EXISTS("{mail.starter.already.exists}", 7409),
  MAIL_WORKER_IS_NOT_FOUND_BY_ID("{mail.worker.is.not.found.by.id}", 7405),
  INVALID_JSON_SCHEMA("{invalid.json.schema}", 7406),
  MAIL_STARTER_INCOMPATIBLE_WITH_WORKFLOW("{mail.starter.incompatible.with.workflow}", 7407),
  INVALID_OUTPUT_TEMPLATE("{invalid.output.template}", 7408),
  INVALID_EMAIL("{invalid.email}", 7409);

  Errors2(String level, String errorAlias, int number) {
    this.level = level;
    code = "LC-%04d".formatted(number);
    errorMessageAlias = errorAlias;
    solvingMessageAlias = "{%s.solving.advice}".formatted(errorAlias.substring(1, errorAlias.length() - 1));
  }

  Errors2(String errorAlias, int number) {
    this(Const.ErrorLevel.CRITICAL, errorAlias, number);
  }

  public boolean isCritical() {
    return Const.ErrorLevel.CRITICAL.equals(level);
  }

  public final String code;
  public final String level;
  public final String errorMessageAlias;
  public final String solvingMessageAlias;

  public static Errors2 findByMessageAlias(String alias) {
    return  Stream.of(Errors2.values()).filter(e -> e.errorMessageAlias.equals(alias)).findFirst().orElse(UNEXPECTED);
  }

  public static Errors2 findByCode(String code) {
    return Stream.of(Errors2.values()).filter(e -> e.code.equals(code)).findFirst().orElse(UNEXPECTED);
  }

}
