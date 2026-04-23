package ru.mts.ip.workflow.engine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.mts.ip.workflow.engine.temporal.RestCallInput.RestCallConfig.AuthDef;

@Data
@NoArgsConstructor
public class ReqOdataRemoteDescribe {
  private String odataUrl;
  private AuthDef authDef;
  public ReqOdataRemoteDescribe(String odataUrl, AuthDef authDef) {
    this.odataUrl = odataUrl;
    this.authDef = authDef;
  }
}
