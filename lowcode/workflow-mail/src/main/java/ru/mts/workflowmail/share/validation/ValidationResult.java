package ru.mts.workflowmail.share.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mts.workflowmail.exception.ErrorDescription;
import ru.mts.workflowmail.service.Const;

import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ValidationResult {

  private final JsonNode json;
  private final List<ErrorDescription> errors;

  public List<ErrorDescription> getWarningErrors(){
    return errors == null ? List.of() : errors.stream().filter(err -> Const.ErrorLevel.WARNING.equals(err.getLevel())).toList();
  }

  public List<ErrorDescription> getErrors(){
    return errors == null ? List.of() : errors;
  }

  public List<ErrorDescription> getCriticalErrors(){
    return errors == null ? List.of() : errors.stream().filter(err -> Const.ErrorLevel.CRITICAL.equals(err.getLevel())).toList();
  }

  public boolean containCriticalErrors() {
    return !getCriticalErrors().isEmpty();
  }

  public boolean containErrors() {
    return errors != null && !errors.isEmpty();
  }

  public Optional<JsonNode> getValidatedJson() {
    return Optional.ofNullable(json);
  }

  public <T> Optional<T> readValue(ObjectMapper om, Class<T> clazz){
    if(json != null) {
      try {
        return Optional.of(om.treeToValue(json, clazz));
      } catch (JsonProcessingException ex) {
        log.error("Wrong json parsing ", ex);
      }
    }
    return Optional.empty();
  }

}
