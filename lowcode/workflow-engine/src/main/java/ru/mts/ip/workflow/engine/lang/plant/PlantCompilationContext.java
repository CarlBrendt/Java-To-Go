package ru.mts.ip.workflow.engine.lang.plant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;
import lombok.Getter;
import ru.mts.ip.workflow.engine.Const;

public class PlantCompilationContext {

  private final List<PlantLine> lines = new ArrayList<>();
  private int currentPosition = 0;
  private final IdGen idGen = new IdGen();

  @Getter
  private PlantTail tail;
  PlantInstruction result;
  @Getter
  private String commentSection;

  String nextId(String prefix, String exists) {
    return idGen.nextId(prefix, exists);
  }

  String nextId(String prefix) {
    return nextId(prefix, null);
  }

  public Optional<PlantInstruction> tryReadArgument(Predicate<PlantLine> pred) {
    return current().filter(pred).flatMap((c) -> PlantInstructionType.createInstruction(this));
  }

  public final Optional<PlantLine> eatLine(Predicate<PlantLine> pred) {
    return current().filter(pred).flatMap((c) -> eat());
  }

  public Optional<PlantLine> eat() {
    PlantLine res = currentPosition < lines.size() ? lines.get(currentPosition++) : null;
    return Optional.ofNullable(res);
  }

  public Optional<PlantLine> current() {
    PlantLine res = currentPosition < lines.size() ? lines.get(currentPosition) : null;
    return Optional.ofNullable(res);
  }

  public PlantCompilationContext(String text) {
    tail = PlantUtils.readInstruction(PlantUtils.findTailJsonText(text), PlantTail.class);
    try (Scanner sc = new Scanner(text)) {
      while (sc.hasNextLine()) {
        toPlantLine(sc.nextLine()).ifPresent(lines::add);
      }
    }
  }

  public PlantLine eatRequired(Predicate<PlantLine> predicate, String... expected) {
    return eat().filter(predicate)
        .orElseThrow(() -> new PlantUmlSyntaxError(
            String.format("Unexpected instruction[%s]. Required any of %s",
                current().map(PlantLine::getLem).orElse(""), Arrays.toString(expected))));
  }

  public PlantLine currentRequired(Predicate<PlantLine> predicate, String... expected) {
    return current().filter(predicate)
        .orElseThrow(() -> new PlantUmlSyntaxError(
            String.format("Unexpected instruction[%s]. Required any of %s",
                current().map(PlantLine::getLem).orElse(""), Arrays.toString(expected))));
  }

  private JsonNode findDescription(String text) {
    if (!tail.getActivities().containsKey(text)) {
      throw new PlantUmlSyntaxError("description is not found for: %s".formatted(text));
    } else {
      return tail.getActivities().get(text);
    }
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class IdFiled {
    private String id;
  }

  private Optional<PlantLine> toPlantLine(String text) {
    text = text.trim();
    PlantLine res = null;
    if (text.contains(Const.Plant.RQ_COMMENT)) {
      String[] chunks = text.split(Const.Plant.RQ_COMMENT);
      if (chunks.length == 2) {
        String[] sub = chunks[0].split(Const.Plant.LQ_COMMENT);
        if (sub.length == 2) {
          String id = sub[1];
          JsonNode desc = findDescription(id);
          res = new PlantLine(id, desc, chunks[1].trim());
        } else {
          res = new PlantLine(null, null, chunks[1].trim());
        }
      }
    } else if (!text.isBlank()) {
      res = new PlantLine(null, null, text.trim());
    }
    return Optional.ofNullable(res);
  }

  @Override
  public String toString() {
    return "current: " + current().orElse(null);
  }

}
