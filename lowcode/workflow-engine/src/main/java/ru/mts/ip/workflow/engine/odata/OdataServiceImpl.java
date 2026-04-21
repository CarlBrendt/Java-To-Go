package ru.mts.ip.workflow.engine.odata;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.dto.ReqOdataRemoteDescribe;

@Service
@RequiredArgsConstructor
public class OdataServiceImpl implements OdataService {

  private final OdataClient client;

  @Override
  public JsonNode describe(String xml) {
    return client.describe(xml);
  }

  @Override
  public JsonNode remoteDescribe(ReqOdataRemoteDescribe req) {
    return client.remoteDescribe(req);
  }

  @Override
  public String getRawEdm(ReqOdataRemoteDescribe reqRemoteDescribe) {
    return client.fetchRawEdm(reqRemoteDescribe);
  }

}
