package ru.mts.ip.workflow.engine;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InternationalizerImpl implements Internationalizer{

  private final MessageSource messageSource;
  
  @Override
  public String resolveMessage(String code) {
    return resolveMessage(code, null);
  }
  
  @Override
  public String resolveMessage(String code, Object[] args) {
    try {
      Locale locale = LocaleContextHolder.getLocale();
      String message = messageSource.getMessage(code, args, locale);
      return message;
    } catch (NoSuchMessageException ex) {
      return code;
    }
  }

}
