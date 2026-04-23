package ru.mts.workflowmail.service;

public interface Internationalizer {
  String resolveMessage(String code, Object[] args);
  String resolveMessage(String code);
}
