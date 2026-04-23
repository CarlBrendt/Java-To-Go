package ru.mts.workflowscheduler.share.validation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.workflowscheduler.exception.ErrorDescription;

import java.util.ArrayList;
import java.util.List;


@Data
@Accessors(chain = true)
public class JsonParseResult {
  private JsonNode json;
  private List<ErrorDescription> errors = new ArrayList<>();
}
