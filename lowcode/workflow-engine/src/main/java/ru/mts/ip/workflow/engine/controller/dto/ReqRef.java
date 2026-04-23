package ru.mts.ip.workflow.engine.controller.dto;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class ReqRef {
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
  private UUID id;
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
  private String name;
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
  private String version;
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
  private String stand;
  @Schema(requiredMode = RequiredMode.NOT_REQUIRED)
  private String tenantId;
}
