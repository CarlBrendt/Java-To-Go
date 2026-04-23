package ru.mts.ip.workflow.engine.lang.plant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.DataCondition;

public class If implements PlantInstruction {

  private static final Pattern IF_PATTERN = Pattern.compile("^if *\\((?<p>.*)\\) *then| *\\((?<fd>.*)\\)");
  private static final Pattern ELSEIF_PATTERN = Pattern.compile("^elseif *\\((?<p>.*)\\) *then| *\\((?<fd>.*)\\)");
  private static final Pattern ELSE_PATTERN = Pattern.compile("^else| *\\((?<fd>.*)\\)");

  private static final String ELSE = "else";
  private static final String IF = "if";
  private static final String ELSEIF = "elseif";
  private static final String ENDIF = "endif";

  private Activity impl;
  private PlantInstruction next;
  private List<Flow> flows = new ArrayList<>();

  @Override
  public String getBreak() {
    if (isParentOfRepeat()) {
      List<PlantInstruction> last = flows.get(0).getSuccessInstructions();
      if (!last.isEmpty()) {
        return last.get(0).getId();
      }
    }
    return null;
  }

  If(UmlPath path) {
    this(path, false);
  }

  If(UmlPath path, boolean repeat) {
    impl = path.asState();

    List<DataCondition> conditions = new ArrayList<>(impl.getDataConditions());

    DataCondition defaultCondition = impl.getDefaultCondition();
    conditions.add(
        new DataCondition().setSuccessFlowDescription(defaultCondition.getSuccessFlowDescription())
            .setConditionDescription(defaultCondition.getConditionDescription())
            .setTransition(defaultCondition.getTransition()));

    for (int i = 0; i < conditions.size(); i++) {
      DataCondition dc = conditions.get(i);
      Flow flow = new Flow();
      flow.setBreakFlow(i == 0 && repeat);
      List<PlantInstruction> successInstrunctions = new ArrayList<>();
      flow.setSuccessFlowDescription(dc.getSuccessFlowDescription());
      flow.setPredicate(dc.getCondition()).setPredicateDescription(dc.getConditionDescription());
      flow.setId(path.nextId(Flow.class.getSimpleName(), dc.getId()));
      flow.setPredicate(dc.getCondition());
      String trunsition = dc.getTransition();


      flows.add(flow);

      if (repeat && i == 0) {
        continue;
      }

      if (trunsition != null) {
        for (UmlPath p : path.findDetailed(trunsition)) {
          if (p != null && p.getId() != null) {
            successInstrunctions.add(PlantInstructionType.createInstruction(p).orElseThrow());
          }
        }
        flow.setSuccessInstructions(successInstrunctions);
      }
    }

  }

  private PlantCompilationContext ctx;

  public If(PlantCompilationContext ctx) {
    this.ctx = ctx;
    ctx.currentRequired(this::isIf, "if (predicate) then [(success flow)]");
    Flow headFlow = eatFlow(this::isIf).orElseThrow();
    flows.add(headFlow);
    Optional<Flow> optFlow = Optional.empty();
    while ((optFlow = eatFlow(l -> isElseIf(l) || isElse(l))).isPresent()) {
      optFlow.map(flows::add);
    }
    impl = compileState();
    impl.setId(headFlow.getId());
    ctx.eatRequired(this::isEndIf, ENDIF);
  }

  private Optional<Flow> eatFlow(Predicate<PlantLine> filter) {
    return ctx.eatLine(filter).flatMap(line -> {
      Flow res = null;
      if (isIf(line)) {
        res = parseFlow(IF_PATTERN, line);
      } else if (isElseIf(line)) {
        res = parseFlow(ELSEIF_PATTERN, line);
      } else if (isElse(line)) {
        res = parseElse(line);
      }
      if (res != null) {
        List<PlantInstruction> flowInstructions = readFlowInstructions();
        res.setSuccessInstructions(flowInstructions);
        Boolean currentIsBreak =
            ctx.current().map(g -> g.getLem().startsWith("break;")).orElse(false);
        res.setId(ctx.nextId(Flow.class.getSimpleName(), line.getId()));
        if (currentIsBreak) {
          if (!flowInstructions.isEmpty()) {
            throw new PlantUmlSyntaxError("break; with instructions id not allowed");
          }
          ctx.eat();
        }
      }
      return Optional.ofNullable(res);
    });
  }


