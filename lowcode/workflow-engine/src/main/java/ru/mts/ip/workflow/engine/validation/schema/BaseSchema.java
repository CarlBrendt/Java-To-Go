package ru.mts.ip.workflow.engine.validation.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.Const.Errors2;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.ValidateDecision;

public class BaseSchema {
  
  protected List<ConstraintViolation> violations = new ArrayList<>();
  protected final List<Constraint> constraints = new ArrayList<>();
  
  @Getter
  private String path;
  
  public BaseSchema(List<Constraint> constraints) {
    this.constraints.addAll(constraints);
  }

  public BaseSchema() {
    this(List.of());
  }
  public BaseSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }
  
  protected void addConstraint(Constraint constraint) {
    constraints.add(constraint);
  }
  
  public BaseSchema copy() {
    return new BaseSchema(constraints);
  }
  
  protected Map<String, BaseSchema> getNested(){
    return Map.of();
  }

  protected void enrichContext(Context ctx, JsonNode value){
  
  }

  protected List<Constraint> copyConstraints(){
    return new ArrayList<>(constraints);
  }

  public void initPath(String val) {
    path = val == null || val.isBlank() ? "$" : val;
    getNested().forEach((k,v) -> v.initPath("%s.%s".formatted(path, k)));
  }
  
  public boolean containErrors() {
    return !violations.stream().filter(ConstraintViolation::containCrilicals).toList().isEmpty();
  }
  
  public void validate(Context ctx, JsonNode json) {
    enrichContext(ctx, json);
    ValidateDecision des = new ValidateDecision();
    for(Constraint c : constraints) {
      des = c.getValidation().test(ctx, json);
      Errors2 error = des.getError();
      List<ErrorDescription> completeErrors = des.getErrorDescriptions();
      
      if(error != null) {
        violations.add(new ConstraintViolation().setError(des.getError()).setPath(path).setRejectedValue(json).setMessageArgs(des.getArgs()));
        break;
      } else if (completeErrors != null && !completeErrors.isEmpty()) {
        completeErrors.stream().forEach(ed -> ed.setRootFieldPath(path));
        violations.add(new ConstraintViolation().setCompleteErrors(completeErrors));
        if(!completeErrors.stream().filter(err -> Const.ErrorLevel.CRITICAL.equals(err.getLevel())).toList().isEmpty()) {
          break;
        }
      }
    }
  }
  
  public List<ConstraintViolation> getViolations(){
    List<ConstraintViolation> res = new ArrayList<>(violations);
    getNested().values().forEach(f -> res.addAll(f.getViolations()));
    return res;
  }
  
}
