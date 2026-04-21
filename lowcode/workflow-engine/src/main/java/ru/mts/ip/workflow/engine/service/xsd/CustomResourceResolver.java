package ru.mts.ip.workflow.engine.service.xsd;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import lombok.SneakyThrows;
import ru.mts.ip.workflow.engine.dto.XsdValidation.XsdImport;

public class CustomResourceResolver  implements LSResourceResolver {

  private final Map<String, String> schemaResources = new HashMap<>();
  private final Decoder base64 = Base64.getDecoder();

  @SneakyThrows
  public CustomResourceResolver(List<XsdImport> imports) {
    imports.forEach(v -> {
      schemaResources.put(v.getXsdFileName(), new String(base64.decode(v.getBase64FileContent()), StandardCharsets.UTF_8));
    });
  }

  @Override
  public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
      String resourceContent = schemaResources.get(systemId);
      if (resourceContent != null) {
        return new CustomLSInput(publicId, systemId, resourceContent);
      }
      return null; 
  }
  
}
