package ru.mts.workflowmail.controller.dto;

import java.util.List;

public record ResStarterSearch(long total, List<ResStarterShortListValue> starters) {
}
