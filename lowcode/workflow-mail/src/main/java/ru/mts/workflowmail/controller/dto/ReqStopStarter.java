package ru.mts.workflowmail.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.validation.annotation.Validated;
import ru.mts.workflowmail.service.Const;

import java.util.UUID;


@Data
@Accessors(chain = true)
@Validated
public class ReqStopStarter {
  @NotNull
  @NotEmpty
  private String name;
  private String tenantId = Const.DEFAULT_TENANT_ID;
  @NotNull
  private UUID workflowId;
}
