package ru.mts.ip.workflow.engine.odata;

import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.ReqOdataRemoteDescribe;

public interface OdataService {
  JsonNode describe(String xml);
  JsonNode remoteDescribe(ReqOdataRemoteDescribe req);
  String getRawEdm(ReqOdataRemoteDescribe reqRemoteDescribe);
}