  private List<PlantInstruction> readFlowInstructions() {
    List<PlantInstruction> res = new ArrayList<>();
    Optional<PlantInstruction> optArg = Optional.empty();
    while ((optArg = ctx.tryReadArgument(l -> !isPartOfInstruction(l))).isPresent()) {
      optArg.map(res::add);
    }
    PlantUtils.linkArgs(res);
    return res;
  }


  void setBreakExit(PlantInstruction next) {
    nextConsumers.forEach(c -> {
      c.accept(next == null ? null : next.getId());
    });
  }


  @Override
  public void setNext(PlantInstruction next) {
    if (next == null) {
      return;
    }
    this.next = next;
    flows.forEach(f -> {
      List<PlantInstruction> success = f.successInstructions;
      if (!success.isEmpty()) {
        PlantInstruction last = success.get(success.size() - 1);
        PlantInstruction existsNext = last.getNext();
        if (existsNext instanceof Repeat) {
          next.setNext(existsNext);
          last.setNext(next);
        } else if (existsNext == null) {
          last.setNext(next);
        }
      }
    });
    nextConsumers.forEach(c -> {
      c.accept(next == null ? null : next.getId());
    });
  }

  @Override
  public void setParent(PlantInstruction parent) {
    if (parent instanceof Repeat) {
      flows.stream().skip(flows.size() - 1).findFirst().ifPresent(f -> {
        List<PlantInstruction> si = f.getSuccessInstructions();
        si.stream().skip(si.size() - 1).findFirst().ifPresent(last -> {
          last.setNext(parent);
        });
      });
    }
  }

  private boolean isParentOfRepeat() {
    return flows.stream().map(f -> f.isBreakFlow()).reduce((a, b) -> a || b).orElse(false);
  }

  private Flow parseElse(PlantLine pl) {
    Matcher m = ELSE_PATTERN.matcher(pl.getLem());
    String predicateText = null;
    String flowDescription = null;
    if (m.find() && m.find()) {
      flowDescription = m.group("fd");
    }
    return new Flow().setPredicate(predicateText).setSuccessFlowDescription(flowDescription);
  }

  private Flow parseFlow(Pattern pattern, PlantLine pl) {
    Matcher m = pattern.matcher(pl.getLem());
    String predicateDescription = null;
    String flowDescription = null;
    if (m.find()) {
      predicateDescription = m.group("p");
    }
    if (m.find()) {
      flowDescription = m.group("fd");
    }
    return new Flow().setSuccessFlowDescription(flowDescription)
        .setPredicateDescription(predicateDescription)
        .setPredicate(parseDataCondition(pl.getComment()).getCondition());

  }

  private DataCondition parseDataCondition(JsonNode comment) {
    return PlantUtils.readInstruction(comment, DataCondition.class);
  }


  private boolean isEndIf(PlantLine line) {
    return line.getLem().startsWith(ENDIF);
  }

  private boolean isIf(PlantLine line) {
    return line.getLem().startsWith(IF);
  }

  private boolean isElseIf(PlantLine line) {
    return line.getLem().startsWith(ELSEIF);
  }

  private boolean isElse(PlantLine line) {
    return line.getLem().startsWith(ELSE);
  }

  public static boolean canParse(String line) {
    return line.startsWith(IF);
  }

  boolean isPartOfInstruction(PlantLine lem) {
    return isElse(lem) || isElseIf(lem) || isEndIf(lem) || lem.getLem().startsWith("break;");
  }

  @Data
  @Accessors(chain = true)
  static class Flow {
    private boolean breakFlow;
    private String id;
    private String predicate;
    private String predicateDescription;
    private String successFlowDescription;
    private List<PlantInstruction> successInstructions = new ArrayList<>();

    DataCondition toDataCondition() {
      var res = new DataCondition()
          .setId(id)
          .setCondition(predicate).setConditionDescription(predicateDescription)
          .setSuccessFlowDescription(successFlowDescription);
      successInstructions.stream().findFirst().map(pi -> pi.getId()).ifPresent(res::setTransition);
      return res;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Flow[%s]".formatted(id));
      successInstructions.stream().forEach(i -> {
        sb.append(" (%s) ->".formatted(i.getId()));
      });
      return sb.toString();
    }
  }


