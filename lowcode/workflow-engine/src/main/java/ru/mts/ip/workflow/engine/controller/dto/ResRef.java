package ru.mts.ip.workflow.engine.controller.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@Accessors(chain = true)
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
public class ResRef {
  private UUID id;
  private String name;
  private Integer version;
  private String tenantId;
  private String startUrl;
}