package ru.mts.ip.workflow.engine.dto;

import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.Data;
import ru.mts.ip.workflow.engine.executor.ExternalProperties;

@Data
public class SapConnection {
  private String name;
  private String tenantId;
  private String description;
  private Map<String, JsonNode> props;
  
  public void applySecrets(ExternalProperties secrets) {
    if(props != null) {
      Optional.ofNullable(props.get("jco.client.user"))
          .filter(JsonNode::isTextual).map(JsonNode::asText)
          .map(secrets::get).ifPresent(v -> props.put("jco.client.user", new TextNode(v)));
      Optional.ofNullable(props.get("jco.client.passwd"))
        .filter(JsonNode::isTextual).map(JsonNode::asText)
        .map(secrets::get).ifPresent(v -> props.put("jco.client.passwd", new TextNode(v)));
    }
  }
  
  public void removeCredentials() {
    if(props != null) {
      props.remove("jco.client.user");
      props.remove("jco.client.passwd");
    }
  }
}
