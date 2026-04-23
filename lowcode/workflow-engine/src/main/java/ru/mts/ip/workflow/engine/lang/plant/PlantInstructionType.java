package ru.mts.ip.workflow.engine.lang.plant;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import ru.mts.ip.workflow.engine.Const;

@AllArgsConstructor
public enum PlantInstructionType {

  START(Start::canParse), 
  ACTIVITY(PlantActivity::canParse), 
  REPEAT(t -> t.startsWith(Const.Plant.REPEAT)), 
  IF(If::canParse);

  private final Predicate<String> preadicate;

  private boolean canParese(String lem) {
    return preadicate.test(lem);
  }

  public static Optional<PlantInstructionType> determineType(String lem) {
    List<PlantInstructionType> candidates =
        Stream.of(PlantInstructionType.values()).filter(t -> t.canParese(lem)).toList();
    if (candidates.size() > 1) {
      throw new IllegalStateException();
    } else if (candidates.size() == 1) {
      return Optional.of(candidates.get(0));
    }
    return Optional.empty();
  }

  static Optional<PlantInstruction> createInstruction(PlantCompilationContext lineSource) {
    return lineSource.current().map(line -> {
      var lem = line.getLem();
      var type = PlantInstructionType.determineType(lem)
          .orElseThrow(() -> new PlantUmlSyntaxError(String.format("Unknown lem[%s]", lem)));
      return switch (type) {
        case START -> new Start(lineSource);
        case IF -> new If(lineSource);
        case ACTIVITY -> new PlantActivity(lineSource);
        case REPEAT -> new Repeat(lineSource);
      };
    }).or(() -> Optional.empty());
  }

  static Optional<PlantInstruction> createInstruction(UmlPath root) {
    if (root.isRepeat()) {
      return Optional.of(new Repeat(root));
    } else if (root.isActivity()) {
      return Optional.of(new PlantActivity(root));
    } else if (root.isIf()) {
      return Optional.of(new If(root));
    } else {
      throw new IllegalStateException();
    }
  }


  static Optional<PlantInstruction> createInstruction(String src) {
    return createInstruction(new PlantCompilationContext(src));
  }

}
