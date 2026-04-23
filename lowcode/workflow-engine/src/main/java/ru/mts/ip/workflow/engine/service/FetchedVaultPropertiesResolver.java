package ru.mts.ip.workflow.engine.service;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.mts.ip.workflow.engine.utility.Utils;

import java.util.Optional;
import java.util.regex.Pattern;

public class FetchedVaultPropertiesResolver {
  
  public static final String LEFT_ANCHOR = "f_secret{";
  public static final String RGHT_ANCHOR = "}";
  public static final Pattern LEFT_ANCHOR_PATTERN = Pattern.compile("f_secret\\{");
  public static final Pattern RIGHT_ANCHOR_PATTERN = Pattern.compile("\\}");
  
  public Optional<String> findValue(String expression) {
    if(containsPlaceholder(expression) && !isStringTemplate(expression)) {
      return Optional.of(expression.substring(LEFT_ANCHOR.length(), expression.length() - getRigthSize()));
    }
    return Optional.empty();
  }
  
  public String resolvePlaceholders(String expression, SimpleCrypt sc) {
    return Utils.replacePlaceholders(expression, script -> String.valueOf(sc.decryp(script)), LEFT_ANCHOR_PATTERN, RIGHT_ANCHOR_PATTERN, getRigthSize());
  }
  
  @Data
  @Accessors(chain = true)
  public static class VaultEntry{
    private String raw;
    private String path;
    private String field;
  }
  
  boolean isStringTemplate(String expression) {
    String result = Utils.replacePlaceholders(expression, script -> "", LEFT_ANCHOR_PATTERN, RIGHT_ANCHOR_PATTERN, getRigthSize());
    return !result.isEmpty();
    
  }

  public boolean containsPlaceholder(String script) {
    var intervals = Utils.findIntervals(script, LEFT_ANCHOR_PATTERN, RIGHT_ANCHOR_PATTERN);
    return !intervals.isEmpty();
  }


  public int getRigthSize() {
    return RGHT_ANCHOR.length();
  }
  
  
  
}
