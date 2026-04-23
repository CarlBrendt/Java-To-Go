package ru.mts.workflowmail.filter;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FeignMdcRequestInterceptor implements RequestInterceptor {

  @Override
  public void apply(RequestTemplate requestTemplate) {
    Map<String, String> mdcMap = MDC.getCopyOfContextMap();
    if (mdcMap != null) {
      mdcMap.forEach((key, value) -> requestTemplate.header("x-mdc-" + key, value));
    }
  }
}
