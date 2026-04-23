package ru.mts.workflowscheduler.service;

public interface Internationalizer {
  String resolveMessage(String code, Object[] args);
  String resolveMessage(String code);
}
