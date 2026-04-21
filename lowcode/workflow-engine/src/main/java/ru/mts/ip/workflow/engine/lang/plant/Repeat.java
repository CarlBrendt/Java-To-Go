package ru.mts.ip.workflow.engine.lang.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

public class Repeat implements PlantInstruction {

  private final static String START = "repeat";
  private final static String STOP = "repeat while (true)";
  private List<PlantInstruction> arguments = new ArrayList<>();
  private Activity impl;

  public Repeat(PlantCompilationContext ctx) {
    ctx.eatRequired(l -> l.getLem().startsWith(START), START);
    Optional<PlantInstruction> optArg = Optional.empty();
    while ((optArg = ctx.tryReadArgument(l -> !l.getLem().startsWith(STOP))).isPresent()) {
      optArg.map(arguments::add);
    }

    ctx.eatRequired(l -> l.getLem().startsWith(STOP), STOP);
    impl = arguments.stream().findFirst().orElseThrow().toActivity();
    arguments.forEach(pi -> pi.setParent(this));
    PlantUtils.linkArgs(arguments);
  }

  @Override
  public boolean contains(String id) {
    return arguments.stream().map(a -> a.getId()).toList().contains(id);
  }



  @Override
  public String getBreak() {
    if (!arguments.isEmpty()) {
      return arguments.stream().skip(arguments.size() - 1).findFirst().map(i -> i.getBreak())
          .orElse(null);
    } else {
      return null;
    }
  }

  public Repeat(UmlPath path) {
    impl = path.asState();
    for (UmlPath p : path.detailForRepeat().stream().findFirst().orElseThrow()) {
      if (p.getId() != null) {
        if (p.isActivity()) {
          arguments.add(new PlantActivity(p));
        } else if (p.isIf()) {
          arguments.add(new If(p, p.isParentOfRepeat()));
        }
      }
    }
  }


  @Override
  public void visit(Consumer<PlantInstruction> consumer) {
    arguments.forEach(pi -> pi.visit(consumer));
  }

  @Override
  public void print(TextView view) {
    view.line(START);
    arguments.forEach(i -> i.print(view.tab()));
    view.line(STOP);
  }

  @Override
  public Activity toActivity() {
    return impl;
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  @Override
  public void setNext(PlantInstruction next) {
    arguments.stream().filter(a -> a instanceof If).map(If.class::cast)
        .forEach(i -> i.setBreakExit(next));
  }

}
