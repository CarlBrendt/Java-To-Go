package ru.mts.workflowmail.share.validation;

import com.fasterxml.jackson.databind.JsonNode;

public interface Validation {
  public abstract ValidateDecision validate(Context ctx, JsonNode field);
}
