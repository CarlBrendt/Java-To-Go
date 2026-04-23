package ru.mts.workflowmail.utility;


import lombok.experimental.UtilityClass;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;


@UtilityClass
public class DateHelper {

  public static final String DEFAULT_DATE_TIME_PATTERN = "dd-MM-yyyy'T'HH:mm:ss";
  public static final String ZONED_DATE_TIME_PATTERN = "d.MM.y HH:m:s XXX'['VV']'";
  public static final String ISO_OFFSSET_DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ssXXX";
  public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_FORMATTER =
      DateTimeFormatter.ISO_DATE_TIME;
  private static final DateTimeFormatter QUEUE_DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private static final ZoneOffset DEFAULT_CLIENT_ZONE_OFFSET = ZoneOffset.of("+03:00");

  public OffsetDateTime now() {
    return OffsetDateTime.now(Clock.systemUTC());
  }

  public OffsetDateTime today() {
    ZoneOffset systemOffset = ZoneId.systemDefault().getRules().getOffset(Instant.now());
    OffsetDateTime today = OffsetDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, systemOffset);
    return today;
  }

  public OffsetDateTime parseISO(String text) {
    OffsetDateTime odt = OffsetDateTime.parse(text, ISO_OFFSET_DATE_TIME_FORMATTER);
    return odt.withOffsetSameInstant(ZoneOffset.UTC);
  }

  public boolean testISOValidDate(String text) {
    try {
      OffsetDateTime.parse(text, ISO_OFFSET_DATE_TIME_FORMATTER);
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String asTextQueueLocalDateTime(OffsetDateTime ldt) {
    return ldt.format(QUEUE_DATE_TIME_FORMATTER);
  }


  public String asTextISO(OffsetDateTime odt, String ifNull) {
    if (odt == null) {
      return ifNull;
    } else {
      return asTextISO(odt);
    }
  }

  public String asTextISO(Instant instant, String ifNull) {
    if (instant == null) {
      return ifNull;
    } else {
      return asTextISO(instant.atOffset(DEFAULT_CLIENT_ZONE_OFFSET));
    }
  }

  public String asTextISO(Instant instant) {
    return asTextISO(instant, null);
  }

  public String asTextISO(Date date) {

    return date == null ? null : asTextISO(date.toInstant(), null);
  }

  public String asTextISO(OffsetDateTime odt) {
    Objects.requireNonNull(odt);
    odt = odt.withOffsetSameInstant(DEFAULT_CLIENT_ZONE_OFFSET);
    return odt.format(ISO_OFFSET_DATE_TIME_FORMATTER);
  }

  public LocalDateTime parseLocalDateTime(String dateText, String format) {
    return LocalDateTime.parse(dateText, DateTimeFormatter.ofPattern(format));
  }

  public String asTextUTC(OffsetDateTime odt, String ifNull) {
    if (odt == null) {
      return ifNull;
    }
    return asTextUTC(odt);
  }

  public String asTextUTC(OffsetDateTime odt) {
    Objects.requireNonNull(odt);
    OffsetDateTime utc = odt.withOffsetSameInstant(ZoneOffset.UTC);
    return utc.format(ISO_OFFSET_DATE_TIME_FORMATTER);
  }

}
