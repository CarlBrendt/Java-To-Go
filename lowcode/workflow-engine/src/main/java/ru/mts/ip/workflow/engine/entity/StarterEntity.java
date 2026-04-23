package ru.mts.ip.workflow.engine.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import ru.mts.ip.workflow.engine.Const;
import ru.mts.ip.workflow.engine.entity.converter.IbmmqStarterDetailsConverter;
import ru.mts.ip.workflow.engine.entity.converter.KafkaStarterDetailsConverter;
import ru.mts.ip.workflow.engine.entity.converter.MailStarterDetailsConverter;
import ru.mts.ip.workflow.engine.entity.converter.RabbitmqStarterDetailsConverter;
import ru.mts.ip.workflow.engine.entity.converter.SapStarterDetailsConverter;
import ru.mts.ip.workflow.engine.entity.converter.SchedulerStarterDetailsConverter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "starter",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "tenant_id", "type"}))
public class StarterEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;

  @NotNull
  @Column(name = "type")
  private String type;

  @NotNull
  @Column(name = "name")
  private String name;

  @NotNull
  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "description")
  private String description;

  @NotNull
  @Column(name = "desired_status")
  private String desiredStatus;

  @NotNull
  @Column(name = "actual_status")
  private String actualStatus;

  @CreationTimestamp
  @Column(name = "create_time")
  private OffsetDateTime createTime;

  @UpdateTimestamp
  @Column(name = "change_time")
  private OffsetDateTime changeTime;

  @Column(name = "start_date_time")
  private OffsetDateTime startDateTime;

  @Column(name = "end_date_time")
  private OffsetDateTime endDateTime;

  @JoinColumn(name = "start_workflow_id")
  @ManyToOne()
  private WorkflowDefinition workflowDefinition;

  @Column(name = "kafka_details", columnDefinition = "bytea")
  @Convert(converter = KafkaStarterDetailsConverter.class)
  private KafkaStarterDetails kafkaConsumer;

  @Column(name = "rabbitmq_details", columnDefinition = "bytea")
  @Convert(converter = RabbitmqStarterDetailsConverter.class)
  private RabbitmqStarterDetails rabbitmqConsumer;

  @Column(name = "ibmmq_details", columnDefinition = "bytea")
  @Convert(converter = IbmmqStarterDetailsConverter.class)
  private IbmmqStarterDetails ibmmqConsumer;

  @Column(name = "sap_details", columnDefinition = "bytea")
  @Convert(converter = SapStarterDetailsConverter.class)
  private SapStarterDetails sapInbound;

  @Column(name = "mail_details", columnDefinition = "bytea")
  @Convert(converter = MailStarterDetailsConverter.class)
  private MailStarterDetails mailConsumer;

  @Column(name = "scheduler_details", columnDefinition = "bytea")
  @Convert(converter = SchedulerStarterDetailsConverter.class)
  private SchedulerStarterDetails scheduler;

  @OneToOne(mappedBy = "starter")
  private WorkerEntity worker;

  @OneToMany(mappedBy = "starter", fetch = FetchType.LAZY)
  @BatchSize(size = 100)
  private List<StarterExclusionEntity> exclusions;

  @PreUpdate
  @PrePersist
  public void initDefaults() {
    tenantId = Optional.ofNullable(tenantId).orElse(Const.DEFAULT_TENANT_ID);
    desiredStatus = Optional.ofNullable(desiredStatus).orElse(Const.StarterStatus.STARTED);
    actualStatus = Optional.ofNullable(actualStatus).orElse(Const.StarterStatus.UNKNOWN);
  }
}
