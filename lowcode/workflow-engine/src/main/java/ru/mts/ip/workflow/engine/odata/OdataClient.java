package ru.mts.ip.workflow.engine.odata;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import com.fasterxml.jackson.databind.JsonNode;
import ru.mts.ip.workflow.engine.dto.ReqOdataRemoteDescribe;

@FeignClient(value = "wf-odata-client")
public interface OdataClient {
  @PostMapping("/api/v1/odata/describe")
  JsonNode describe(String text);
  @PostMapping("/api/v1/odata/remote-describe")
  JsonNode remoteDescribe(ReqOdataRemoteDescribe req);
  @PostMapping(value = "/api/v1/odata/fetch-raw-edm")
  String fetchRawEdm(ReqOdataRemoteDescribe doc);
}
