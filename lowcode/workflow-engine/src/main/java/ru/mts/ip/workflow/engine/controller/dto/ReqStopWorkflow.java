package ru.mts.ip.workflow.engine.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import static ru.mts.ip.workflow.engine.Const.DEFAULT_TENANT_ID;
import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReqStopWorkflow {

  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private ReqStopRef workflowRef;
  @NotBlank
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  private String businessKey;
  @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = "false")
  private boolean terminate;

  @Data
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ReqStopRef {
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String name;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer version;
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, defaultValue = DEFAULT_TENANT_ID)
    private String tenantId;
  }

}
