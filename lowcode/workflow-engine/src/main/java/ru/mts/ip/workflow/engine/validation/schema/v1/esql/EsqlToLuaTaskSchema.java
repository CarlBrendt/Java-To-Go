package ru.mts.ip.workflow.engine.validation.schema.v1.esql;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.controller.dto.EsqlCompilationWorkflowDefinitionSchema;
import ru.mts.ip.workflow.engine.validation.Constraint;
import ru.mts.ip.workflow.engine.validation.Context;
import ru.mts.ip.workflow.engine.validation.schema.ArraySchema;
import ru.mts.ip.workflow.engine.validation.schema.ConstraintViolation;
import ru.mts.ip.workflow.engine.validation.schema.ObjectSchema;
import ru.mts.ip.workflow.engine.validation.schema.StringSchema;

public class EsqlToLuaTaskSchema extends ObjectSchema {
  
  public final static String WORKFLOW_DEFINITION = "workflowDefinition";
  public final static String PARENT_ACTIVITY_ID = "parentActivityId";
  public final static String ESQL_SOURCES = "esqlSources";
  public final static String ESQL_SOURCES_NAME = "name";
  public final static String ESQL_SOURCES_CONTENT = "content";
  
  public EsqlToLuaTaskSchema(List<Constraint> constraints) {
    super(constraints);
    putField(WORKFLOW_DEFINITION, new EsqlCompilationWorkflowDefinitionSchema());
    putField(PARENT_ACTIVITY_ID, new StringSchema());
    putField(ESQL_SOURCES, new ArraySchema(new ObjectSchema(Map.of(
        ESQL_SOURCES_NAME, new StringSchema(Constraint.NOT_BLANK), 
        ESQL_SOURCES_CONTENT, new StringSchema(Constraint.FILLED, Constraint.NOT_BLANK) 
      )), Constraint.FILLED, Constraint.NOT_EMPTY_ARRAY));
  }

  public EsqlToLuaTaskSchema(Constraint ...constraints) {
    this(List.of(constraints));
  }
  
  @Override
  public List<ConstraintViolation> getViolations() {
    return super.getViolations();
  }

}
