package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class LocalDateTimeTest {

  @Test
  void constructor_fieldValues() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 45, 123);
    assertEquals(2020, dt.getYear());
    assertEquals(6, dt.getMonthOfYear());
    assertEquals(15, dt.getDayOfMonth());
    assertEquals(10, dt.getHourOfDay());
    assertEquals(30, dt.getMinuteOfHour());
    assertEquals(45, dt.getSecondOfMinute());
    assertEquals(123, dt.getMillisOfSecond());
  }

  @Test
  void constructor_invalidHour_throws() {
    assertThrows(
        IllegalFieldValueException.class, () -> new LocalDateTime(2020, 6, 15, 24, 0, 0, 0));
  }

  @Test
  void equals_requiresSameFieldsAndChronology() {
    LocalDateTime a = new LocalDateTime(2020, 6, 15, 10, 0, 0, 0);
    LocalDateTime b = new LocalDateTime(2020, 6, 15, 10, 0, 0, 0);
    LocalDateTime c = new LocalDateTime(2020, 6, 15, 11, 0, 0, 0);
    assertEquals(a, b);
    assertNotEquals(a, c);
  }

  @Test
  void plusHours_rollsOverIntoNextDay() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 23, 0, 0, 0);
    LocalDateTime result = dt.plusHours(2);
    assertEquals(16, result.getDayOfMonth());
    assertEquals(1, result.getHourOfDay());
  }

  @Test
  void plusMonths_clampsDayOfMonth() {
    LocalDateTime dt = new LocalDateTime(2013, 1, 31, 12, 0, 0, 0);
    LocalDateTime result = dt.plusMonths(1);
    assertEquals(28, result.getDayOfMonth());
    assertEquals(12, result.getHourOfDay());
  }

  @Test
  void toLocalDate_and_toLocalTime_splitCorrectly() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 45, 123);
    LocalDate date = dt.toLocalDate();
    LocalTime time = dt.toLocalTime();
    assertEquals(new LocalDate(2020, 6, 15), date);
    assertEquals(new LocalTime(10, 30, 45, 123), time);
  }

  @Test
  void toString_hasNoTimeZoneOffset() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 0, 0);
    assertEquals("2020-06-15T10:30:00.000", dt.toString());
  }

  @Test
  void toDateTime_appliesZone() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 0, 0);
    DateTime withZone = dt.toDateTime(DateTimeZone.UTC);
    assertEquals(10, withZone.getHourOfDay());
    assertEquals(DateTimeZone.UTC, withZone.getZone());
  }

  @Test
  void compareTo_ordersByFieldValues() {
    LocalDateTime earlier = new LocalDateTime(2020, 1, 1, 0, 0, 0, 0);
    LocalDateTime later = new LocalDateTime(2020, 1, 1, 0, 0, 1, 0);
    assertTrue(earlier.compareTo(later) < 0);
    assertEquals(0, earlier.compareTo(earlier));
  }

  // -- constructors ---------------------------------------------------------
  @Test
  void noArgConstructors_areCloseToNow() {
    LocalDateTime now = new LocalDateTime();
    LocalDateTime now2 = new LocalDateTime(DateTimeZone.UTC);
    LocalDateTime now3 =
        new LocalDateTime(
            (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstance());
    assertTrue(Math.abs(now.getYear() - now2.getYear()) <= 1);
    assertEquals(now.getChronology(), now3.getChronology());
  }

  @Test
  void millisConstructors() {
    assertEquals(1970, new LocalDateTime(0L).getYear());
    assertEquals(1970, new LocalDateTime(0L, DateTimeZone.UTC).getYear());
    assertEquals(
        1970,
        new LocalDateTime(
                0L, (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC())
            .getYear());
  }

  @Test
  void fiveAndSixArgConstructors() {
    assertEquals(0, new LocalDateTime(2020, 1, 1, 0, 0).getSecondOfMinute());
    assertEquals(30, new LocalDateTime(2020, 1, 1, 0, 0, 30).getSecondOfMinute());
  }

  @Test
  void objectConstructors_parseIsoStrings() {
    LocalDateTime expected = new LocalDateTime(2020, 6, 15, 10, 30, 0, 0);
    assertEquals(expected, new LocalDateTime((Object) "2020-06-15T10:30:00"));
    assertEquals(expected, new LocalDateTime((Object) "2020-06-15T10:30:00", DateTimeZone.UTC));
    assertEquals(
        expected,
        new LocalDateTime(
            (Object) "2020-06-15T10:30:00",
            (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC()));
  }

  @Test
  void fromCalendarFields_and_fromDateFields() {
    Calendar cal = new GregorianCalendar(2020, Calendar.JUNE, 15, 10, 30, 0);
    assertEquals(2020, LocalDateTime.fromCalendarFields(cal).getYear());

    Date date = cal.getTime();
    assertEquals(2020, LocalDateTime.fromDateFields(date).getYear());
  }

  @Test
  void now_withZoneOrChronology() {
    LocalDateTime now = new LocalDateTime();
    assertTrue(Math.abs(LocalDateTime.now(DateTimeZone.UTC).getYear() - now.getYear()) <= 1);
    assertTrue(
        Math.abs(
                LocalDateTime.now(
                            (Chronology)
                                com.legacy.system.datetime.chrono.ISOChronology.getInstance())
                        .getYear()
                    - now.getYear())
            <= 1);
  }

  @Test
  void parse_withAndWithoutFormatter() {
    LocalDateTime a = LocalDateTime.parse("2020-06-15T10:30:00");
    LocalDateTime b =
        LocalDateTime.parse(
            "15/06/2020 10:30",
            com.legacy.system.datetime.format.DateTimeFormat.forPattern("dd/MM/yyyy HH:mm"));
    assertEquals(a, b);
  }

  // -- size / get / isSupported / getValue -----------------------------------------
  @Test
  void size_and_get_and_isSupported() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    // Internally LocalDateTime is stored as {year, monthOfYear, dayOfMonth, millisOfDay}.
    assertEquals(4, dt.size());
    assertEquals(15, dt.get(DateTimeFieldType.dayOfMonth()));
    assertTrue(dt.isSupported(DateTimeFieldType.hourOfDay()));
    assertFalse(dt.isSupported((DateTimeFieldType) null));
    assertTrue(dt.isSupported(DurationFieldType.hours()));
    assertFalse(dt.isSupported((DurationFieldType) null));
    assertEquals(2020, dt.getValue(0));
  }

  @Test
  void equals_selfAndDifferentType() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(dt, dt);
    assertFalse(dt.equals("not a datetime"));
    assertEquals(dt.hashCode(), new LocalDateTime(2020, 6, 15, 10, 30).hashCode());
  }

  // -- toDateTime / toDate ---------------------------------------------------------
  @Test
  void toDateTime_noArgAndZoneOverload() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(10, dt.toDateTime().getHourOfDay());
    DateTime withZone = dt.toDateTime(DateTimeZone.UTC);
    assertEquals(10, withZone.getHourOfDay());
    assertEquals(DateTimeZone.UTC, withZone.getZone());
  }

  @Test
  void toDate_noArgAndTimeZoneOverload() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    Date date = dt.toDate();
    assertNotEquals(null, date);
    Date dateUtc = dt.toDate(TimeZone.getTimeZone("UTC"));
    assertNotEquals(null, dateUtc);
  }

  // -- withDate / withTime / withFields / withField / withFieldAdded -------------
  @Test
  void withDate_and_withTime() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 30, 500);
    LocalDateTime redated = dt.withDate(1999, 3, 3);
    assertEquals(1999, redated.getYear());
    assertEquals(10, redated.getHourOfDay());
    LocalDateTime retimed = dt.withTime(1, 2, 3, 4);
    assertEquals(1, retimed.getHourOfDay());
    assertEquals(2020, retimed.getYear());
  }

  @Test
  void withFields_and_withField_and_withFieldAdded() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    LocalDateTime result = dt.withFields(new LocalDate(1999, 1, 2));
    assertEquals(1999, result.getYear());
    assertEquals(10, result.getHourOfDay());

    LocalDateTime fieldResult = dt.withField(DateTimeFieldType.dayOfMonth(), 20);
    assertEquals(20, fieldResult.getDayOfMonth());

    LocalDateTime addedResult = dt.withFieldAdded(DurationFieldType.days(), 2);
    assertEquals(17, addedResult.getDayOfMonth());
  }

  @Test
  void withDurationAdded_and_withPeriodAdded() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    LocalDateTime result = dt.withDurationAdded(Duration.standardHours(1), 1);
    assertEquals(11, result.getHourOfDay());
    LocalDateTime result2 = dt.withPeriodAdded(Period.days(1), 1);
    assertEquals(16, result2.getDayOfMonth());
  }

  @Test
  void plus_and_minus_durationAndPeriod() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(11, dt.plus(Duration.standardHours(1)).getHourOfDay());
    assertEquals(9, dt.minus(Duration.standardHours(1)).getHourOfDay());
    assertEquals(16, dt.plus(Period.days(1)).getDayOfMonth());
    assertEquals(14, dt.minus(Period.days(1)).getDayOfMonth());
  }

  // -- plus/minus every field -------------------------------------------------
  @Test
  void plusAndMinus_everyField() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 30, 500);
    assertEquals(2021, dt.plusYears(1).getYear());
    assertEquals(7, dt.plusMonths(1).getMonthOfYear());
    assertEquals(22, dt.plusWeeks(1).getDayOfMonth());
    assertEquals(16, dt.plusDays(1).getDayOfMonth());
    assertEquals(11, dt.plusHours(1).getHourOfDay());
    assertEquals(31, dt.plusMinutes(1).getMinuteOfHour());
    assertEquals(31, dt.plusSeconds(1).getSecondOfMinute());
    assertEquals(501, dt.plusMillis(1).getMillisOfSecond());

    assertEquals(2019, dt.minusYears(1).getYear());
    assertEquals(5, dt.minusMonths(1).getMonthOfYear());
    assertEquals(8, dt.minusWeeks(1).getDayOfMonth());
    assertEquals(14, dt.minusDays(1).getDayOfMonth());
    assertEquals(9, dt.minusHours(1).getHourOfDay());
    assertEquals(29, dt.minusMinutes(1).getMinuteOfHour());
    assertEquals(29, dt.minusSeconds(1).getSecondOfMinute());
    assertEquals(499, dt.minusMillis(1).getMillisOfSecond());
  }

  // -- getters and withers for every field -------------------------------------
  @Test
  void gettersAndWithers_everyField() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 30, 500);
    assertEquals(1, dt.getEra());
    assertEquals(20, dt.getCenturyOfEra());
    assertEquals(2020, dt.getYearOfEra());
    assertEquals(20, dt.getYearOfCentury());
    assertEquals(2020, dt.getYear());
    assertEquals(2020, dt.getWeekyear());
    assertEquals(6, dt.getMonthOfYear());
    assertEquals(25, dt.getWeekOfWeekyear());
    assertEquals(167, dt.getDayOfYear());
    assertEquals(15, dt.getDayOfMonth());
    assertEquals(1, dt.getDayOfWeek());
    assertEquals(10, dt.getHourOfDay());
    assertEquals(30, dt.getMinuteOfHour());
    assertEquals(30, dt.getSecondOfMinute());
    assertEquals(500, dt.getMillisOfSecond());
    assertTrue(dt.getMillisOfDay() > 0);

    assertEquals(1, dt.withEra(1).getEra());
    assertEquals(19, dt.withCenturyOfEra(19).getCenturyOfEra());
    assertEquals(2021, dt.withYearOfEra(2021).getYearOfEra());
    assertEquals(21, dt.withYearOfCentury(21).getYearOfCentury());
    assertEquals(1999, dt.withYear(1999).getYear());
    assertEquals(2019, dt.withWeekyear(2019).getWeekyear());
    assertEquals(3, dt.withMonthOfYear(3).getMonthOfYear());
    assertEquals(2, dt.withWeekOfWeekyear(2).getWeekOfWeekyear());
    assertEquals(10, dt.withDayOfYear(10).getDayOfYear());
    assertEquals(10, dt.withDayOfMonth(10).getDayOfMonth());
    assertEquals(
        DateTimeConstants.MONDAY, dt.withDayOfWeek(DateTimeConstants.MONDAY).getDayOfWeek());
    assertEquals(5, dt.withHourOfDay(5).getHourOfDay());
    assertEquals(5, dt.withMinuteOfHour(5).getMinuteOfHour());
    assertEquals(5, dt.withSecondOfMinute(5).getSecondOfMinute());
    assertEquals(5, dt.withMillisOfSecond(5).getMillisOfSecond());
    assertEquals(5, dt.withMillisOfDay(5).getMillisOfDay());
  }

  // -- toString(pattern) --------------------------------------------------------
  @Test
  void toString_withPattern() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals("15/06/2020 10:30", dt.toString("dd/MM/yyyy HH:mm"));
    assertEquals("15/06/2020 10:30", dt.toString("dd/MM/yyyy HH:mm", Locale.ENGLISH));
  }

  // -- property accessors ----------------------------------------------------------
  @Test
  void propertyAccessors_returnPropertiesForEveryField() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 45, 123);
    assertEquals(2020, dt.year().get());
    assertEquals(20, dt.yearOfCentury().get());
    assertEquals(2020, dt.yearOfEra().get());
    assertEquals(2020, dt.weekyear().get());
    assertEquals(6, dt.monthOfYear().get());
    assertEquals(25, dt.weekOfWeekyear().get());
    assertEquals(15, dt.dayOfMonth().get());
    assertEquals(1, dt.dayOfWeek().get());
    assertEquals(167, dt.dayOfYear().get());
    assertEquals(1, dt.era().get());
    assertEquals(20, dt.centuryOfEra().get());
    assertEquals(10, dt.hourOfDay().get());
    assertEquals(30, dt.minuteOfHour().get());
    assertEquals(45, dt.secondOfMinute().get());
    assertEquals(123, dt.millisOfSecond().get());
  }

  @Test
  void property_byFieldType_matchesDirectAccessor() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(dt.dayOfMonth().get(), dt.property(DateTimeFieldType.dayOfMonth()).get());
  }

  // -- LocalDateTime.Property -----------------------------------------------------------
  @Test
  void property_addToCopy_and_setCopy_leaveOriginalUnchanged() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 45, 123);
    LocalDateTime.Property secondProperty = dt.secondOfMinute();

    assertEquals(dt, secondProperty.getLocalDateTime());
    assertEquals(dt.getChronology(), secondProperty.getChronology());
    assertTrue(secondProperty.getMillis() >= 0);

    assertEquals(50, secondProperty.addToCopy(5).getSecondOfMinute());
    assertEquals(50, secondProperty.addToCopy(5L).getSecondOfMinute());
    assertEquals(5, secondProperty.addWrapFieldToCopy(20).getSecondOfMinute());
    assertEquals(5, secondProperty.setCopy(5).getSecondOfMinute());
    assertEquals(45, dt.getSecondOfMinute());
  }

  @Test
  void property_setCopyByText() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(12, dt.monthOfYear().setCopy("December").getMonthOfYear());
    assertEquals(12, dt.monthOfYear().setCopy("December", Locale.ENGLISH).getMonthOfYear());
  }

  @Test
  void property_roundCopyVariants() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 45, 500);
    LocalDateTime.Property secondProperty = dt.secondOfMinute();
    assertEquals(45, secondProperty.roundFloorCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundCeilingCopy().getSecondOfMinute());
    assertEquals(45, secondProperty.roundHalfFloorCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundHalfCeilingCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundHalfEvenCopy().getSecondOfMinute());
  }

  @Test
  void property_withMinimumAndMaximumValue() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(1, dt.dayOfMonth().withMinimumValue().getDayOfMonth());
    assertEquals(30, dt.dayOfMonth().withMaximumValue().getDayOfMonth());
  }

  @Test
  void property_serializationRoundTrip_preservesValue() throws Exception {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    LocalDateTime.Property original = dt.dayOfMonth();

    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    LocalDateTime.Property roundTripped;
    try (java.io.ObjectInputStream in =
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
      roundTripped = (LocalDateTime.Property) in.readObject();
    }
    assertEquals(original.get(), roundTripped.get());
    assertEquals(original.getLocalDateTime(), roundTripped.getLocalDateTime());
  }

  // -- zero-amount / null-argument fast paths --------------------------------------
  @Test
  void zeroAndNull_fastPaths_returnSameInstance() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertSame(dt, dt.plusYears(0));
    assertSame(dt, dt.plusMonths(0));
    assertSame(dt, dt.plusWeeks(0));
    assertSame(dt, dt.plusDays(0));
    assertSame(dt, dt.plusHours(0));
    assertSame(dt, dt.plusMinutes(0));
    assertSame(dt, dt.plusSeconds(0));
    assertSame(dt, dt.plusMillis(0));
    assertSame(dt, dt.minusYears(0));
    assertSame(dt, dt.minusMonths(0));
    assertSame(dt, dt.minusWeeks(0));
    assertSame(dt, dt.minusDays(0));
    assertSame(dt, dt.minusHours(0));
    assertSame(dt, dt.minusMinutes(0));
    assertSame(dt, dt.minusSeconds(0));
    assertSame(dt, dt.minusMillis(0));
    assertSame(dt, dt.withFieldAdded(DurationFieldType.days(), 0));
    assertSame(dt, dt.withFields(null));
    assertSame(dt, dt.withPeriodAdded(null, 1));
    assertSame(dt, dt.withDurationAdded((ReadableDuration) null, 1));
  }

  @Test
  void withFieldAdded_nullField_throws() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertThrows(IllegalArgumentException.class, () -> dt.withFieldAdded(null, 1));
  }

  @Test
  void property_nullOrUnsupportedField_throws() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertThrows(IllegalArgumentException.class, () -> dt.property(null));
  }

  // -- now() / compareTo / toString(pattern) / fromDateFields ------------------------
  @Test
  void now_noArg_isCloseToCurrentTime() {
    LocalDateTime nowViaDefaultZone = new LocalDateTime();
    assertTrue(Math.abs(LocalDateTime.now().getYear() - nowViaDefaultZone.getYear()) <= 1);
  }

  @Test
  void compareTo_differentChronology_fallsBackToGenericComparison() {
    LocalDateTime iso = new LocalDateTime(2020, 6, 15, 10, 30);
    LocalDateTime gregorian =
        new LocalDateTime(
            2020,
            6,
            15,
            10,
            30,
            0,
            0,
            (Chronology) com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC());
    assertEquals(0, iso.compareTo(gregorian));
  }

  @Test
  void toString_nullPattern_fallsBackToDefaultToString() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    assertEquals(dt.toString(), dt.toString((String) null));
  }

  @Test
  void fromDateFields_roundTripsThroughToDate() {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30, 0, 0);
    assertEquals(dt, LocalDateTime.fromDateFields(dt.toDate()));
  }

  @Test
  void serializationRoundTrip_resolvesToEqualValue() throws Exception {
    LocalDateTime dt = new LocalDateTime(2020, 6, 15, 10, 30);
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
      out.writeObject(dt);
    }
    LocalDateTime roundTripped;
    try (java.io.ObjectInputStream in =
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
      roundTripped = (LocalDateTime) in.readObject();
    }
    assertEquals(dt, roundTripped);
  }
}
