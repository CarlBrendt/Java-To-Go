package ru.mts.workflowmail.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

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
