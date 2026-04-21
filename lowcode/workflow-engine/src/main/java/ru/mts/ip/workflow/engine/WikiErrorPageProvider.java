package ru.mts.ip.workflow.engine;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WikiErrorPageProvider {

  private final EngineConfigurationProperties props;
  public String findPageUrl(String code) {
    return UriComponentsBuilder.fromHttpUrl(props.getWorkflowWikiErrorsUrlPattern()).buildAndExpand(code).toString();
  }
  
}
