package ru.mts.ip.workflow.engine.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import ru.mts.ip.workflow.engine.entity.converter.JsonNodeByteArrayConverter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(
    name = "starter_exclusion"
)
public class StarterExclusionEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID id;


  @JoinColumn(name = "starter_id")
  @ManyToOne
  private StarterEntity starter;

  @CreationTimestamp
  private OffsetDateTime createTime;

  @Column(name = "details", columnDefinition = "bytea")
  @Convert(converter = JsonNodeByteArrayConverter.class)
  private JsonNode details;
}
