package ru.mts.ip.workflow.engine.validation;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.exception.ErrorDescription;


@Data
@Accessors(chain = true)
public class JsonParseResult {
  private JsonNode json;
  private List<ErrorDescription> errors = new ArrayList<>();
}