  @Override
  public void visit(Consumer<PlantInstruction> consumer) {
    consumer.accept(this);
    flows.stream().flatMap(f -> f.getSuccessInstructions().stream())
        .forEach(i -> i.visit(consumer));
  }

  private static final String FLOW_HEAD_TEMPLATE = "%s (%s) then";
  private static final String DESC_TEMPLATE = "(%s)";

  private String compileLem(Flow flow, String head) {
    StringBuilder sb = new StringBuilder();
    sb.append(head);
    if (flow.getSuccessFlowDescription() != null) {
      sb.append(" ").append(DESC_TEMPLATE.formatted(flow.getSuccessFlowDescription()));
    }
    return sb.toString();
  }

  @Data
  static class Details {
    private String condition;

    Details(Flow flow) {
      condition = flow.getPredicate();
    }
  }

  private boolean skipTail() {
    boolean res = true;
    String lastTransition = null;
    for (int i = 0; i < flows.size(); i++) {
      Flow flow = flows.get(i);
      List<PlantInstruction> instructions = flow.getSuccessInstructions();
      if (instructions.isEmpty()) {
        return false;
      }
      PlantInstruction last =
          instructions.stream().skip(instructions.size() - 1).findFirst().orElse(null);
      if (!flow.isBreakFlow()) {
        if (lastTransition == null) {
          lastTransition = last.getId();
        }
        res &= Objects.equals(lastTransition, last.getId());
      }
    }

    return res;
  }

  @Override
  public void print(TextView view) {
    for (int i = 0; i < flows.size(); i++) {
      Flow f = flows.get(i);
      if (i == 0) {
        PlantLine pl = new PlantLine(f.getId(), new Details(f),
            compileLem(f, FLOW_HEAD_TEMPLATE.formatted(IF, f.getPredicateDescription())));
        view.lemWithDescription(pl);
        List<PlantInstruction> toPrint = f.getSuccessInstructions();
        if (!toPrint.isEmpty()) {
          toPrint = skipTail() ? toPrint.subList(0, toPrint.size() - 1) : toPrint;
          toPrint.forEach(pi -> pi.print(view.tab()));
        }
        if (f.isBreakFlow()) {
          view.tab().line("break;");
        }
      } else if (i == flows.size() - 1) {
        view.line(compileLem(f, ELSE));
        List<PlantInstruction> toPrint = f.getSuccessInstructions();
        if (!toPrint.isEmpty()) {
          toPrint = skipTail() ? toPrint.subList(0, toPrint.size() - 1) : toPrint;
          toPrint.forEach(pi -> pi.print(view.tab()));
        }
        view.line(ENDIF);
      } else {
        PlantLine pl = new PlantLine(f.getId(), new Details(f),
            compileLem(f, FLOW_HEAD_TEMPLATE.formatted(ELSEIF, f.getPredicateDescription())));
        view.lemWithDescription(pl);
        f.getSuccessInstructions().forEach(pi -> pi.print(view.tab()));
      }
    }
  }

  private Activity compileState() {
    Activity res = new Activity();
    res.setType(Const.ActivityType.SWITCH);

    List<Flow> headFlows = flows.subList(0, flows.size() - 1);
    List<Flow> tailFlows = flows.subList(flows.size() - 1, flows.size());
    
    List<DataCondition> head = headFlows.stream().map(Flow::toDataCondition).toList();
    
    DataCondition defaultCondition = tailFlows.stream().findFirst().map(Flow::toDataCondition).orElseThrow();;
    DataCondition last = tailFlows.stream().findFirst()
        .filter(f -> !f.getSuccessInstructions().isEmpty())
        .map(Flow::toDataCondition)
        .or(() -> Optional.of(defaultCondition)).orElseThrow();
    
    nextConsumers.add(c -> {
      defaultCondition.setTransition(c);
    });
    
    head.stream().filter(c -> c.getTransition() == null).forEach(c -> nextConsumers.add(h -> {
      c.setTransition(h);
    }));
    
    res.setDefaultCondition(last);
    res.setId(ctx.nextId(getClass().getSimpleName()));
    res.setDataConditions(head);

    return res;
  }
  
  private List<Consumer<String>> nextConsumers = new ArrayList<>();


  @Override
  public Activity toActivity() {
    return impl;
  }

  @Override
  public String getId() {
    return impl.getId();
  }

  @Override
  public PlantInstruction getNext() {
    return next;
  }

}
