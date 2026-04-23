package ru.mts.ip.workflow.engine.configuration;

import java.util.Locale;
import java.util.stream.Stream;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import lombok.RequiredArgsConstructor;
import ru.mts.ip.workflow.engine.Const.Errors2;

@Configuration
@RequiredArgsConstructor
public class LocalizationConfiguration {

  @Bean
  MessageSource messageSource() {
      ReloadableResourceBundleMessageSource messageSource
        = new ReloadableResourceBundleMessageSource();
      messageSource.setBasename("classpath:messages");
      messageSource.setDefaultEncoding("UTF-8");
      messageSource.setDefaultLocale(Locale.forLanguageTag("ru"));
      checkRULocalization(messageSource);
      return messageSource;
  }
  
  private void checkRULocalization(MessageSource messageSource) {
    Stream.of(Errors2.values()).forEach(val -> {
      try {
        Locale locale = LocaleContextHolder.getLocale();
        messageSource.getMessage(val.getErrorMessageAlias(), new Object[] {}, locale);
      } catch (NoSuchMessageException ex) {
        System.out.println(val.getErrorMessageAlias());
        System.out.println(val.getSolvingMessageAlias());
      }
    });
  }

  
}
