package ru.mts.ip.workflow.engine.utility;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class Utils {

  public static String getFileExtension(String filename) {
    if (filename == null) {
      return null;
    }
    int dotIndex = filename.lastIndexOf(".");
    if (dotIndex >= 0) {
      return filename.substring(dotIndex + 1).toLowerCase();
    }
    return "";
  }
  
  public static String selectInterval(String source, Interval interval) {
    return source.substring(interval.getStart().start, interval.end.end);
  }
  
  public static String replacePlaceholders(String source, Function<String, String> replacer, Pattern leftPattern, Pattern rightPattern, int rightSize) {
    
    StringBuilder result = new StringBuilder(source);
    List<Area> anchors = findAnchors(source, leftPattern, rightPattern);
    Deque<Area> deque = new ArrayDeque<>();
    int diff = 0;
    for(Area anchor : anchors) {
      if(anchor.left) {
        deque.add(anchor);
      } else {
        if(!deque.isEmpty()) {
          Area left = deque.pollLast();
          if(deque.isEmpty()) {
            int leftStart = left.start;
            int leftEnd = left.end;
            
            int toReplaceLeftIndex = diff + leftEnd;
            int toReplaceRightIndex = diff + anchor.end - rightSize;
            
            String toReplace = result.substring(toReplaceLeftIndex, toReplaceRightIndex);
            String replacedBy = replacer.apply(toReplace);  
            
            int leftAnchorLength = leftEnd - leftStart;
            int rightAnchorLength = anchor.end - anchor.start;
            int anchorLength = leftAnchorLength + rightAnchorLength;

            result = result.replace(leftStart + diff, anchor.end + diff,  replacedBy);
            int beforeReplaceLength = anchorLength + toReplace.length();
            int afterReplaceLength = replacedBy.length();
            
            diff += afterReplaceLength - beforeReplaceLength;
          }
        }
      }
    }
    return result.toString();
  }
  
  public static List<Area> findAnchors(String source, Pattern leftPattern, Pattern rightPattern) {
    List<Area> areas = new ArrayList<>();
    Matcher leftAnchorMatcher = leftPattern.matcher(source);
    Matcher rightAnchorMatcher = rightPattern.matcher(source);
    
    while(leftAnchorMatcher.find()) {
      areas.add(new Area(leftAnchorMatcher.start(), leftAnchorMatcher.end(), true));
    }
    while(rightAnchorMatcher.find()) {
      areas.add(new Area(rightAnchorMatcher.start(), rightAnchorMatcher.end(), false));
    }
    Collections.sort(areas);
    return areas;
  }

  public static List<Interval> findIntervals(String source, Pattern leftPattern, Pattern rightPattern) {
    List<Area> anchors = findAnchors(source, leftPattern, rightPattern);
    List<Interval> result = new ArrayList<>();
    Area left = null;
    Area right = null;
    for(int i = 0 ; i < anchors.size(); i++) {
      if(left != null && right != null) {
        result.add(new Interval(left, right));
        left = null;
        right = null;
      }
      var next = anchors.get(i);
      if(next.left) {
        if(left == null) {
          left = next;
        }
      } else {
        if(left != null) {
          right = next;
        }
      }
    }

    if(left != null) {
      result.add(new Interval(left, right));
    }
    
    return result;
  }
  
  @Getter
  @RequiredArgsConstructor
  public static class Interval {
    private final Area start;
    private final Area end;
    @Override
    public String toString() {
      return  "[%d,%s)".formatted(start.start, end == null ? "?" : end.end);
    }
    
    public boolean isCompleted() {
      return start != null && end != null;
    }
  }
  
  @Getter
  @RequiredArgsConstructor
  public static class Area implements Comparable<Area> {
    private final int start;
    private final int end;
    private final boolean left;
    @Override
    public int compareTo(Area o) {
      return Integer.compare(start, o.start);
    }
    @Override
    public String toString() {
      return left ? "l[%d,%d)".formatted(start, end) : "r[%d,%d)".formatted(start, end);
    }
  }
  
  
}
