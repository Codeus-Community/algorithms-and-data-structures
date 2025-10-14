package org.codeus.hyperloglog.common;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {

  public final static ZoneId ZONE_ID = ZoneOffset.systemDefault();
  public static final DateTimeFormatter DATE_HOUR_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH").withZone(ZONE_ID);

  /**
   * Parses the given ISO-8601 timestamp string into a `ZonedDateTime` object.
   * If the input is null or blank, the current date and time in the system's default time zone is returned.
   *
   * @param atIso the ISO-8601 timestamp string to parse; if null or blank, the current time is used.
   * @return a `ZonedDateTime` object representing the parsed timestamp or the current time.
   */
  public static ZonedDateTime parseOrNow(String atIso) {
    if (atIso == null || atIso.isBlank()) {
      return ZonedDateTime.now(ZONE_ID);
    }
    // Accepts full ISO-8601; if no zone provided, assume UTC
    ZonedDateTime zdt = ZonedDateTime.parse(atIso);
    return zdt.withZoneSameInstant(ZONE_ID);
  }
}
