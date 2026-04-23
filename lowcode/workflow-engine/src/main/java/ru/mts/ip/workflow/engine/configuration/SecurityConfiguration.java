package ru.mts.ip.workflow.engine.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.mts.ip.workflow.engine.EngineConfigurationProperties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {

  private final EngineConfigurationProperties engineConfig;
  
  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    boolean sequrityEnabled = engineConfig.isSecurityEnabled();
    
    http.csrf(csrf -> csrf.disable())
      .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .exceptionHandling(eh -> eh.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    
    log.info("Security: " + (sequrityEnabled ? "enabled" : "disabled"));
    
    if(sequrityEnabled) {
      http.authorizeHttpRequests(authorize -> authorize
          .requestMatchers("/api/**").authenticated()
          .anyRequest().permitAll()
      ).oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));
    } else {
      http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
    }
    
    return http.build();
  }
  
}
