package ru.mts.workflowmail.share.validation.schema;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.exception.ErrorMessageArgs;
import ru.mts.workflowmail.service.Const;
import ru.mts.workflowmail.share.validation.Errors2;

import java.util.List;

@Data
@Accessors(chain = true)
public class ConstraintViolation {
  private Errors2 error;
  private String path;
  private JsonNode rejectedValue;
  private ErrorMessageArgs messageArgs;
  private List<ErrorDescription> completeErrors;
  private ConstraintViolation cause;

  public boolean isError() {
    return error != null || (completeErrors != null && !completeErrors.isEmpty());
  }

  public boolean containsCriticalErrors() {
    if(error != null) {
      return Const.ErrorLevel.CRITICAL.equals(error.getLevel());
    } else {
      return completeErrors != null && !completeErrors.stream()
          .filter(ed -> Const.ErrorLevel.CRITICAL.equals(ed.getLevel()))
          .toList()
          .isEmpty();
    }
  }
}
