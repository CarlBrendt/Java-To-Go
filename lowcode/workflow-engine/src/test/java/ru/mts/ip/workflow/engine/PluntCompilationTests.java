package ru.mts.ip.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.controller.dto.DtoMapperImpl;
import ru.mts.ip.workflow.engine.entity.WorkflowDefinition;
import ru.mts.ip.workflow.engine.json.JsonSerializerImpl;
import ru.mts.ip.workflow.engine.lang.plant.PlantCompiler;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.DataCondition;
import ru.mts.ip.workflow.engine.repository.WorkflowDefinitionRepository;
import ru.mts.ip.workflow.engine.testutils.JsonWorkflowDefinitionRepository;
import ru.mts.ip.workflow.engine.testutils.PlantUmlRepository;


public class PluntCompilationTests {

  
  private PlantCompiler initCompiler() {
    return new PlantCompiler(new JsonSerializerImpl(new ObjectMapper(), new DtoMapperImpl()));
  }
  
  @Test
  @SneakyThrows
  public void test0PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("0.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    assertEquals(compiled.getStart(), "0");
    assertEquals(compiled.getActivities().size(), 1);
    assertEquals(compiled.getActivities().get(0).getId(), "0");
    assertEquals(compiled.getActivities().get(0).getType(), "inject");
    assertEquals(compiled.getActivities().get(0).getOutputFilter().get("i"), "spel{100}");

    ObjectMapper om = new ObjectMapper();
    Map<String, JsonNode> toInject = compiled.getActivities().get(0).getInjectData();

    TypeReference<List<Integer>> listTypeRef = new TypeReference<>() {};
    List<Integer> var3 =
        om.treeToValue(toInject.get("var3"), om.getTypeFactory().constructType(listTypeRef));

    TypeReference<Map<String, String>> mapTypeRef = new TypeReference<>() {};
    Map<String, String> var2 =
        om.treeToValue(toInject.get("var2"), om.getTypeFactory().constructType(mapTypeRef));
    String var1 = toInject.get("var1").asText();

    assertEquals(var3, List.of(0, 1));
    assertEquals(var2, Map.of("n", "n"));
    assertEquals(var1, "var1");

  }

