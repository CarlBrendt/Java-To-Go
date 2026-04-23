package ru.mts.workflowmail.config;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.util.AntPathMatcher;

@Configuration
@RequiredArgsConstructor
public class ObservationConfiguration {

  private final EngineConfigurationProperties engineConfig;

  @Bean
  ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
      return new ObservedAspect(observationRegistry);
  }

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> skipActuatorEndpointsFromObservation() {
      AntPathMatcher pathMatcher = new AntPathMatcher("/");
      return (registry) -> registry.observationConfig().observationPredicate((name, context) -> {
          if (context instanceof ServerRequestObservationContext observationContext) {
              return pathMatcher.match("/api/**", observationContext.getCarrier().getRequestURI());
          } else {
              return true;
          }
      });
  }

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> skipSecuritySpansFromObservation() {
      return (registry) -> registry.observationConfig()
          .observationPredicate((name, context) -> !name.startsWith("spring.security"));
  }

  @Bean
  Filter traceIdInResponseFilter(Tracer tracer) {
      return (request, response, chain) -> {
        try {
          Span currentSpan = tracer.currentSpan();
          if (currentSpan != null) {
            HttpServletResponse resp = (HttpServletResponse) response;
            resp.addHeader("trace-id", currentSpan.context().traceId());
          }
        } finally {
          chain.doFilter(request, response);
        }
      };
  }


  @Bean
  OpenTelemetry openTelemetry() {

    Resource resource = Resource.getDefault().toBuilder()
        .put(ResourceAttributes.SERVICE_NAME, engineConfig.getOpentelemetryServiceName())
        .put(ResourceAttributes.SERVICE_VERSION, engineConfig.getOpentelemetryServiceVersion())
        .build();

    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(OtlpHttpSpanExporter.builder().setEndpoint(engineConfig.getOpentelemetryTraceExporterEndpoint())
            .build()))
        .setResource(resource)
        .build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
        .buildAndRegisterGlobal();

    return openTelemetry;

  }

}
