package ru.mts.ip.workflow.engine.lang.plant;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.exception.DuplicateIdException;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.WorkflowCall;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PlantActivity implements PlantInstruction {

  private final Activity impl;
  private PlantInstruction next;

  @Data
  static class Details {
    private String type;
    private WorkflowCall workflowCall;
    private Map<String, JsonNode> injectData;
    private Map<String, String> outputFilter;
    private String timerDuration;

    Details(Activity state) {
      type = state.getType();
      workflowCall = state.getWorkflowCall();
      timerDuration = state.getTimerDuration();
      if (state.getOutputFilter() != null) {
        outputFilter = new HashMap<>(state.getOutputFilter());
      }
      if (state.getInjectData() != null) {
        injectData = new HashMap<>(state.getInjectData());
      }
    }
  }

  public PlantActivity(PlantCompilationContext ctx) {
    PlantLine initLine = ctx.eatRequired(this::isStart, ":action;");
    String lem = initLine.getLem();
    JsonNode comment = initLine.getComment();
    if (comment != null) {
      impl = PlantUtils.readInstruction(comment, Activity.class);
      String description = lem.substring(1, lem.length() - 1);
      impl.setDescription(description);
      try {
        impl.setId(ctx.nextId(getClass().getSimpleName(), initLine.getId()));
      }catch (DuplicateIdException ex){
        throw new PlantUmlSyntaxError(ex.getMessage());
      }
    } else {
      throw new PlantUmlSyntaxError("Activity.comment is required");
    }
  }

  PlantActivity(UmlPath path) {
    this.impl = path.asState();
  }

  private boolean isStart(PlantLine line) {
    return canParse(line.getLem());
  }

  public static boolean canParse(String lem) {
    return lem.startsWith(":") && lem.endsWith(";");
  }

  @Override
  @SneakyThrows
  public void print(TextView view) {
    String description = impl.getDescription();
    PlantLine pl = new PlantLine(getId(), new Details(impl), ":%s;".formatted(description), true);
    view.lemWithDescription(pl);
  }

  @Override
  public Activity toActivity() {
    return impl;
  }

  @Override
  public void visit(Consumer<PlantInstruction> consumer) {
    consumer.accept(this);
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  @Override
  public PlantInstruction getNext() {
    return next;
  }

  @Override
  public void setNext(PlantInstruction next) {

    if (this.next instanceof Repeat) {
      if (next == null) {
        return;
      }
      next.setNext(this.next);
    }
    this.next = next;
    String nextId = this.next == null ? null : this.next.getId();
    impl.setTransition(nextId);
  }

  @Override
  public String toString() {
    return "Activity[%s]".formatted(impl != null ? impl.getId() : null);
  }

}
