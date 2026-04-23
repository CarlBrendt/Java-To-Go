package ru.mts.workflowscheduler.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResStarterSearch(long total, List<ResStarterShortListValue> starters) {
}
