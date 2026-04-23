package ru.mts.workflowscheduler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import ru.mts.workflowscheduler.config.EngineConfigurationProperties;

@Component
@RequiredArgsConstructor
public class WikiErrorPageProvider {

  private final EngineConfigurationProperties props;
  public String findPageUrl(String code) {
    try {
      return UriComponentsBuilder.fromHttpUrl(props.getWorkflowWikiErrorsUrlPattern()).buildAndExpand(code).toString();
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
