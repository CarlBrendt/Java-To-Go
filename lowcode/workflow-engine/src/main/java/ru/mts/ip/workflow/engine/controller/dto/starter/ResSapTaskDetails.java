package ru.mts.ip.workflow.engine.controller.dto.starter;

import lombok.Data;
import ru.mts.ip.workflow.engine.service.blobstorage.BlobRef;

import java.util.UUID;

@Data
public class ResSapTaskDetails {
  
  private String errorMessage;
  private String stackTrace;
  
  private String idocId;
  private String idocTID;
  private String idocNumber;
  private UUID starterId;

  private BlobRef idocContentRef;
  private String idocContentBase64;
  
}
