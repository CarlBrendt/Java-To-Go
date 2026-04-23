package ru.mts.workflowscheduler.share.validation;

import com.fasterxml.jackson.databind.JsonNode;

public interface Validation {
  public abstract ValidateDecision validate(Context ctx, JsonNode field);
}
