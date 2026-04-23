package ru.mts.ip.workflow.engine;

public interface Internationalizer {
  String resolveMessage(String code, Object[] args);
  String resolveMessage(String code);
}
