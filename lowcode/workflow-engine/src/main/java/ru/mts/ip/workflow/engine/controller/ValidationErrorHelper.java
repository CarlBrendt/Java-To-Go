package ru.mts.ip.workflow.engine.controller;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Internationalizer;
import ru.mts.ip.workflow.engine.WikiErrorPageProvider;

@Component
@RequiredArgsConstructor
public class ValidationErrorHelper {
  
  private final Internationalizer internationalizer;
  private final WikiErrorPageProvider wikiErrorPageProvider;

  public String resolveMessage(String code, Object[] args) {
    return internationalizer.resolveMessage(code, args);
  }
  
  public String resolveMessage(String code) {
    return internationalizer.resolveMessage(code);
  }
  
  public String findPageUrl(String code) {
    return wikiErrorPageProvider.findPageUrl(code);
  }
  
}
