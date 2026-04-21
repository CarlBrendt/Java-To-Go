package ru.mts.ip.workflow.engine.dto;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntpApi {
  
  private String versionId;
  private String subscriptionAuth;
  private Product product;
  private String ims;
  private Auth auth;
  private Stand stand;
  private Url url;
  private String serviceAccount;
  
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Product {
    private String id;
    private String name;
    private String productName;
    private String versionId;
    private String description;
    private List<Event> events = new ArrayList<>();
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Event {
    private String id;
    private String url;
    private String manifestFileId;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Auth {
    private String type;
    private String clientId;
    private String secret;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Stand {
    private String id;
    private String url;
    private String manifestFileId;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Url {
    private String method;
    private String path;
    private String summary;
  }
}
