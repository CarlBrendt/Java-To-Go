package ru.mts.workflowmail.service.blobstorage;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class BlobSaveOptions {
  private String fileName;
  private String runId;
  private String businessKey;
  private UUID workflowDefinitionId;
  private OffsetDateTime expirationDate;
}
