package ru.mts.workflowmail.validation;

public class CronValidator {

  // Валидация минут (0-59)
  public static boolean validateMinutes(String minutes) {
    return validateCronField(minutes, 0, 59);
  }

  // Валидация часов (0-23)
  public static boolean validateHours(String hours) {
    return validateCronField(hours, 0, 23);
  }

  // Валидация дней месяца (1-31)
  public static boolean validateDayOfMonth(String dayOfMonth) {
    return validateCronField(dayOfMonth, 1, 31);
  }

  // Валидация месяцев (1-12)
  public static boolean validateMonth(String month) {
    return validateCronField(month, 1, 12);
  }

  // Валидация дней недели (0-6, где 0 = SUN)
  public static boolean validateDayOfWeek(String dayOfWeek) {
    return validateCronField(dayOfWeek, 0, 6);
  }

  // Общая функция для проверки cron-поля
  private static boolean validateCronField(String field, int min, int max) {
    // Разрешаем звездочку (все значения)
    if (field.equals("*")) {
      return true;
    }

    // Разрешаем перечисления (через запятую)
    if (field.contains(",")) {
      String[] parts = field.split(",");
      for (String part : parts) {
        if (!validateSingleValue(part, min, max)) {
          return false;
        }
      }
      return true;
    }

    // Разрешаем диапазоны (через дефис)
    if (field.contains("-")) {
      String[] range = field.split("-");
      if (range.length != 2) {
        return false;
      }
      return validateSingleValue(range[0], min, max) && validateSingleValue(range[1], min, max);
    }

    // Разрешаем шаги (через слэш)
    if (field.contains("/")) {
      String[] stepParts = field.split("/");
      if (stepParts.length != 2) {
        return false;
      }
      // Проверяем, что первая часть либо число, либо звездочка
      if (!stepParts[0].equals("*") && !validateSingleValue(stepParts[0], min, max)) {
        return false;
      }
      // Проверяем, что вторая часть — это число
      return validateSingleValue(stepParts[1], 1, max);
    }

    // Проверяем одиночное значение
    return validateSingleValue(field, min, max);
  }

  // Проверка одиночного значения
  private static boolean validateSingleValue(String value, int min, int max) {
    try {
      int intValue = Integer.parseInt(value);
      return intValue >= min && intValue <= max;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  public static void main(String[] args) {
    // Примеры использования
    System.out.println(validateMinutes("30")); // true
    System.out.println(validateMinutes("0-59")); // true
    System.out.println(validateMinutes("0,15,30,45")); // true
    System.out.println(validateMinutes("*/15")); // true
    System.out.println(validateMinutes("60")); // false

    System.out.println(validateHours("12")); // true
    System.out.println(validateHours("0-23")); // true
    System.out.println(validateHours("0,6,12,18")); // true
    System.out.println(validateHours("*/6")); // true
    System.out.println(validateHours("24")); // false

    System.out.println(validateDayOfMonth("1-31")); // true
    System.out.println(validateDayOfMonth("1,15,31")); // true
    System.out.println(validateDayOfMonth("*/10")); // true
    System.out.println(validateDayOfMonth("32")); // false

    System.out.println(validateMonth("1-12")); // true
    System.out.println(validateMonth("1,6,12")); // true
    System.out.println(validateMonth("*/3")); // true
    System.out.println(validateMonth("13")); // false

    System.out.println(validateDayOfWeek("0-6")); // true
    System.out.println(validateDayOfWeek("0,3,6")); // true
    System.out.println(validateDayOfWeek("*/2")); // true
    System.out.println(validateDayOfWeek("7")); // false
  }
}
