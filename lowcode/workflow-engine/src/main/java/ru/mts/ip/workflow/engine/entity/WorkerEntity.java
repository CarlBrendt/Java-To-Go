package ru.mts.ip.workflow.engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.scheduling.support.CronExpression;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.utility.CronHelper;
import ru.mts.ip.workflow.engine.utility.DateHelper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.mts.ip.workflow.engine.Const.StarterType.SCHEDULER;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "starter_worker")
@NamedEntityGraph(
    name = "Worker.withStarter",
    attributeNodes = @NamedAttributeNode(value = "starter", subgraph = "starter.exclusion"),
    subgraphs = {@NamedSubgraph(name = "starter.exclusion", attributeNodes = {@NamedAttributeNode("exclusions")})}
)
public class WorkerEntity {
  public static final int INITIAL_RETRY_COUNT = 100;
  public static final int RUNNING_TIMEOUT_MINUTES = 5;
  public static final int RETRY_DELAY_MINUTES = 3;
  public static final int LOCK_TIME_MINUTES = 5;
  public static final double RETRY_DELAY_FACTOR = 1.3;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @NotNull
  @Version
  @Column(name = "version")
  private Long version;

  @CreationTimestamp
  @Column(name = "create_time")
  private OffsetDateTime createTime;

  @UpdateTimestamp
  @Column(name = "change_time")
  private OffsetDateTime changeTime;

  @Column(name = "executor_id")
  private UUID executorId;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "starter_id")
  private StarterEntity starter;

  @Column(name = "locked_until_time")
  private OffsetDateTime lockedUntilTime;

  @Column(name = "overdue_time")
  private OffsetDateTime overdueTime;

  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "status")
  private String status;

  @Column(name = "error_message")
  private String errorMessage;

  @Column(name = "error_stack_trace", length = 4000)
  private String errorStackTrace;

  @PrePersist
  public void setDefaults() {
    status = Optional.ofNullable(status).orElse(Const.WorkerStatus.SCHEDULED_TO_START);
    retryCount = Optional.ofNullable(retryCount).orElse(INITIAL_RETRY_COUNT);
    overdueTime = Optional.ofNullable(overdueTime).orElse(DateHelper.now());
  }

  public WorkerEntity lock(UUID executorId) {
    lockedUntilTime = DateHelper.now().plusMinutes(LOCK_TIME_MINUTES);
    this.executorId = executorId;
    return this;
  }

  public WorkerEntity run() {
    lockedUntilTime = DateHelper.now().plusMinutes(RUNNING_TIMEOUT_MINUTES);
    status = Const.WorkerStatus.STARTED;
    return this;
  }

  public WorkerEntity success() {
    lockedUntilTime = DateHelper.now().plusMinutes(RUNNING_TIMEOUT_MINUTES);
    retryCount = INITIAL_RETRY_COUNT;
    overdueTime = calcScheduledOverdue().orElse(DateHelper.now());
    return this;
  }

  public WorkerEntity error(String errorMessage, String stacktrace) {
    this.lockedUntilTime = null;
    this.executorId = null;
    this.status = Const.WorkerStatus.ERROR;
    this.overdueTime = calcNextOverdue();
    this.retryCount--;
    this.errorMessage = StringUtils.truncate(errorMessage, 255);
    this.errorStackTrace = StringUtils.truncate(stacktrace, 4000);
    return this;
  }

  private OffsetDateTime calcNextOverdue() {
    long a = INITIAL_RETRY_COUNT - retryCount + 1;
    long b = (long) (RETRY_DELAY_FACTOR * TimeUnit.MINUTES.toNanos(RETRY_DELAY_MINUTES));
    var retryOverdueTime = DateHelper.now().plusNanos(a + b);
    return calcScheduledOverdue().filter(
            scheduledOverdueTime -> !retryOverdueTime.isBefore(scheduledOverdueTime))
        .orElse(retryOverdueTime);
  }

  private Optional<OffsetDateTime> calcScheduledOverdue(){
    if(SCHEDULER.equalsIgnoreCase(starter.getType())) {
      var scheduleDetails = starter.getScheduler();
      if (Const.SchedulerType.CRON.equals(scheduleDetails.getType())) {
        CronExpression expression = CronHelper.getCronExpression(scheduleDetails.getCron());
        return Optional.ofNullable(expression.next(DateHelper.now()));
      } else if (Const.SchedulerType.SIMPLE.equals(scheduleDetails.getType())) {
        Duration duration = Optional.ofNullable(scheduleDetails.getSimple())
            .map(SchedulerStarterDetails.SimpleDuration::getDuration)
            .orElseThrow();
        return Optional.of(DateHelper.now().plus(duration));
      }
      throw new RuntimeException("Unknown starter type: " + starter.getType());
    }
    return Optional.empty();
  }
}
