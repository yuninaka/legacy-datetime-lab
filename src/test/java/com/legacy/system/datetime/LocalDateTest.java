package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.ISOChronology;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class LocalDateTest {

  @Test
  void constructor_usesIsoChronologyUtc_regardlessOfSystemZone() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(ISOChronology.getInstanceUTC(), date.getChronology());
  }

  @Test
  void constructor_invalidDay_throws() {
    assertThrows(IllegalFieldValueException.class, () -> new LocalDate(2021, 2, 29));
  }

  @Test
  void constructor_leapDay_isValidInLeapYear() {
    LocalDate date = new LocalDate(2020, 2, 29);
    assertEquals(29, date.getDayOfMonth());
  }

  @Test
  void equals_requiresSameFieldsAndChronology() {
    LocalDate a = new LocalDate(2020, 6, 15);
    LocalDate b = new LocalDate(2020, 6, 15);
    LocalDate c = new LocalDate(2020, 6, 16);
    assertEquals(a, b);
    assertNotEquals(a, c);
  }

  @Test
  void compareTo_ordersLargestFieldFirst() {
    LocalDate earlier = new LocalDate(2020, 1, 1);
    LocalDate later = new LocalDate(2020, 1, 2);
    assertTrue(earlier.compareTo(later) < 0);
    assertTrue(later.compareTo(earlier) > 0);
    assertEquals(0, earlier.compareTo(new LocalDate(2020, 1, 1)));
  }

  @Test
  void compareTo_mismatchedPartialFields_throwsClassCastException() {
    LocalDate date = new LocalDate(2020, 1, 1);
    YearMonth yearMonth = new YearMonth(2020, 1);
    assertThrows(ClassCastException.class, () -> date.compareTo(yearMonth));
  }

  @Test
  void plusMonths_clampsDayOfMonth() {
    LocalDate jan31 = new LocalDate(2013, 1, 31);
    LocalDate result = jan31.plusMonths(1);
    assertEquals(2013, result.getYear());
    assertEquals(2, result.getMonthOfYear());
    assertEquals(28, result.getDayOfMonth());
  }

  @Test
  void plusYears_leapDayOntoNonLeapYear_clampsToFeb28() {
    LocalDate leapDay = new LocalDate(2000, 2, 29);
    LocalDate result = leapDay.plusYears(1);
    assertEquals(2001, result.getYear());
    assertEquals(2, result.getMonthOfYear());
    assertEquals(28, result.getDayOfMonth());
  }

  @Test
  void plusDays_rollsOverMonthAndYearBoundaries() {
    LocalDate dec31 = new LocalDate(2020, 12, 31);
    LocalDate result = dec31.plusDays(1);
    assertEquals(2021, result.getYear());
    assertEquals(1, result.getMonthOfYear());
    assertEquals(1, result.getDayOfMonth());
  }

  @Test
  void getDayOfWeek_epochIsThursday() {
    LocalDate epoch = new LocalDate(1970, 1, 1);
    assertEquals(DateTimeConstants.THURSDAY, epoch.getDayOfWeek());
  }

  @Test
  void toString_isIsoDateFormat() {
    LocalDate date = new LocalDate(2020, 6, 5);
    assertEquals("2020-06-05", date.toString());
  }

  @Test
  void toDateTimeAtStartOfDay_combinesWithZone() {
    LocalDate date = new LocalDate(2020, 6, 15);
    DateTime dt = date.toDateTimeAtStartOfDay(DateTimeZone.UTC);
    assertEquals(0, dt.getMillisOfDay());
    assertEquals(2020, dt.getYear());
    assertEquals(6, dt.getMonthOfYear());
    assertEquals(15, dt.getDayOfMonth());
  }

  @Test
  void isLeapYearField_reflectsGregorianRule() {
    assertTrue(new LocalDate(2000, 1, 1).year().isLeap());
    assertFalse(new LocalDate(1900, 1, 1).year().isLeap());
  }

  @Test
  void withField_outOfRange_throws() {
    LocalDate feb1 = new LocalDate(2021, 2, 1);
    assertThrows(IllegalFieldValueException.class, () -> feb1.withDayOfMonth(29));
  }

  // -- constructors ---------------------------------------------------------
  @Test
  void noArgConstructors_areCloseToToday() {
    LocalDate today = new LocalDate();
    LocalDate today2 = new LocalDate(DateTimeZone.UTC);
    LocalDate today3 = new LocalDate((Chronology) ISOChronology.getInstance());
    assertTrue(Math.abs(today.getYear() - today2.getYear()) <= 1);
    assertEquals(today.getChronology(), today3.getChronology());
  }

  @Test
  void millisConstructors() {
    assertEquals(1970, new LocalDate(0L).getYear());
    assertEquals(1970, new LocalDate(0L, DateTimeZone.UTC).getYear());
    assertEquals(1970, new LocalDate(0L, (Chronology) ISOChronology.getInstanceUTC()).getYear());
  }

  @Test
  void objectConstructors_parseIsoStrings() {
    assertEquals(new LocalDate(2020, 6, 15), new LocalDate((Object) "2020-06-15"));
    assertEquals(
        new LocalDate(2020, 6, 15), new LocalDate((Object) "2020-06-15", DateTimeZone.UTC));
    assertEquals(
        new LocalDate(2020, 6, 15),
        new LocalDate((Object) "2020-06-15", (Chronology) ISOChronology.getInstanceUTC()));
  }

  @Test
  void fromDateFields_and_fromCalendarFields() {
    Date date = new GregorianCalendar(2020, Calendar.JUNE, 15).getTime();
    assertEquals(new LocalDate(2020, 6, 15), LocalDate.fromDateFields(date));

    Calendar cal = new GregorianCalendar(2020, Calendar.JUNE, 15);
    assertEquals(new LocalDate(2020, 6, 15), LocalDate.fromCalendarFields(cal));
  }

  @Test
  void now_withZoneOrChronology() {
    LocalDate today = new LocalDate();
    assertTrue(Math.abs(LocalDate.now(DateTimeZone.UTC).getYear() - today.getYear()) <= 1);
    assertTrue(
        Math.abs(
                LocalDate.now((Chronology) ISOChronology.getInstance()).getYear() - today.getYear())
            <= 1);
  }

  @Test
  void parse_withAndWithoutFormatter() {
    LocalDate a = LocalDate.parse("2020-06-15");
    LocalDate b =
        LocalDate.parse(
            "15/06/2020",
            com.legacy.system.datetime.format.DateTimeFormat.forPattern("dd/MM/yyyy"));
    assertEquals(a, b);
  }

  // -- get/isSupported/getValue/getField -----------------------------------------
  @Test
  void get_byFieldType() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(15, date.get(DateTimeFieldType.dayOfMonth()));
    assertEquals(2020, date.get(DateTimeFieldType.year()));
  }

  @Test
  void isSupported_dateFieldsTrue_timeFieldsFalse() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertTrue(date.isSupported(DateTimeFieldType.dayOfMonth()));
    assertFalse(date.isSupported(DateTimeFieldType.hourOfDay()));
    assertTrue(date.isSupported(DurationFieldType.days()));
    assertFalse(date.isSupported(DurationFieldType.hours()));
  }

  @Test
  void getValue_and_getField_byIndex() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2020, date.getValue(0));
    assertEquals(DateTimeFieldType.year(), date.getField(0, date.getChronology()).getType());
  }

  // -- equals/hashCode/compareTo full branches -----------------------------------
  @Test
  void equals_and_hashCode_selfAndDifferentType() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(date, date);
    assertFalse(date.equals("not a date"));
    assertEquals(date.hashCode(), new LocalDate(2020, 6, 15).hashCode());
  }

  @Test
  void compareTo_selfIsZero() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(0, date.compareTo(date));
  }

  // -- plus/minus remaining fields --------------------------------------------
  @Test
  void plusAndMinus_weeks() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(22, date.plusWeeks(1).getDayOfMonth());
    assertEquals(8, date.minusWeeks(1).getDayOfMonth());
  }

  @Test
  void minus_yearsMonthsDays() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2019, date.minusYears(1).getYear());
    assertEquals(5, date.minusMonths(1).getMonthOfYear());
    assertEquals(14, date.minusDays(1).getDayOfMonth());
  }

  @Test
  void withPeriodAdded_and_plusPeriod_and_minusPeriod() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(new LocalDate(2020, 7, 15), date.withPeriodAdded(Period.months(1), 1));
    assertEquals(new LocalDate(2020, 7, 15), date.plus(Period.months(1)));
    assertEquals(new LocalDate(2020, 5, 15), date.minus(Period.months(1)));
  }

  @Test
  void withFieldAdded_and_withField_and_withFields() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(17, date.withFieldAdded(DurationFieldType.days(), 2).getDayOfMonth());
    assertEquals(20, date.withField(DateTimeFieldType.dayOfMonth(), 20).getDayOfMonth());
    assertEquals(new LocalDate(1999, 1, 2), date.withFields(new LocalDate(1999, 1, 2)));
  }

  @Test
  void withYearOfEraEtAl_setsExpectedFields() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2020, date.withYearOfEra(2020).getYearOfEra());
    assertEquals(20, date.withYearOfCentury(20).getYearOfCentury());
    assertEquals(19, date.withCenturyOfEra(19).getCenturyOfEra());
    assertEquals(1, date.withEra(1).getEra());
    assertEquals(10, date.withDayOfYear(10).getDayOfYear());
    assertEquals(
        DateTimeConstants.MONDAY, date.withDayOfWeek(DateTimeConstants.MONDAY).getDayOfWeek());
    assertEquals(2019, date.withWeekyear(2019).getWeekyear());
    assertEquals(2, date.withWeekOfWeekyear(2).getWeekOfWeekyear());
  }

  // -- to* conversions ------------------------------------------------------------
  @Test
  void toDate_roundTripsThroughJavaUtilDate() {
    LocalDate date = new LocalDate(2020, 6, 15);
    Date javaDate = date.toDate();
    assertEquals(date, LocalDate.fromDateFields(javaDate));
  }

  @Test
  void toDateTimeAtStartOfDay_noArg() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(0, date.toDateTimeAtStartOfDay().getMillisOfDay());
  }

  @Test
  @SuppressWarnings("deprecation")
  void toDateTimeAtMidnight_deprecatedOverloads() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(0, date.toDateTimeAtMidnight().getMillisOfDay());
    assertEquals(0, date.toDateTimeAtMidnight(DateTimeZone.UTC).getMillisOfDay());
  }

  @Test
  void toDateTimeAtCurrentTime_overloads() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2020, date.toDateTimeAtCurrentTime().getYear());
    assertEquals(2020, date.toDateTimeAtCurrentTime(DateTimeZone.UTC).getYear());
  }

  @Test
  @SuppressWarnings("deprecation")
  void toDateMidnight_overloads() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2020, date.toDateMidnight().getYear());
    assertEquals(2020, date.toDateMidnight(DateTimeZone.UTC).getYear());
  }

  @Test
  void toLocalDateTime_withTime() {
    LocalDate date = new LocalDate(2020, 6, 15);
    LocalDateTime dt = date.toLocalDateTime(new LocalTime(10, 30));
    assertEquals(10, dt.getHourOfDay());
    assertEquals(15, dt.getDayOfMonth());
  }

  @Test
  void toDateTime_withTime_overloads() {
    LocalDate date = new LocalDate(2020, 6, 15);
    DateTime dt1 = date.toDateTime(new LocalTime(10, 30));
    assertEquals(10, dt1.getHourOfDay());
    DateTime dt2 = date.toDateTime(new LocalTime(10, 30), DateTimeZone.UTC);
    assertEquals(10, dt2.getHourOfDay());
    assertEquals(DateTimeZone.UTC, dt2.getZone());
  }

  @Test
  void toInterval_overloads() {
    LocalDate date = new LocalDate(2020, 6, 15);
    Interval interval = date.toInterval();
    assertEquals(1, interval.toDuration().toStandardDays().getDays());
    Interval intervalUtc = date.toInterval(DateTimeZone.UTC);
    assertEquals(DateTimeZone.UTC, intervalUtc.getChronology().getZone());
  }

  @Test
  void toString_withPattern() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals("15/06/2020", date.toString("dd/MM/yyyy"));
    assertEquals("15/06/2020", date.toString("dd/MM/yyyy", Locale.ENGLISH));
  }

  // -- property accessors ----------------------------------------------------------
  @Test
  void propertyAccessors_returnPropertiesForEveryField() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(2020, date.year().get());
    assertEquals(20, date.yearOfCentury().get());
    assertEquals(2020, date.yearOfEra().get());
    assertEquals(2020, date.weekyear().get());
    assertEquals(6, date.monthOfYear().get());
    assertEquals(25, date.weekOfWeekyear().get());
    assertEquals(15, date.dayOfMonth().get());
    assertEquals(1, date.dayOfWeek().get());
    assertEquals(167, date.dayOfYear().get());
    assertEquals(1, date.era().get());
    assertEquals(20, date.centuryOfEra().get());
  }

  @Test
  void property_byFieldType_matchesDirectAccessor() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(date.dayOfMonth().get(), date.property(DateTimeFieldType.dayOfMonth()).get());
  }

  // -- LocalDate.Property -----------------------------------------------------------
  @Test
  void property_addToCopy_and_setCopy_leaveOriginalUnchanged() {
    LocalDate date = new LocalDate(2020, 6, 15);
    LocalDate.Property dayProperty = date.dayOfMonth();

    assertEquals(date, dayProperty.getLocalDate());
    assertEquals(date.getChronology(), dayProperty.getChronology());

    assertEquals(20, dayProperty.addToCopy(5).getDayOfMonth());
    assertEquals(5, dayProperty.addWrapFieldToCopy(-10).getDayOfMonth());
    assertEquals(20, dayProperty.setCopy(20).getDayOfMonth());
    assertEquals(15, date.getDayOfMonth());
  }

  @Test
  void property_setCopyByText() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(12, date.monthOfYear().setCopy("December").getMonthOfYear());
    assertEquals(12, date.monthOfYear().setCopy("December", Locale.ENGLISH).getMonthOfYear());
  }

  @Test
  void property_roundCopyVariants_areNoOpsForWholeDayFields() {
    LocalDate date = new LocalDate(2020, 6, 15);
    LocalDate.Property dayProperty = date.dayOfMonth();
    assertEquals(date, dayProperty.roundFloorCopy());
    assertEquals(date, dayProperty.roundCeilingCopy());
    assertEquals(date, dayProperty.roundHalfFloorCopy());
    assertEquals(date, dayProperty.roundHalfCeilingCopy());
    assertEquals(date, dayProperty.roundHalfEvenCopy());
  }

  @Test
  void property_withMinimumAndMaximumValue() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(1, date.dayOfMonth().withMinimumValue().getDayOfMonth());
    assertEquals(30, date.dayOfMonth().withMaximumValue().getDayOfMonth());
  }

  @Test
  void property_serializationRoundTrip_preservesValue() throws Exception {
    LocalDate date = new LocalDate(2020, 6, 15);
    LocalDate.Property original = date.dayOfMonth();

    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    LocalDate.Property roundTripped;
    try (java.io.ObjectInputStream in =
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
      roundTripped = (LocalDate.Property) in.readObject();
    }
    assertEquals(original.get(), roundTripped.get());
    assertEquals(original.getLocalDate(), roundTripped.getLocalDate());
  }

  // -- direct with* setters not covered by the combined field test above -----------
  @Test
  void withYear_withMonthOfYear_withDayOfMonth_directly() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(1999, date.withYear(1999).getYear());
    assertEquals(3, date.withMonthOfYear(3).getMonthOfYear());
    assertEquals(10, date.withDayOfMonth(10).getDayOfMonth());
  }

  // -- zero-amount / null-argument fast paths --------------------------------------
  @Test
  void zeroAndNull_fastPaths_returnSameInstance() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertSame(date, date.plusYears(0));
    assertSame(date, date.plusMonths(0));
    assertSame(date, date.plusWeeks(0));
    assertSame(date, date.plusDays(0));
    assertSame(date, date.minusYears(0));
    assertSame(date, date.minusMonths(0));
    assertSame(date, date.minusWeeks(0));
    assertSame(date, date.minusDays(0));
    assertSame(date, date.withPeriodAdded(null, 1));
    assertSame(date, date.withPeriodAdded(Period.days(1), 0));
    assertSame(date, date.withFields(null));
  }

  // -- isSupported / get / withField / withFieldAdded: unsupported field branches --
  @Test
  void isSupported_and_get_nullAndUnsupportedFields() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertFalse(date.isSupported((DateTimeFieldType) null));
    assertFalse(date.isSupported((DurationFieldType) null));
    assertThrows(IllegalArgumentException.class, () -> date.get(null));
    assertThrows(IllegalArgumentException.class, () -> date.get(DateTimeFieldType.hourOfDay()));
    assertThrows(IllegalArgumentException.class, () -> date.withField(null, 1));
    assertThrows(
        IllegalArgumentException.class, () -> date.withField(DateTimeFieldType.hourOfDay(), 1));
    assertThrows(IllegalArgumentException.class, () -> date.withFieldAdded(null, 1));
    assertThrows(
        IllegalArgumentException.class, () -> date.withFieldAdded(DurationFieldType.hours(), 1));
  }

  // -- now() / toDate / fromDateFields / toDateTime(LocalTime, zone) ----------------
  @Test
  void now_noArg_isCloseToToday() {
    assertEquals(new LocalDate(), LocalDate.now());
  }

  @Test
  void toDate_and_fromDateFields_roundTrip_multipleDates() {
    for (int month = 1; month <= 12; month++) {
      LocalDate date = new LocalDate(2020, month, 10);
      assertEquals(date, LocalDate.fromDateFields(date.toDate()));
    }
  }

  @Test
  void toDateTime_withTimeAndZone() {
    LocalDate date = new LocalDate(2020, 6, 15);
    DateTime dt = date.toDateTime(new LocalTime(10, 30), DateTimeZone.UTC);
    assertEquals(10, dt.getHourOfDay());
    assertEquals(15, dt.getDayOfMonth());
  }

  @Test
  void toLocalDateTime_nullTime_throws() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertThrows(IllegalArgumentException.class, () -> date.toLocalDateTime(null));
  }

  @Test
  void toString_nullPattern_fallsBackToDefaultToString() {
    LocalDate date = new LocalDate(2020, 6, 15);
    assertEquals(date.toString(), date.toString((String) null));
  }
}
