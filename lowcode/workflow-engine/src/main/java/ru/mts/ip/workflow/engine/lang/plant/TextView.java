package ru.mts.ip.workflow.engine.lang.plant;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.service.WorkflowHistory;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ActivityExecutionStatus;
import ru.mts.ip.workflow.engine.service.WorkflowHistory.ExecutionStat;

@Data
@Accessors(chain = true)
public class TextView {

  private static final int TAB_SIZE = 4;
  private int tabCount;
  private StringBuilder sb = new StringBuilder();
  private PlantTail tail;
  private WorkflowHistory hist;


  public TextView(PlantTail tail, WorkflowHistory hist) {
    this.tail = tail;
    this.hist = hist;
  }

  public TextView(PlantTail tail) {
    this(tail, null);
  }

  public TextView() {
    this(new PlantTail(), null);
  }

  public TextView line(String line) {
    sb.append(" ".repeat(tabCount)).append(line).append(Const.NEW_LINE);
    return this;
  }

  public TextView lemWithDescription(PlantLine pl) {
    sb.append(" ".repeat(tabCount));
    tail.getActivities().put(pl.getId(), pl.getComment());
    sb.append(Const.Plant.LQ_COMMENT).append(pl.getId()).append(Const.Plant.RQ_COMMENT);
    String lem = pl.getLem();
    if (hist != null && pl.isSupportColoring()) {
      ExecutionStat state = hist.getHist().get(pl.getId());
      if (state != null) {
        lem = lem.substring(0, lem.indexOf(";"));
        if (state.getCompleteCount() > 0) {
          sb.append("#lightGreen");
          if (state.getCurrentState() == ActivityExecutionStatus.IN_PROGRESS) {
            lem += " (%d*);".formatted(state.getCompleteCount());
          } else {
            lem += " (%d);".formatted(state.getCompleteCount());
          }
        } else {
          if (state.getCurrentState() == ActivityExecutionStatus.IN_PROGRESS) {
            lem += " (*);".formatted(state.getCompleteCount());
          } else {
            lem += ";".formatted(state.getCompleteCount());
          }
        }
      }
    }
    sb.append(lem).append(Const.NEW_LINE);
    return this;
  }

  public TextView tab() {
    return new TextView().setTabCount(tabCount + TAB_SIZE).setSb(sb).setTail(tail).setHist(hist);
  }

  @Override
  public String toString() {
    sb.append(Const.Plant.LQ_COMMENT).append(Const.NEW_LINE);
    sb.append(PlantUtils.prettyWriteValueAsString(tail)).append(Const.NEW_LINE);
    sb.append(Const.Plant.RQ_COMMENT);
    return sb.toString();
  }

}
