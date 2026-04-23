package ru.mts.ip.workflow.engine.testutils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import org.springframework.test.web.servlet.ResultActions;
import lombok.experimental.UtilityClass;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorContext;
import ru.mts.ip.workflow.engine.exception.ErrorLocation;

@UtilityClass
public class ErrorsFormatHelper {

  public void checkRootErrorDescriptionFormat(ResultActions ra, String enpointPath, Errors2 error) throws Exception {
    ra.andExpect(jsonPath("$").isMap());
    ra.andExpect(jsonPath("timestamp").exists());
    ra.andExpect(jsonPath("timestamp").isString());
    ra.andExpect(jsonPath("timestamp").isNotEmpty());
    ra.andExpect(jsonPath("path").exists());
    ra.andExpect(jsonPath("path").isString());
    ra.andExpect(jsonPath("path").isNotEmpty());
    ra.andExpect(jsonPath("path").value(enpointPath));
    ra.andExpect(jsonPath("errorCode").exists());
    ra.andExpect(jsonPath("errorCode").isString());
    ra.andExpect(jsonPath("errorCode").isNotEmpty());
    ra.andExpect(jsonPath("errorCode").value(error.getCode()));
    ra.andExpect(jsonPath("errorMessage").exists());
    ra.andExpect(jsonPath("errorMessage").isString());
    ra.andExpect(jsonPath("errorMessage").isNotEmpty());
    ra.andExpect(jsonPath("solvingAdviceMessage").exists());
    ra.andExpect(jsonPath("solvingAdviceMessage").isString());
    ra.andExpect(jsonPath("solvingAdviceMessage").isNotEmpty());
    ra.andExpect(jsonPath("solvingAdviceUrl").exists());
    ra.andExpect(jsonPath("solvingAdviceUrl").isString());
    ra.andExpect(jsonPath("solvingAdviceUrl").isNotEmpty());
  }

  public void checkValidationErrorDescriptionFormat(String errorDetailsPath, ResultActions ra, Errors2 error) throws Exception {
    checkValidationErrorDescriptionFormat(errorDetailsPath, ra, error, null, null);
  }

  public void checkValidationErrorDescriptionFormat(String errorDetailsPath, ResultActions ra, Errors2 error, ErrorLocation errLocation) throws Exception {
    checkValidationErrorDescriptionFormat(errorDetailsPath, ra, error, errLocation, null);
  }

  public void checkValidationErrorDescriptionFormat(String errorDetailsPath, ResultActions ra, Errors2 error, ErrorContext errContext) throws Exception {
    checkValidationErrorDescriptionFormat(errorDetailsPath, ra, error, null, errContext);
  }

  public void checkValidationErrorDescriptionFormat(String errorDetailsPath, ResultActions ra, Errors2 error
      , ErrorLocation errLocation, ErrorContext errContext) throws Exception {
    
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "code")).exists());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "code")).isNotEmpty());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "code")).isString());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "code")).value(error.getCode()));
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "message")).exists());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "message")).isNotEmpty());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "message")).isString());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceMessage")).exists());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceMessage")).isNotEmpty());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceMessage")).isString());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceUrl")).exists());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceUrl")).isNotEmpty());
    ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "solvingAdviceUrl")).isString());
    
    if(errLocation != null) {
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location")).isMap());
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location")).isNotEmpty());
      String fieldPath = errLocation.getFieldPath();
      if(fieldPath != null) {
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location.fieldPath")).isString());
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location.fieldPath")).isNotEmpty());
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location.fieldPath")).value(fieldPath));
      } else {
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location.fieldPath")).doesNotExist());
      }
    } else {
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "location")).doesNotExist());
    }
    
    if(errContext != null) {
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context")).isMap());
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context")).isNotEmpty());
      var rejectedValue = errContext.getRejectedValue();
      if(rejectedValue != null) {
//        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue")).isString());
//        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue")).isNotEmpty());
//        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue")).value(rejectedValue));
//        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue"), isA(JsonNode.class)));
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue"), equalTo(rejectedValue)));
      } else {
        ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context.rejectedValue")).doesNotExist());
      }
    } else {
      ra.andExpect(jsonPath("%s.%s".formatted(errorDetailsPath, "context")).doesNotExist());
    }
    
  }
  
//  "code": "LC-WE-3",
//  "message": "Поле не может быть пустым",
//  "solvingAdviceMessage": "Заполните поле корректным значением",
//  "solvingAdviceUrl": "http://wiki-error?errorCode=LC-WE-3",
//  "location": {
//      "fieldPath": "activities"
//  },
//  "context": {
//      "rejectedValue": "null"
//  }
  
  
}
