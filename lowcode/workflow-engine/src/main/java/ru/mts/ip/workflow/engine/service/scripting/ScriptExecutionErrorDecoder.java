package ru.mts.ip.workflow.engine.service.scripting;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.Data;
import ru.mts.ip.workflow.engine.exception.ClientError;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;

public class ScriptExecutionErrorDecoder implements ErrorDecoder {
  
  private final ErrorDecoder errorDecoder = new Default();
  private final static ObjectMapper OM = new ObjectMapper();
  
  @Override
  public Exception decode(String methodKey, Response response) {
    if(response.status() == 400) {
      try (InputStream bodyIs = response.body().asInputStream()) {
        ScriptExecutionErrors errors = OM.readValue(bodyIs, ScriptExecutionErrors.class);
        return ClientError.compiled(errors.getErrorDescriptions());
      } catch (IOException e) {
        return new Exception(e.getMessage());
      }
    } else {
      return errorDecoder.decode(methodKey, response);
    }
  }
  
  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ScriptExecutionErrors {
    private List<ErrorDescription> errorDescriptions;
  }

}