  @Test
  @SneakyThrows
  public void test0PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test0PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("0.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test1PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("1.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);
    
    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));

    assertEquals(compiled.getStart(), "act_0");
    assertEquals(all.size(), 6);
    assertEquals(all.get("act_0").getTransition(), "If_0");

    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_1");
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_3");

    assertEquals(all.get("act_1").getTransition(), "act_2");
    assertEquals(all.get("act_2").getTransition(), "act_4");
    assertEquals(all.get("act_3").getTransition(), "act_4");
    assertNull(all.get("act_4").getTransition());

  }

  @Test
  @SneakyThrows
  public void test1PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test1PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    
    String expected = repo.findDefinition("1.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }


  private String compress(String src) {
    return src.trim().replaceAll("\t", "").replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ",
        "");
  }

  @Test
  @SneakyThrows
  public void test2PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("2.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));

    assertEquals(compiled.getStart(), "If_0");
    assertEquals(all.size(), 3);
    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_0");
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_1");

    assertNull(all.get("act_0").getTransition());
    assertNull(all.get("act_1").getTransition());

  }

  @Test
  @SneakyThrows
  public void test2PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test2PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("2.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test3PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("3.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));

    assertEquals(compiled.getStart(), "If_0");
    assertEquals(all.size(), 6);
    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_0");
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_1");

    assertEquals(all.get("act_0").getTransition(), "If_1");
    assertEquals(all.get("act_1").getTransition(), "If_1");

    Activity ifState1 = all.get("If_1");

    List<DataCondition> conditions1 = ifState1.getDataConditions();
    assertEquals(conditions1.size(), 1);
    assertEquals(conditions1.get(0).getTransition(), "act_2");
    assertEquals(ifState1.getDefaultCondition().getTransition(), "act_3");

    assertNull(all.get("act_2").getTransition());
    assertNull(all.get("act_3").getTransition());

  }

  @Test
  @SneakyThrows
  public void test3PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test3PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("3.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test4PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("4.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(compiled.getStart(), "If_0");
    assertEquals(all.size(), 7);
    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_4");
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_1");

    assertEquals(all.get("act_4").getTransition(), "If_1");
    Activity ifState1 = all.get("If_1");
    conditions = ifState1.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_2");
    assertEquals(ifState1.getDefaultCondition().getTransition(), "act_3");

    assertEquals(all.get("act_2").getTransition(), "act_5");
    assertEquals(all.get("act_3").getTransition(), "act_5");

    assertNull(all.get("act_5").getTransition());
    assertNull(all.get("act_1").getTransition());

  }

  @Test
  @SneakyThrows
  public void test4PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test4PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("4.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test5PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("5.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(compiled.getStart(), "If_0");
    assertEquals(all.size(), 2);
    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertNull(conditions.get(0).getTransition());
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_0");
    assertEquals(all.get("act_0").getTransition(), "If_0");

  }

  @Test
  @SneakyThrows
  public void test5PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test5PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("5.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test6PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("6.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(all.size(), 5);

    assertEquals(compiled.getStart(), "act_0");
    assertEquals(all.get("act_0").getTransition(), "act_3");
    assertEquals(all.get("act_3").getTransition(), "If_0");

    Activity ifState = all.get("If_0");
    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "act_2");
    assertEquals(ifState.getDefaultCondition().getTransition(), "act_1");

    assertEquals(all.get("act_1").getTransition(), "act_3");
    assertNull(all.get("act_2").getTransition());

  }

  @Test
  public void test6PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test6PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("6.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  public void test7PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg = wdr.findFirstByNameOrderByVersionDesc("testLoopWorkflow").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("7.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  public void test8PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("testEventWorkflow").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("8.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  public void test9PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("testRestCallWorkflow").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("9.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test10PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("10.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(all.size(), 4);

    assertEquals(compiled.getStart(), "if_0");
    Activity ifState = all.get("if_0");
    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 2);
    assertEquals(conditions.get(0).getTransition(), "drink_bear");
    assertEquals(conditions.get(1).getTransition(), "drink_vine");
    assertEquals(ifState.getDefaultCondition().getTransition(), "go_home");

    assertNull(all.get("drink_bear").getTransition());
    assertNull(all.get("drink_vine").getTransition());
    assertNull(all.get("go_home").getTransition());

  }

  @Test
  @SneakyThrows
  public void test11PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("11.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(7, all.size());
    assertEquals(compiled.getStart(), "call_task_service");

    Activity ifState = all.get("if_0");
    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "go_home1");
    assertEquals(ifState.getDefaultCondition().getTransition(), "approval_task");

    ifState = all.get("if_1");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "execute");
    assertEquals(ifState.getDefaultCondition().getTransition(), "go_home");

    assertEquals(all.get("execute").getTransition(), "call_task_service");
    assertEquals(all.get("go_home").getTransition(), "call_task_service");
    assertNull(all.get("go_home1").getTransition());

  }

  @Test
  @SneakyThrows
  public void test12PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("12.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(3, all.size());

    assertEquals(compiled.getStart(), "if_0");
    Activity ifState = all.get("if_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertNull(conditions.get(0).getTransition());
    assertEquals(ifState.getDefaultCondition().getTransition(), "if_1");

    ifState = all.get("if_1");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "call_task_service");
    assertEquals("if_0", ifState.getDefaultCondition().getTransition());

  }

  @Test
  public void test12PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test12PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("12.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }


  @Test
  @SneakyThrows
  public void test13PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("13.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);
    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(all.size(), 9);

    assertEquals(compiled.getStart(), "e1");
    Activity ifState = all.get("if_1");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e7");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e9");

    ifState = all.get("if_1");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e7");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e9");

    assertEquals(all.get("e1").getTransition(), "e2");
    assertEquals(all.get("e2").getTransition(), "if_1");
    assertEquals(all.get("e7").getTransition(), "e8");
    assertEquals(all.get("e9").getTransition(), "e10");
    assertEquals(all.get("e8").getTransition(), "e15");
    assertEquals(all.get("e10").getTransition(), "e15");
    assertEquals(all.get("e15").getTransition(), "e16");
    assertNull(all.get("e16").getTransition());
  }

  @Test
  public void test13PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test13PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("13.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test14PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("14.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(18, all.size());

    assertEquals(compiled.getStart(), "e1");
    assertEquals(all.get("e1").getTransition(), "e2");
    assertEquals(all.get("e2").getTransition(), "e3");
    assertEquals(all.get("e3").getTransition(), "e4");
    assertEquals(all.get("e4").getTransition(), "if_0");

    Activity ifState = all.get("if_0");
    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e15");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e5");

    assertEquals(all.get("e5").getTransition(), "e6");
    assertEquals(all.get("e6").getTransition(), "if_1");

    ifState = all.get("if_1");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e7");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e9");
    assertEquals(all.get("e7").getTransition(), "e8");
    assertEquals(all.get("e9").getTransition(), "e10");

    assertEquals(all.get("e10").getTransition(), "e11");
    assertEquals(all.get("e12").getTransition(), "e13");
    assertEquals(all.get("e13").getTransition(), "e14");
    assertEquals(all.get("e14").getTransition(), "e3");
    assertEquals(all.get("e15").getTransition(), "e16");
    assertNull(all.get("e16").getTransition());
  }


  @Test
  @SneakyThrows
  public void test14PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test14PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("14.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

  @Test
  @SneakyThrows
  public void test15PlantCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("15.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));
    assertEquals(18, all.size());

    assertEquals(compiled.getStart(), "e1");
    assertEquals(all.get("e1").getTransition(), "e2");
    assertEquals(all.get("e2").getTransition(), "e3");
    assertEquals(all.get("e3").getTransition(), "if_0");

    Activity ifState = all.get("if_0");
    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e4");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e7");
    assertEquals(all.get("e4").getTransition(), "if_1");

    ifState = all.get("if_1");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e5");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e6");

    assertEquals(all.get("e5").getTransition(), "e8");
    assertEquals(all.get("e6").getTransition(), "e8");
    assertEquals(all.get("e7").getTransition(), "e8");

    assertEquals(all.get("e8").getTransition(), "if_2");
    ifState = all.get("if_2");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals("e13", conditions.get(0).getTransition());
    assertEquals(ifState.getDefaultCondition().getTransition(), "if_3");

    ifState = all.get("if_3");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e9");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e11");
    assertEquals(all.get("e11").getTransition(), "e12");

    assertEquals(all.get("e9").getTransition(), "if_4");

    ifState = all.get("if_4");
    conditions = ifState.getDataConditions();
    assertEquals(conditions.size(), 1);
    assertEquals(conditions.get(0).getTransition(), "e3");
    assertEquals(ifState.getDefaultCondition().getTransition(), "e10");


    assertEquals(all.get("e12").getTransition(), "e3");
    assertEquals(all.get("e10").getTransition(), "e9");
    assertNull(all.get("e13").getTransition());
  }

  @Test
  @SneakyThrows
  public void test15PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg =
        wdr.findFirstByNameOrderByVersionDesc("test15PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("15.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }
  
  
  @Test
  @SneakyThrows
  public void test16PluntCompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    String src = repo.findDefinition("16.plunt").orElseThrow();
    WorkflowExpression compiled = compiler.compileWorkflowExpression(src);

    Map<String, Activity> all =
        compiled.getActivities().stream().collect(Collectors.toMap(v -> v.getId(), v -> v));

    assertEquals("act_0", compiled.getStart());
    assertEquals(5, all.size());
    assertEquals("If_0", all.get("act_0").getTransition());

    Activity ifState = all.get("If_0");

    List<DataCondition> conditions = ifState.getDataConditions();
    assertEquals(1, conditions.size());
    assertEquals("act_1", conditions.get(0).getTransition());
    assertEquals("act_4", ifState.getDefaultCondition().getTransition());

    assertEquals("If_0", all.get("act_0").getTransition());
    assertEquals("act_2", all.get("act_1").getTransition());
    assertEquals("act_4", all.get("act_2").getTransition());
    assertNull(all.get("act_4").getTransition());
    
  }
  
  @Test
  @SneakyThrows
  public void test16PluntDecompile() {
    PlantCompiler compiler = initCompiler();
    PlantUmlRepository repo = new PlantUmlRepository();
    WorkflowDefinitionRepository wdr = new JsonWorkflowDefinitionRepository();
    WorkflowDefinition gg = wdr.findFirstByNameOrderByVersionDesc("test16PluntDecompile").orElseThrow();
    String decompiled = compiler.decompileWorkflowExpression(gg.getCompiled());
    String expected = repo.findDefinition("16.plunt").orElseThrow();
    assertEquals(compress(expected), compress(decompiled));
  }

}
