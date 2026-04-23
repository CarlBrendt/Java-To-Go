package ru.mts.ip.workflow.engine.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.mts.ip.workflow.engine.dto.ReqOdataRemoteDescribe;

@Tag(name = "Odata API")
@SecurityRequirement(name = "mts-isso")
public interface OdataApi {
  @PostMapping("/api/v1/odata/describe")
  JsonNode describe(@RequestBody String xml);
  
  @PostMapping("/api/v1/odata/remote-describe")
  JsonNode remoteDescribe(@RequestBody ReqOdataRemoteDescribe req);
  
  @SecurityRequirement(name = "mts-isso")
  @PostMapping(value = "/api/v1/odata/fetch-raw-edm", consumes = "application/json", produces = "application/xml")
  String fetchRawEdm(@RequestBody ReqOdataRemoteDescribe doc);
}
