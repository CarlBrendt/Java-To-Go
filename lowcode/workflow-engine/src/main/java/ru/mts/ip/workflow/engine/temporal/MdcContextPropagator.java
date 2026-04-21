package ru.mts.ip.workflow.engine.temporal;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MdcContextPropagator implements ContextPropagator {

  private final DataConverter dataConverter = DefaultDataConverter.newDefaultInstance();
  private final Set<String> contextKeys;

  public MdcContextPropagator(Set<String> contextKeys) {
    this.contextKeys = Optional.ofNullable(contextKeys).orElse(Set.of());}

  @Override
  public String getName() {
    return "MdcContext";
  }

  @Override
  public Map<String, Payload> serializeContext(Object context) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap(); // Копируем текущий MDC
    if (mdcContext == null) {
      return Map.of();
    }

    Map<String, Payload> payloadMap = new HashMap<>();
    for (Map.Entry<String, String> entry : mdcContext.entrySet()) {
      if (contextKeys.contains(entry.getKey())) {
        Payload payload = dataConverter.toPayload(entry.getValue()).get(); // String -> Payload
        payloadMap.put(entry.getKey(), payload);
      }
    }
    return payloadMap;
  }

  @Override
  public Object deserializeContext(Map<String, Payload> header) {
    Map<String, String> mdcContext = new HashMap<>();
    for (Map.Entry<String, Payload> entry : header.entrySet()) {
      if (contextKeys.contains(entry.getKey())) {
        String value = dataConverter.fromPayload(entry.getValue(), String.class, String.class);
        if (Objects.nonNull(value)) {
          mdcContext.put(entry.getKey(), value);
        }
      }
    }
    return mdcContext;
  }

  @Override
  public Object getCurrentContext() {
    Map<String, String> map = new HashMap<>();
    contextKeys.stream()
        .filter(key -> Objects.nonNull(MDC.get(key)))
        .forEach(key -> map.put(key, MDC.get(key)));
    return map;
  }

  @Override
  public void setCurrentContext(Object context) {
    if (context instanceof Map contextMap) {
      Map<String, String> map = (Map<String, String>) contextMap;
      contextKeys.stream()
          .filter(key -> Objects.nonNull(map.get(key)))
          .forEach(key -> MDC.put(key, map.get(key)));
    }
  }
}
