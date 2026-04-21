package ru.mts.ip.workflow.engine.controller.dto;

import lombok.Data;
import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;


@Data
public class ReqSearchStarterTask {
  private Integer page;
  private Integer pageSize;
  private StarterTaskEntity.State state;
}
