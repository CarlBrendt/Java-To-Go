package ru.mts.workflowscheduler.share.script;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.Data;
import ru.mts.workflowscheduler.exception.ErrorDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ScriptExecutionErrorDecoder implements ErrorDecoder {

  private final ErrorDecoder errorDecoder = new Default();
  private final static ObjectMapper OM = new ObjectMapper();

  @Override
  public Exception decode(String methodKey, Response response) {
    if(response.status() == 400) {
      try (InputStream bodyIs = response.body().asInputStream()) {
        ScriptExecutionErrors errors = OM.readValue(bodyIs, ScriptExecutionErrors.class);
        ClientError err = ClientError.compiled(errors.getErrorDescriptions());
        return err;
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
