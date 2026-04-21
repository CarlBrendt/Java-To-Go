package ru.mts.ip.workflow.engine.lang.plant;

import static ru.mts.ip.workflow.engine.Const.Plant.DOC_END;
import static ru.mts.ip.workflow.engine.Const.Plant.DOC_START;
import static ru.mts.ip.workflow.engine.Const.Plant.START;
import static ru.mts.ip.workflow.engine.Const.Plant.STOP;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

public class Start implements PlantInstruction {

  private List<PlantInstruction> arguments = new ArrayList<>();
  private Activity impl;

  public Start(PlantCompilationContext ctx) {
    ctx.eatRequired(l -> l.getLem().startsWith(DOC_START), DOC_START);
    ctx.eatRequired(l -> l.getLem().startsWith(START), START);
    Optional<PlantInstruction> optArg = Optional.empty();
    while ((optArg = ctx.tryReadArgument(l -> !STOP.equals(l.getLem()))).isPresent()) {
      optArg.map(arguments::add);
    }
    ctx.eatRequired(l -> l.getLem().startsWith(STOP), STOP);
    ctx.eatRequired(l -> l.getLem().startsWith(DOC_END), DOC_END);
    impl = arguments.stream().findFirst().orElseThrow().toActivity();
    PlantUtils.linkArgs(arguments);
  }

  @Override
  public void visit(Consumer<PlantInstruction> consumer) {
    arguments.forEach(pi -> pi.visit(consumer));
  }

  public Start(UmlPath root) {
    impl = root.asState();
    for (UmlPath p : root.asPlain()) {
      if (p != null && p.getId() != null) {
        arguments.add(PlantInstructionType.createInstruction(p).orElseThrow());
      }
    }
  }

  public static boolean canParse(String lem) {
    return DOC_START.equals(lem);
  }

  @Override
  public void print(TextView painter) {
    painter.line(DOC_START);
    painter.line(START);
    arguments.forEach(i -> i.print(painter.tab()));
    painter.line(STOP);
    painter.line(DOC_END);
  }

  @Override
  public Activity toActivity() {
    return impl;
  }

  @Override
  public String getId() {
    return impl.getId();
  }

}
