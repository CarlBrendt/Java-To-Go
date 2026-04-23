package ru.mts.ip.workflow.engine.lang.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.json.JsonSerializer;
import ru.mts.ip.workflow.engine.lang.DSLCompiler;
import ru.mts.ip.workflow.engine.lang.plant.UmlPath.Traverser;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;
import ru.mts.ip.workflow.engine.service.WorkflowInstance;

@Component("plant")
@RequiredArgsConstructor
public class PlantCompiler implements DSLCompiler {

  private final JsonSerializer serializer;

  @Override
  @SneakyThrows
  public JsonNode compileWorkflowExpressionToJson(String src) {
    return compileWorkflowDefinition(src).getCompiled();
  }

  @Override
  @SneakyThrows
  public String decompileWorkflowExpression(JsonNode compiled) {
    return decompileWorkflowDefinition(new WorkflowDefinition().setCompiled(compiled));
  }

  @Override
  public WorkflowDefinition compileWorkflowDefinition(String src) {
    WorkflowExpression res = new WorkflowExpression();

    PlantCompilationContext ctx = new PlantCompilationContext(src);
    PlantInstruction inst = PlantInstructionType.createInstruction(ctx).orElseThrow();
    List<Activity> activities = new ArrayList<>();
    inst.visit(i -> activities.add(i.toActivity()));
    
    Activity entry = inst.toActivity();
    res.setStart(entry.getId());
    res.setActivities(activities);
    
    var def = ctx.getTail().toDefinition();
    def.setCompiled(serializer.toJson(res));
    return def;
  }


  @Override
  public String decompileWorkflowDefinition(WorkflowDefinition def) {
    PlantTail tail = new PlantTail(def);
    JsonNode expressionJson = def.getCompiled();
    WorkflowExpression compiled = serializer.treeToValue(expressionJson, WorkflowExpression.class);
    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(s -> s.getId(), s -> s));
    String root = compiled.getStart();
    Pouch p = new Pouch();
    p.setStates(all);
    UmlPath path = new UmlPath(root, new Traverser(compiled), new ArrayList<>());
    Start start = new Start(path);
    TextView tv = new TextView(tail);
    start.print(tv);
    return tv.toString();
  }


  @Override
  public String decompileWorkflowInstance(WorkflowInstance inst) {
    WorkflowDefinition def = inst.getDef();
    PlantTail tail = new PlantTail(def);
    JsonNode expressionJson = def.getCompiled();
    WorkflowExpression compiled = serializer.treeToValue(expressionJson, WorkflowExpression.class);
    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(s -> s.getId(), s -> s));
    String root = compiled.getStart();
    Pouch p = new Pouch();
    p.setStates(all);
    UmlPath path = new UmlPath(root, new Traverser(compiled), new ArrayList<>());
    Start start = new Start(path);
    TextView tv = new TextView(tail, inst.getHist());
    start.print(tv);
    return tv.toString();
  }

  @Override
  public WorkflowExpression compileWorkflowExpression(String src) {
    JsonNode json = compileWorkflowExpressionToJson(src);
    return serializer.treeToValue(json, WorkflowExpression.class);
  }

}
