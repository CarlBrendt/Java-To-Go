package ru.mts.ip.workflow.engine.controller.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.dto.SapInbound;

@Data
public class ReqSapInboundStarter {
  private ReqRefVersionless inboundRef;
  private SapInbound inboundDef;
}
