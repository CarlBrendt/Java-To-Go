package ru.mts.ip.workflow.engine.utility;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class HeaderUtils {

  /**
   * Получить все заголовки из HttpServletRequest, начинающиеся с "x-mdc-"
   *
   * @param request HttpServletRequest объект
   * @return Map с ключами и значениями заголовков
   */
  public static Map<String, String> extractMdc(HttpServletRequest request) {
    Map<String, String> mdcHeaders = new HashMap<>();
    String mdcPrefix = "x-mdc-";

    // Получаем все имена заголовков
    Enumeration<String> headerNames = request.getHeaderNames();

    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        if (headerName.startsWith(mdcPrefix)) {
         var mdcName = headerName.substring(mdcPrefix.length());
          mdcHeaders.put(mdcName, request.getHeader(headerName));
        }
      }
    }

    return mdcHeaders;
  }
}
