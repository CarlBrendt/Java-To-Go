package ru.mts.ip.workflow.engine.controller.dto.starter;

import ru.mts.ip.workflow.engine.entity.StarterTaskEntity;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResStarterTask(UUID id, Integer retryCount, StarterTaskEntity.State state,
    LocalDateTime overdueTime, LocalDateTime createTime, UUID workflowDefinitionToStartId,
    ResSapTaskDetails sapTaskDetails
) {

  public record ResSapTaskDetails(String errorMessage, String stackTrace,
      String idocId, String idocTID, String idocNumber, UUID starterId,
      BlobRef idocContentRef, String idocContentBase64) {

  }
}
