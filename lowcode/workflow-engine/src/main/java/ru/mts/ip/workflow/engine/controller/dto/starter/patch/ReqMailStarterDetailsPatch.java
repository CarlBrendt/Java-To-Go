package ru.mts.ip.workflow.engine.controller.dto.starter.patch;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Data
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ReqMailStarterDetailsPatch {
  private Optional<MailConnectionPatch> connectionDef;
  private Optional<ReqMailFilterPatch> mailFilter;
  private Optional<ReqMailPollConfigPatch> pollConfig;
  private Optional<JsonNode> outputTemplate;


  @Data
  public static final class MailConnectionPatch {
    private Optional<String> protocol;
    private Optional<String> host;
    private Optional<Integer> port;
    private Optional<ReqMailAuthPatch> mailAuth;
  }


  @Data
  public static final class ReqMailFilterPatch {
    private Optional<List<String>> senders;
    private Optional<List<String>> subjects;
    private Optional<OffsetDateTime> startMailDateTime;
  }


  @Data
  public static final class ReqMailAuthPatch {
    private Optional<String> username;
    private Optional<String> password;
    private Optional<ReqMailCertificatePatch> certificate;
  }


  @Data
  public static final class ReqMailCertificatePatch {
    private Optional<String> trustStoreType;
    private Optional<String> trustStoreCertificates;
  }


  @Data
  public static final class ReqMailPollConfigPatch {
    private Optional<Long> pollDelaySeconds;
    private Optional<Integer> maxFetchSize;

  }
}
