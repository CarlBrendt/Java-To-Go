package ru.mts.ip.workflow.engine.configuration;

import java.net.InetSocketAddress;
import java.net.Proxy;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;

@Configuration
public class LLMClientConfiiguration {

  @Bean
  RestClient.Builder restClientBuilder(EngineConfigurationProperties props, OpenAiConnectionProperties openApiProps) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    if(props.isLocalProxyEnabled()) {
      InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
      Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
      factory.setProxy(proxy);
    }
    RestTemplate restTemplate = new RestTemplate();
    restTemplate.setRequestFactory(factory);
    return RestClient.builder(restTemplate).defaultHeader("X-ASGK-TOKEN", openApiProps.getApiKey().substring(3));
  }
  
  @Bean
  WebClient.Builder webClientBuilder(EngineConfigurationProperties props, OpenAiConnectionProperties openApiProps) {
    HttpClient httpClient =
        HttpClient.create();
    if(props.isLocalProxyEnabled()) {
      httpClient = httpClient
          .proxy(proxy -> proxy.type(reactor.netty.transport.ProxyProvider.Proxy.HTTP)
            .host("127.0.0.1")
            .port(8080)
          );
    }

    ReactorClientHttpConnector conn = new ReactorClientHttpConnector(httpClient);
    var webClientBuilder = WebClient.builder().clientConnector(conn);
    webClientBuilder.defaultHeader("X-ASGK-TOKEN", openApiProps.getApiKey().substring(3));
    return webClientBuilder;
  }
}
