package ru.mts.ip.workflow.engine.service.blobstorage;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "db-file-storage")
public interface BlobStorageServiceClient {

  @PostMapping("/api/v1/files/upload")
  ResSavedBlob save(@RequestBody JsonNode content,
      @RequestParam(value = "filename", required = false) String filename,
      @RequestParam(value = "run-id", required = false) String runId,
      @RequestParam(value = "business-key", required = false) String businessKey,
      @RequestParam(value = "workflow-definition-id", required = false) UUID workflowDefinitionId,
      @RequestParam(value = "expiration-date", required = false) String expiredTime);

  @GetMapping("/api/v1/files/{id}")
  ResponseEntity<Resource> find(@PathVariable UUID id);
  
  @PostMapping("/api/v1/files/upload")
  ResSavedBlob save(@RequestBody Resource content,
      @RequestParam(value = "filename", required = false) String filename,
      @RequestParam(value = "run-id", required = false) String runId,
      @RequestParam(value = "business-key", required = false) String businessKey,
      @RequestParam(value = "workflow-definition-id", required = false) UUID workflowDefinitionId,
      @RequestParam(value = "expiration-date", required = false) String expiredTime);

}
