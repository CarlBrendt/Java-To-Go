package ru.mts.ip.workflow.engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.converter.SapTaskDetailsConverter;
import ru.mts.ip.workflow.engine.exception.ClientError;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static ru.mts.ip.workflow.engine.Const.Task.INITIAL_RETRY_COUNT;
import static ru.mts.ip.workflow.engine.Const.Task.MAX_DELAY_MINUTES;
import static ru.mts.ip.workflow.engine.Const.Task.RETRY_DELAY_FACTOR;


@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "starter_task")
public class StarterTaskEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @Column(name = "type")
  @Enumerated(EnumType.STRING)
  private Type type;

  @Version
  @Column(name = "version")
  private Long version;
  
  @Column(name = "retry_count")
  private Integer retryCount;

  @Column(name = "locked_until_time")
  private OffsetDateTime lockedUntilTime;

  @Column(name = "state")
  @Enumerated(EnumType.STRING)  
  private State state;

  @Column(name = "overdue_time")
  private OffsetDateTime overdueTime;

  @Column(name = "create_time")
  private OffsetDateTime createTime;

  @JoinColumn(name = "start_workflow_id")
  @ManyToOne(fetch = FetchType.EAGER)
  private WorkflowDefinition workflowDefinition;

  @Column(name = "sap_details")
  @Convert(converter = SapTaskDetailsConverter.class)
  private SapTaskDetails sapTaskDetails;
  
  public StarterTaskEntity lock() {
    lockedUntilTime = OffsetDateTime.now().plusMinutes(1);
    return this;
  }

  public StarterTaskEntity complete() {
    lockedUntilTime = null;
    state = State.COMPLETE;
    return this;
  }

  public StarterTaskEntity manualStop() {
    lockedUntilTime = null;
    state = State.MANUAL_STOP;
    return this;
  }

  public StarterTaskEntity skip(ClientError clientError) {
    lockedUntilTime = null;
    state = State.SKIPPED;
    sapTaskDetails = sapTaskDetails == null ? new SapTaskDetails() : sapTaskDetails;
    sapTaskDetails.setErrorMessage(ExceptionUtils.getMessage(clientError));
    sapTaskDetails.setStackTrace(ExceptionUtils.getStackTrace(clientError));
    return this;
  }

  public StarterTaskEntity error(Exception ex) {
    lockedUntilTime = null;
    state = State.ERROR;
    overdueTime = calcNextOverdue();
    retryCount--;
    sapTaskDetails = sapTaskDetails == null ? new SapTaskDetails() : sapTaskDetails;
    sapTaskDetails.setErrorMessage(ex.getMessage());
    sapTaskDetails.setStackTrace(ExceptionUtils.getStackTrace(ex));
    return this;
  }
  
  private OffsetDateTime calcNextOverdue() {
    long a = INITIAL_RETRY_COUNT - retryCount + 1;
    long b = (long) (RETRY_DELAY_FACTOR * TimeUnit.MINUTES.toNanos(Const.Task.RETRY_DELAY_MINUTES));
    var plusNanos = Math.max((a + b), TimeUnit.MINUTES.toNanos(MAX_DELAY_MINUTES));
    return OffsetDateTime.now().plusNanos(plusNanos);
  }
  
  @PrePersist
  public void setDefaults() {
    createTime = Optional.ofNullable(createTime).orElse(OffsetDateTime.now());
    overdueTime = createTime;
    type = Type.WORKFLOW_START;
    state = State.NEW;
    retryCount = INITIAL_RETRY_COUNT;
  }
  
  
  public static enum Type{
    WORKFLOW_START
  }

  public static enum State{
    NEW, ERROR, COMPLETE, MANUAL_STOP, SKIPPED
  }

}
