package ru.mts.ip.workflow.engine.validation.schema;

import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.exception.ErrorMessageArgs;

@Data
@Accessors(chain = true)
public class ConstraintViolation {
  private Errors2 error;
  private String path;
  private String activityId;
  private JsonNode rejectedValue;
  private ErrorMessageArgs messageArgs;
  private List<ErrorDescription> completeErrors;
  
  public boolean containCrilicals() {
    if(error != null) {
      return Const.ErrorLevel.CRITICAL.equals(error.getLevel());
    } else {
      return completeErrors == null ? false : !completeErrors.stream().filter(ed -> Const.ErrorLevel.CRITICAL.equals(ed.getLevel())).toList().isEmpty();
    }
  }
}
