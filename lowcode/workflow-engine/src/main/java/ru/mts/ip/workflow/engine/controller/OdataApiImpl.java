package ru.mts.ip.workflow.engine.controller;

import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.dto.ReqOdataRemoteDescribe;
import ru.mts.ip.workflow.engine.odata.OdataService;

@RestController
@RequiredArgsConstructor
public class OdataApiImpl implements OdataApi {

  private final OdataService odataService;
  
  @Override
  public JsonNode describe(String text) {
    return odataService.describe(text);
  }

  @Override
  public JsonNode remoteDescribe(ReqOdataRemoteDescribe req) {
    return odataService.remoteDescribe(req);
  }

  @Override
  public String fetchRawEdm(ReqOdataRemoteDescribe doc) {
    return odataService.getRawEdm(doc);
  }

}
