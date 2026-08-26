package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.GregorianChronology;
import com.legacy.system.datetime.chrono.ISOChronology;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class DateTimeTest {

  @Test
  void constructor_fieldValues_defaultToIsoChronologyDefaultZone() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
    assertEquals(2020, dt.getYear());
    assertEquals(6, dt.getMonthOfYear());
    assertEquals(15, dt.getDayOfMonth());
    assertEquals(10, dt.getHourOfDay());
    assertEquals(30, dt.getMinuteOfHour());
    assertEquals(45, dt.getSecondOfMinute());
    assertEquals(123, dt.getMillisOfSecond());
    assertEquals(ISOChronology.getInstance(DateTimeZone.UTC), dt.getChronology());
  }

  @Test
  void constructor_shortForm_defaultsTrailingFieldsToZero() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(0, dt.getSecondOfMinute());
    assertEquals(0, dt.getMillisOfSecond());
  }

  @Test
  void constructor_invalidMonth_throws() {
    assertThrows(
        IllegalFieldValueException.class, () -> new DateTime(2020, 13, 1, 0, 0, DateTimeZone.UTC));
  }

  @Test
  void constructor_invalidDayOfMonth_throws() {
    assertThrows(
        IllegalFieldValueException.class, () -> new DateTime(2021, 2, 29, 0, 0, DateTimeZone.UTC));
  }

  @Test
  void constructor_millis_epoch() {
    DateTime dt = new DateTime(0L, DateTimeZone.UTC);
    assertEquals(1970, dt.getYear());
    assertEquals(1, dt.getMonthOfYear());
    assertEquals(1, dt.getDayOfMonth());
    assertEquals(DateTimeConstants.THURSDAY, dt.getDayOfWeek());
  }

  // -- equals vs isEqual: chronology/zone sensitivity ----------------------
  @Test
  void equals_requiresSameChronologyAndZone() {
    DateTime utc = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime tokyo = utc.withZone(DateTimeZone.forID("Asia/Tokyo"));
    assertEquals(utc.getMillis(), tokyo.getMillis());
    assertNotEquals(utc, tokyo);
  }

  @Test
  void isEqual_ignoresChronologyAndZone() {
    DateTime utc = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime tokyo = utc.withZone(DateTimeZone.forID("Asia/Tokyo"));
    assertTrue(utc.isEqual(tokyo));
  }

  @Test
  void compareTo_comparesMillisOnly() {
    DateTime earlier = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime later = new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC);
    assertTrue(earlier.compareTo(later) < 0);
    assertTrue(later.compareTo(earlier) > 0);
    assertEquals(0, earlier.compareTo(earlier.withZone(DateTimeZone.forID("Asia/Tokyo"))));
  }

  @Test
  void hashCode_consistentWithEquals() {
    DateTime a = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime b = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  // -- withZone vs withZoneRetainFields -------------------------------------
  @Test
  void withZone_keepsInstantFixed_changesLocalFields() {
    DateTime utc = new DateTime(2020, 6, 15, 12, 0, DateTimeZone.UTC);
    DateTime tokyo = utc.withZone(DateTimeZone.forID("Asia/Tokyo"));
    assertEquals(utc.getMillis(), tokyo.getMillis());
    assertEquals(21, tokyo.getHourOfDay());
  }

  @Test
  void withZoneRetainFields_keepsLocalFields_changesInstant() {
    DateTime utc = new DateTime(2020, 6, 15, 12, 0, DateTimeZone.UTC);
    DateTime tokyo = utc.withZoneRetainFields(DateTimeZone.forID("Asia/Tokyo"));
    assertEquals(12, tokyo.getHourOfDay());
    assertNotEquals(utc.getMillis(), tokyo.getMillis());
  }

  // -- plus/minus field arithmetic ------------------------------------------
  @Test
  void plusMonths_clampsDayOfMonth() {
    DateTime jan31 = new DateTime(2013, 1, 31, 0, 0, DateTimeZone.UTC);
    DateTime result = jan31.plusMonths(1);
    assertEquals(2013, result.getYear());
    assertEquals(2, result.getMonthOfYear());
    assertEquals(28, result.getDayOfMonth());
  }

  @Test
  void minusMonths_clampsToLeapFebruary() {
    DateTime mar31 = new DateTime(2000, 3, 31, 0, 0, DateTimeZone.UTC);
    DateTime result = mar31.minusMonths(1);
    assertEquals(29, result.getDayOfMonth());
  }

  @Test
  void plusZero_returnsEqualInstant() {
    DateTime dt = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    assertSame(dt, dt.plusDays(0));
  }

  @Test
  void withDayOfMonth_outOfRange_throws() {
    DateTime jan31 = new DateTime(2021, 1, 31, 0, 0, DateTimeZone.UTC);
    assertThrows(IllegalFieldValueException.class, () -> jan31.withDayOfMonth(32));
    DateTime feb1 = new DateTime(2021, 2, 1, 0, 0, DateTimeZone.UTC);
    assertThrows(IllegalFieldValueException.class, () -> feb1.withDayOfMonth(29));
  }

  // -- DST-sensitive Period vs Duration addition ----------------------------
  @Test
  void plusPeriodOfOneDay_acrossDstSpringForward_addsOnly23Hours() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    // 2018-03-11 is the US DST transition (clocks spring forward 02:00 -> 03:00); starting
    // at noon the day before means "same local time next day" lands after the transition.
    DateTime beforeDst = new DateTime(2018, 3, 10, 12, 0, newYork);
    DateTime plusOneDay = beforeDst.plus(Period.days(1));
    assertEquals(11, plusOneDay.getDayOfMonth());
    assertEquals(12, plusOneDay.getHourOfDay());
    assertEquals(0, plusOneDay.getMinuteOfHour());
    assertEquals(23 * 60 * 60 * 1000L, plusOneDay.getMillis() - beforeDst.getMillis());
  }

  @Test
  void plusDurationOfOneDay_acrossDstSpringForward_addsExactly24Hours() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    DateTime beforeDst = new DateTime(2018, 3, 10, 12, 0, newYork);
    DateTime plusOneDay = beforeDst.plus(Duration.standardDays(1));
    assertEquals(24 * 60 * 60 * 1000L, plusOneDay.getMillis() - beforeDst.getMillis());
    // The wall-clock time shifts forward by an hour because the DST gap was skipped.
    assertEquals(13, plusOneDay.getHourOfDay());
    assertEquals(0, plusOneDay.getMinuteOfHour());
  }

  @Test
  void toString_isIso8601WithOffset() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
    assertEquals("2020-06-15T10:30:00.000Z", dt.toString());
  }

  @Test
  void toDateTimeISO_usesIsoChronology() {
    DateTime custom =
        new DateTime(
            2020,
            6,
            15,
            0,
            0,
            com.legacy.system.datetime.chrono.GregorianChronology.getInstance(DateTimeZone.UTC));
    DateTime iso = custom.toDateTimeISO();
    assertEquals(ISOChronology.class, iso.getChronology().getClass());
  }

  @Test
  void isBeforeAfterEqual_comparesMillis() {
    DateTime a = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime b = new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC);
    assertTrue(a.isBefore(b));
    assertTrue(b.isAfter(a));
    assertFalse(a.isAfter(b));
    assertTrue(a.isEqual(a.withZone(DateTimeZone.forID("Asia/Tokyo"))));
  }

  // -- constructors -----------------------------------------------------------
  @Test
  void noArgConstructors_produceCloseToNow() {
    long now = DateTimeUtils.currentTimeMillis();
    assertTrue(Math.abs(new DateTime().getMillis() - now) < 60_000L);
    assertTrue(Math.abs(new DateTime(DateTimeZone.UTC).getMillis() - now) < 60_000L);
    assertTrue(
        Math.abs(new DateTime((Chronology) ISOChronology.getInstanceUTC()).getMillis() - now)
            < 60_000L);
  }

  @Test
  void millisConstructors() {
    assertEquals(0L, new DateTime(0L).getMillis());
    assertEquals(DateTimeZone.UTC, new DateTime(0L, DateTimeZone.UTC).getZone());
    assertEquals(
        ISOChronology.getInstanceUTC(),
        new DateTime(0L, (Chronology) ISOChronology.getInstanceUTC()).getChronology());
  }

  @Test
  void objectConstructors_parseIsoStrings() {
    DateTime a = new DateTime((Object) "2020-06-15T10:30:00.000Z");
    DateTime b = new DateTime((Object) "2020-06-15T10:30:00.000Z", DateTimeZone.UTC);
    DateTime c =
        new DateTime(
            (Object) "2020-06-15T10:30:00.000Z", (Chronology) ISOChronology.getInstanceUTC());
    assertEquals(a.getMillis(), b.getMillis());
    assertEquals(a.getMillis(), c.getMillis());
  }

  @Test
  void fieldConstructors_fiveToEightArgs() {
    assertEquals(0, new DateTime(2020, 1, 1, 0, 0).getSecondOfMinute());
    assertEquals(2020, new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC).getYear());
    assertEquals(
        2020,
        new DateTime(2020, 1, 1, 0, 0, (Chronology) ISOChronology.getInstanceUTC()).getYear());
    assertEquals(30, new DateTime(2020, 1, 1, 0, 0, 30).getSecondOfMinute());
    assertEquals(30, new DateTime(2020, 1, 1, 0, 0, 30, DateTimeZone.UTC).getSecondOfMinute());
    assertEquals(
        30,
        new DateTime(2020, 1, 1, 0, 0, 30, (Chronology) ISOChronology.getInstanceUTC())
            .getSecondOfMinute());
    assertEquals(999, new DateTime(2020, 1, 1, 0, 0, 30, 999).getMillisOfSecond());
  }

  // -- now(zone/chronology) ----------------------------------------------------
  @Test
  void now_withZoneOrChronology_isCloseToCurrentTime() {
    long nowMillis = DateTimeUtils.currentTimeMillis();
    assertTrue(Math.abs(DateTime.now(DateTimeZone.UTC).getMillis() - nowMillis) < 60_000L);
    assertTrue(
        Math.abs(DateTime.now((Chronology) ISOChronology.getInstanceUTC()).getMillis() - nowMillis)
            < 60_000L);
  }

  // -- parse --------------------------------------------------------------------
  @Test
  void parse_withAndWithoutFormatter() {
    DateTime a = DateTime.parse("2020-06-15T10:30:00.000Z");
    DateTime b =
        DateTime.parse("2020-06-15", com.legacy.system.datetime.format.ISODateTimeFormat.date());
    assertEquals(15, a.getDayOfMonth());
    assertEquals(15, b.getDayOfMonth());
  }

  // -- toDateTime(zone/chronology) -----------------------------------------------
  @Test
  void toDateTime_zoneAndChronologyOverloads() {
    DateTime utc = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime tokyo = utc.toDateTime(DateTimeZone.forID("Asia/Tokyo"));
    assertEquals(utc.getMillis(), tokyo.getMillis());
    DateTime gregorian = utc.toDateTime((Chronology) GregorianChronology.getInstanceUTC());
    assertEquals(GregorianChronology.getInstanceUTC(), gregorian.getChronology());
  }

  // -- withXxx / plusXxx / minusXxx for every field ------------------------------
  @Test
  void plusAndMinus_everyField() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 30, 500, DateTimeZone.UTC);
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

  @Test
  void withXxx_everyField() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 30, 500, DateTimeZone.UTC);
    assertEquals(1999, dt.withYear(1999).getYear());
    assertEquals(3, dt.withMonthOfYear(3).getMonthOfYear());
    assertEquals(10, dt.withDayOfMonth(10).getDayOfMonth());
    assertEquals(5, dt.withHourOfDay(5).getHourOfDay());
    assertEquals(5, dt.withMinuteOfHour(5).getMinuteOfHour());
    assertEquals(5, dt.withSecondOfMinute(5).getSecondOfMinute());
    assertEquals(5, dt.withMillisOfSecond(5).getMillisOfSecond());
    assertEquals(5, dt.withMillisOfDay(5).getMillisOfDay());
    assertEquals(2019, dt.withWeekyear(2019).getWeekyear());
    assertEquals(2, dt.withWeekOfWeekyear(2).getWeekOfWeekyear());
    assertEquals(
        DateTimeConstants.MONDAY, dt.withDayOfWeek(DateTimeConstants.MONDAY).getDayOfWeek());
    assertEquals(10, dt.withDayOfYear(10).getDayOfYear());
    assertEquals(20, dt.withYearOfCentury(20).getYearOfCentury());
    assertEquals(2020, dt.withYearOfEra(2020).getYearOfEra());
    assertEquals(19, dt.withCenturyOfEra(19).getCenturyOfEra());
    assertEquals(1, dt.withEra(1).getEra());
    DateTime midnight = dt.withTimeAtStartOfDay();
    assertEquals(0, midnight.getMillisOfDay());
  }

  @Test
  void withDate_and_withTime_overloads() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 30, 500, DateTimeZone.UTC);
    DateTime redated = dt.withDate(1999, 3, 3);
    assertEquals(1999, redated.getYear());
    assertEquals(3, redated.getMonthOfYear());
    assertEquals(3, redated.getDayOfMonth());
    assertEquals(10, redated.getHourOfDay());

    DateTime redated2 = dt.withDate(new LocalDate(1999, 3, 3));
    assertEquals(redated.getMillis(), redated2.getMillis());

    DateTime retimed = dt.withTime(1, 2, 3, 4);
    assertEquals(1, retimed.getHourOfDay());
    assertEquals(2, retimed.getMinuteOfHour());
    assertEquals(3, retimed.getSecondOfMinute());
    assertEquals(4, retimed.getMillisOfSecond());
    assertEquals(2020, retimed.getYear());

    DateTime retimed2 = dt.withTime(new LocalTime(1, 2, 3, 4));
    assertEquals(retimed.getMillis(), retimed2.getMillis());
  }

  @Test
  void withFields_copiesSupportedPartialFields() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime result = dt.withFields(new LocalDate(1999, 1, 2));
    assertEquals(1999, result.getYear());
    assertEquals(1, result.getMonthOfYear());
    assertEquals(2, result.getDayOfMonth());
    assertEquals(10, result.getHourOfDay());
  }

  @Test
  void withField_setsSingleFieldByType() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime result = dt.withField(DateTimeFieldType.dayOfMonth(), 20);
    assertEquals(20, result.getDayOfMonth());
  }

  @Test
  void withFieldAdded_addsSingleFieldByType() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime result = dt.withFieldAdded(DurationFieldType.days(), 2);
    assertEquals(17, result.getDayOfMonth());
  }

  @Test
  void withEarlierAndLaterOffsetAtOverlap_onFixedOffsetZone_returnsEquivalentInstant() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(dt.getMillis(), dt.withEarlierOffsetAtOverlap().getMillis());
    assertEquals(dt.getMillis(), dt.withLaterOffsetAtOverlap().getMillis());
  }

  // -- to* conversions ------------------------------------------------------------
  @Test
  void toConversions_produceExpectedTypes() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
    assertEquals(new LocalDate(2020, 6, 15), dt.toLocalDate());
    assertEquals(new LocalDateTime(2020, 6, 15, 10, 30, 45, 123), dt.toLocalDateTime());
    assertEquals(new LocalTime(10, 30, 45, 123), dt.toLocalTime());
    assertEquals(dt.getMillis(), dt.toDateTime().getMillis());
  }

  // -- property accessors ----------------------------------------------------------
  @Test
  void propertyAccessors_returnPropertiesForEveryField() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
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
    assertTrue(dt.millisOfDay().get() > 0);
    assertTrue(dt.secondOfDay().get() > 0);
    assertTrue(dt.minuteOfDay().get() > 0);
  }

  @Test
  void property_byFieldType_matchesDirectAccessor() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(dt.dayOfMonth().get(), dt.property(DateTimeFieldType.dayOfMonth()).get());
  }

  // -- DateTime.Property -----------------------------------------------------------
  @Test
  void property_addAndSetOperations_leaveOriginalUnchanged() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
    DateTime.Property secondProperty = dt.secondOfMinute();

    assertEquals(dt.getMillis(), secondProperty.getMillis());
    assertEquals(dt.getChronology(), secondProperty.getChronology());
    assertEquals(dt, secondProperty.getDateTime());

    assertEquals(50, secondProperty.addToCopy(5).getSecondOfMinute());
    assertEquals(50, secondProperty.addToCopy(5L).getSecondOfMinute());
    assertEquals(5, secondProperty.addWrapFieldToCopy(20).getSecondOfMinute());
    assertEquals(5, secondProperty.setCopy(5).getSecondOfMinute());
    assertEquals(45, dt.getSecondOfMinute());
  }

  @Test
  void property_setCopyByText() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime result = dt.monthOfYear().setCopy("December");
    assertEquals(12, result.getMonthOfYear());
    DateTime result2 = dt.monthOfYear().setCopy("December", java.util.Locale.ENGLISH);
    assertEquals(12, result2.getMonthOfYear());
  }

  @Test
  void property_roundCopyVariants() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, 45, 500, DateTimeZone.UTC);
    DateTime.Property secondProperty = dt.secondOfMinute();
    assertEquals(45, secondProperty.roundFloorCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundCeilingCopy().getSecondOfMinute());
    // Exactly halfway (500ms): roundHalfFloor favors the floor, roundHalfCeiling favors the
    // ceiling.
    assertEquals(45, secondProperty.roundHalfFloorCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundHalfCeilingCopy().getSecondOfMinute());
    assertEquals(46, secondProperty.roundHalfEvenCopy().getSecondOfMinute());
  }

  @Test
  void property_withMinimumAndMaximumValue() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(1, dt.dayOfMonth().withMinimumValue().getDayOfMonth());
    assertEquals(30, dt.dayOfMonth().withMaximumValue().getDayOfMonth());
  }

  @Test
  void property_serializationRoundTrip_preservesValue() throws Exception {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    DateTime.Property original = dt.dayOfMonth();

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    DateTime.Property roundTripped;
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      roundTripped = (DateTime.Property) in.readObject();
    }
    assertEquals(original.get(), roundTripped.get());
    assertEquals(original.getDateTime(), roundTripped.getDateTime());
  }

  // -- zero-amount / same-value fast paths (return `this`) -------------------------
  @Test
  void zeroAmountAndSameValue_fastPaths_returnSameInstance() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertSame(dt, dt.plusYears(0));
    assertSame(dt, dt.plusMonths(0));
    assertSame(dt, dt.plusWeeks(0));
    assertSame(dt, dt.plusHours(0));
    assertSame(dt, dt.plusMinutes(0));
    assertSame(dt, dt.plusSeconds(0));
    assertSame(dt, dt.plusMillis(0));
    assertSame(dt, dt.minusYears(0));
    assertSame(dt, dt.minusMonths(0));
    assertSame(dt, dt.minusWeeks(0));
    assertSame(dt, dt.minusHours(0));
    assertSame(dt, dt.minusMinutes(0));
    assertSame(dt, dt.minusSeconds(0));
    assertSame(dt, dt.minusMillis(0));
    assertSame(dt, dt.withFieldAdded(DurationFieldType.days(), 0));
    assertSame(dt, dt.withDurationAdded(0L, 1));
    assertSame(dt, dt.withDurationAdded((ReadableDuration) null, 1));
    assertSame(dt, dt.withPeriodAdded((ReadablePeriod) null, 1));
    assertSame(dt, dt.withFields((ReadablePartial) null));
    assertSame(dt, dt.toDateTime(dt.getZone()));
    assertSame(dt, dt.toDateTime(dt.getChronology()));
    assertSame(dt, dt.withZoneRetainFields(dt.getZone()));
  }

  @Test
  void toDateTimeISO_alreadyIsoDefaultZone_returnsSameInstance() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, (Chronology) ISOChronology.getInstance());
    assertSame(dt, dt.toDateTimeISO());
  }

  @Test
  void now_nullZoneOrChronology_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> DateTime.now((DateTimeZone) null));
    assertThrows(NullPointerException.class, () -> DateTime.now((Chronology) null));
  }

  // -- remaining never-called overloads -------------------------------------------
  @Test
  void noArgNow_isCloseToCurrentTime() {
    assertTrue(Math.abs(DateTime.now().getMillis() - DateTimeUtils.currentTimeMillis()) < 60_000L);
  }

  @Test
  void plusMillisLong_and_minus_overloads() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(dt.getMillis() + 1000L, dt.plus(1000L).getMillis());
    assertEquals(dt.getMillis() - 1000L, dt.minus(1000L).getMillis());
    assertEquals(dt.getMillis() - 1000L, dt.minus(Duration.millis(1000L)).getMillis());
    assertEquals(14, dt.minus(Period.days(1)).getDayOfMonth());
  }

  @Test
  @SuppressWarnings("deprecation")
  void deprecatedConversions_stillWork() {
    DateTime dt = new DateTime(2020, 6, 15, 10, 30, DateTimeZone.UTC);
    assertEquals(2020, dt.toYearMonthDay().getYear());
    assertEquals(10, dt.toTimeOfDay().getHourOfDay());
    assertEquals(2020, dt.toDateMidnight().getYear());
  }
}
