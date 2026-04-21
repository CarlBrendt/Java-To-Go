package ru.mts.ip.workflow.engine.validation;

import com.fasterxml.jackson.databind.JsonNode;

public interface Validation {
  public abstract ValidateDecision test(Context ctx, JsonNode field);
}
