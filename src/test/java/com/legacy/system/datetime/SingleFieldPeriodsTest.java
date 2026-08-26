package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SingleFieldPeriodsTest {

  @Test
  void days_factory_smallValuesAreCached() {
    assertSame(Days.ONE, Days.days(1));
    assertSame(Days.ZERO, Days.days(0));
    assertSame(Days.TWO, Days.days(2));
    assertSame(Days.THREE, Days.days(3));
    assertSame(Days.FOUR, Days.days(4));
    assertSame(Days.FIVE, Days.days(5));
    assertSame(Days.SIX, Days.days(6));
    assertSame(Days.SEVEN, Days.days(7));
    assertSame(Days.MAX_VALUE, Days.days(Integer.MAX_VALUE));
    assertSame(Days.MIN_VALUE, Days.days(Integer.MIN_VALUE));
    assertEquals(100, Days.days(100).getDays());
  }

  @Test
  void daysBetween_instants_countsWholeDays() {
    DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime end = new DateTime(2020, 1, 5, 12, 0, DateTimeZone.UTC);
    assertEquals(4, Days.daysBetween(start, end).getDays());
  }

  @Test
  void daysBetween_localDates_countsWholeDays() {
    LocalDate start = new LocalDate(2020, 1, 1);
    LocalDate end = new LocalDate(2020, 3, 1);
    assertEquals(60, Days.daysBetween(start, end).getDays());
  }

  @Test
  void daysIn_interval_countsWholeDays() {
    Interval interval =
        new Interval(
            new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2020, 1, 4, 0, 0, DateTimeZone.UTC));
    assertEquals(3, Days.daysIn(interval).getDays());
  }

  @Test
  void daysIn_nullInterval_isZero() {
    assertEquals(Days.ZERO, Days.daysIn(null));
  }

  @Test
  void standardDaysIn_convertsFixedLengthPeriod() {
    Period twoDays = Period.days(2);
    assertEquals(2, Days.standardDaysIn(twoDays).getDays());
  }

  @Test
  void standardDaysIn_nullPeriod_isZero() {
    assertEquals(Days.ZERO, Days.standardDaysIn(null));
  }

  @Test
  void parseDays_parsesIsoPeriodString() {
    assertEquals(Days.days(3), Days.parseDays("P3D"));
  }

  @Test
  void parseDays_nullString_isZero() {
    assertEquals(Days.ZERO, Days.parseDays(null));
  }

  @Test
  void toStandardXxx_convertUsingFixedDayLength() {
    Days sevenDays = Days.days(7);
    assertEquals(Weeks.weeks(1), sevenDays.toStandardWeeks());
    assertEquals(Hours.hours(7 * 24), sevenDays.toStandardHours());
    assertEquals(Minutes.minutes(7 * 24 * 60), sevenDays.toStandardMinutes());
    assertEquals(Seconds.seconds(7 * 24 * 60 * 60), sevenDays.toStandardSeconds());
    assertEquals(DateTimeConstants.MILLIS_PER_DAY * 7L, sevenDays.toStandardDuration().getMillis());
  }

  @Test
  void days_isLessThan_and_isGreaterThan() {
    assertTrue(Days.days(2).isLessThan(Days.days(3)));
    assertFalse(Days.days(3).isLessThan(Days.days(2)));
    assertTrue(Days.days(3).isGreaterThan(Days.days(2)));
    assertFalse(Days.days(2).isGreaterThan(Days.days(3)));
  }

  @Test
  void days_plusMinusDays_objectOverload() {
    assertEquals(Days.days(5), Days.days(2).plus(Days.days(3)));
    assertEquals(Days.days(2), Days.days(5).minus(Days.days(3)));
  }

  @Test
  void days_toString() {
    assertEquals("P3D", Days.days(3).toString());
  }

  @Test
  void days_getPeriodType() {
    assertEquals(PeriodType.days(), Days.days(1).getPeriodType());
  }

  @Test
  void days_serializationResolvesToCachedInstance() throws Exception {
    Object roundTripped = roundTripSerialize(Days.THREE);
    assertSame(Days.THREE, roundTripped);
  }

  // -- Weeks ----------------------------------------------------------------
  @Test
  void weeksBetween_countsWholeWeeks() {
    LocalDate start = new LocalDate(2020, 1, 1);
    LocalDate end = new LocalDate(2020, 1, 22);
    assertEquals(3, Weeks.weeksBetween(start, end).getWeeks());
  }

  @Test
  void weeksBetween_instants_countsWholeWeeks() {
    DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
    DateTime end = new DateTime(2020, 1, 22, 0, 0, DateTimeZone.UTC);
    assertEquals(3, Weeks.weeksBetween(start, end).getWeeks());
  }

  @Test
  void weeksIn_interval_countsWholeWeeks() {
    Interval interval =
        new Interval(
            new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2020, 1, 15, 0, 0, DateTimeZone.UTC));
    assertEquals(2, Weeks.weeksIn(interval).getWeeks());
  }

  @Test
  void weeksIn_nullInterval_isZero() {
    assertEquals(Weeks.ZERO, Weeks.weeksIn(null));
  }

  @Test
  void standardWeeksIn_convertsFixedLengthPeriod() {
    assertEquals(2, Weeks.standardWeeksIn(Period.weeks(2)).getWeeks());
    assertEquals(Weeks.ZERO, Weeks.standardWeeksIn(null));
  }

  @Test
  void parseWeeks_parsesIsoPeriodString() {
    assertEquals(Weeks.weeks(2), Weeks.parseWeeks("P2W"));
    assertEquals(Weeks.ZERO, Weeks.parseWeeks(null));
  }

  @Test
  void weeks_toStandardXxx() {
    Weeks oneWeek = Weeks.weeks(1);
    assertEquals(Days.days(7), oneWeek.toStandardDays());
    assertEquals(Hours.hours(7 * 24), oneWeek.toStandardHours());
    assertEquals(Minutes.minutes(7 * 24 * 60), oneWeek.toStandardMinutes());
    assertEquals(Seconds.seconds(7 * 24 * 60 * 60), oneWeek.toStandardSeconds());
    assertEquals(DateTimeConstants.MILLIS_PER_WEEK, oneWeek.toStandardDuration().getMillis());
  }

  @Test
  void weeks_isLessThan_and_isGreaterThan() {
    assertTrue(Weeks.weeks(1).isLessThan(Weeks.weeks(2)));
    assertFalse(Weeks.weeks(2).isLessThan(Weeks.weeks(1)));
    assertTrue(Weeks.weeks(2).isGreaterThan(Weeks.weeks(1)));
    assertFalse(Weeks.weeks(1).isGreaterThan(Weeks.weeks(2)));
  }

  @Test
  void weeks_plusMinus_bothOverloads() {
    assertEquals(Weeks.weeks(5), Weeks.weeks(2).plus(3));
    assertEquals(Weeks.weeks(5), Weeks.weeks(2).plus(Weeks.weeks(3)));
    assertEquals(Weeks.weeks(2), Weeks.weeks(5).minus(3));
    assertEquals(Weeks.weeks(2), Weeks.weeks(5).minus(Weeks.weeks(3)));
  }

  @Test
  void weeks_negated_toString_getPeriodType() {
    assertEquals(Weeks.weeks(-3), Weeks.weeks(3).negated());
    assertEquals("P3W", Weeks.weeks(3).toString());
    assertEquals(PeriodType.weeks(), Weeks.weeks(1).getPeriodType());
  }

  @Test
  void weeks_serializationResolvesToCachedInstance() throws Exception {
    assertSame(Weeks.THREE, roundTripSerialize(Weeks.THREE));
  }

  // -- Months -----------------------------------------------------------------
  @Test
  void months_factory_largeValueIsNotCached() {
    assertEquals(100, Months.months(100).getMonths());
    assertSame(Months.MAX_VALUE, Months.months(Integer.MAX_VALUE));
    assertSame(Months.MIN_VALUE, Months.months(Integer.MIN_VALUE));
  }

  @Test
  void monthsBetween_countsWholeCalendarMonths() {
    LocalDate start = new LocalDate(2020, 1, 31);
    LocalDate end = new LocalDate(2020, 3, 1);
    // Jan 31 + 1 month clamps to Feb 29 (leap year), which is before Mar 1, so 1 whole month fits.
    assertEquals(1, Months.monthsBetween(start, end).getMonths());
  }

  @Test
  void monthsBetween_instants_countsWholeCalendarMonths() {
    DateTime start = new DateTime(2020, 1, 31, 0, 0, DateTimeZone.UTC);
    DateTime end = new DateTime(2020, 3, 1, 0, 0, DateTimeZone.UTC);
    assertEquals(1, Months.monthsBetween(start, end).getMonths());
  }

  @Test
  void monthsIn_interval_countsWholeCalendarMonths() {
    Interval interval =
        new Interval(
            new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2020, 4, 1, 0, 0, DateTimeZone.UTC));
    assertEquals(3, Months.monthsIn(interval).getMonths());
    assertEquals(Months.ZERO, Months.monthsIn(null));
  }

  @Test
  void parseMonths_parsesIsoPeriodString() {
    assertEquals(Months.months(2), Months.parseMonths("P2M"));
    assertEquals(Months.ZERO, Months.parseMonths(null));
  }

  @Test
  void months_isLessThan_and_isGreaterThan() {
    assertTrue(Months.months(1).isLessThan(Months.months(2)));
    assertFalse(Months.months(2).isLessThan(Months.months(1)));
    assertTrue(Months.months(2).isGreaterThan(Months.months(1)));
    assertFalse(Months.months(1).isGreaterThan(Months.months(2)));
  }

  @Test
  void months_plusMinus_bothOverloads() {
    assertEquals(Months.months(5), Months.months(2).plus(3));
    assertEquals(Months.months(5), Months.months(2).plus(Months.months(3)));
    assertEquals(Months.months(2), Months.months(5).minus(3));
    assertEquals(Months.months(2), Months.months(5).minus(Months.months(3)));
  }

  @Test
  void months_multipliedBy_toString_getPeriodType() {
    assertEquals(Months.months(6), Months.months(2).multipliedBy(3));
    assertEquals("P3M", Months.months(3).toString());
    assertEquals(PeriodType.months(), Months.months(1).getPeriodType());
  }

  @Test
  void months_serializationResolvesToCachedInstance() throws Exception {
    assertSame(Months.THREE, roundTripSerialize(Months.THREE));
  }

  // -- Years --------------------------------------------------------------
  @Test
  void years_factory_largeValueIsNotCached() {
    assertEquals(100, Years.years(100).getYears());
    assertSame(Years.MAX_VALUE, Years.years(Integer.MAX_VALUE));
    assertSame(Years.MIN_VALUE, Years.years(Integer.MIN_VALUE));
  }

  @Test
  void yearsBetween_countsWholeCalendarYears() {
    // Feb 29 clamped forward by 1 year lands on Feb 28 of a non-leap year (see DateTimeTest),
    // so reaching exactly that clamped date already counts as one whole year.
    LocalDate leapDay = new LocalDate(2000, 2, 29);
    assertEquals(0, Years.yearsBetween(leapDay, new LocalDate(2001, 2, 27)).getYears());
    assertEquals(1, Years.yearsBetween(leapDay, new LocalDate(2001, 2, 28)).getYears());
  }

  @Test
  void yearsBetween_instants_countsWholeCalendarYears() {
    DateTime start = new DateTime(2000, 2, 29, 0, 0, DateTimeZone.UTC);
    DateTime end = new DateTime(2001, 2, 28, 0, 0, DateTimeZone.UTC);
    assertEquals(1, Years.yearsBetween(start, end).getYears());
  }

  @Test
  void yearsIn_interval_countsWholeCalendarYears() {
    Interval interval =
        new Interval(
            new DateTime(2018, 1, 1, 0, 0, DateTimeZone.UTC),
            new DateTime(2021, 1, 1, 0, 0, DateTimeZone.UTC));
    assertEquals(3, Years.yearsIn(interval).getYears());
    assertEquals(Years.ZERO, Years.yearsIn(null));
  }

  @Test
  void parseYears_parsesIsoPeriodString() {
    assertEquals(Years.years(4), Years.parseYears("P4Y"));
    assertEquals(Years.ZERO, Years.parseYears(null));
  }

  @Test
  void years_isLessThan_and_isGreaterThan() {
    assertTrue(Years.years(1).isLessThan(Years.years(2)));
    assertFalse(Years.years(2).isLessThan(Years.years(1)));
    assertTrue(Years.years(2).isGreaterThan(Years.years(1)));
    assertFalse(Years.years(1).isGreaterThan(Years.years(2)));
  }

  @Test
  void years_plusMinus_bothOverloads() {
    assertEquals(Years.years(5), Years.years(2).plus(3));
    assertEquals(Years.years(5), Years.years(2).plus(Years.years(3)));
    assertEquals(Years.years(2), Years.years(5).minus(3));
    assertEquals(Years.years(2), Years.years(5).minus(Years.years(3)));
  }

  @Test
  void years_negated_toString_getPeriodType() {
    assertEquals(Years.years(-3), Years.years(3).negated());
    assertEquals("P3Y", Years.years(3).toString());
    assertEquals(PeriodType.years(), Years.years(1).getPeriodType());
  }

  @Test
  void years_serializationResolvesToCachedInstance() throws Exception {
    assertSame(Years.THREE, roundTripSerialize(Years.THREE));
  }

  // -- Cross-cutting ----------------------------------------------------------
  @Test
  void toPeriod_hasOnlyThatSingleField() {
    Period p = Days.days(5).toPeriod();
    assertEquals(5, p.getDays());
    assertEquals(0, p.getWeeks());
    assertEquals(0, p.getMonths());
  }

  @Test
  void plus_and_minus_returnSameTypeWithAdjustedAmount() {
    assertEquals(Days.days(5), Days.days(2).plus(3));
    assertEquals(Days.days(2), Days.days(5).minus(3));
  }

  @Test
  void multipliedBy_and_dividedBy() {
    assertEquals(Weeks.weeks(6), Weeks.weeks(2).multipliedBy(3));
    assertEquals(Weeks.weeks(2), Weeks.weeks(6).dividedBy(3));
    assertEquals(Days.days(2), Days.days(6).dividedBy(3));
    assertEquals(Months.months(2), Months.months(6).dividedBy(3));
    assertEquals(Years.years(2), Years.years(6).dividedBy(3));
  }

  @Test
  void negated_flipsSign() {
    assertEquals(Months.months(-3), Months.months(3).negated());
  }

  @Test
  void getFieldType_matchesDurationField() {
    assertEquals(DurationFieldType.days(), Days.days(1).getFieldType());
    assertEquals(DurationFieldType.weeks(), Weeks.weeks(1).getFieldType());
    assertEquals(DurationFieldType.months(), Months.months(1).getFieldType());
    assertEquals(DurationFieldType.years(), Years.years(1).getFieldType());
  }

  @Test
  void addingLargeValues_thatOverflowInt_throwsArithmeticException() {
    Days maxDays = Days.days(Integer.MAX_VALUE);
    assertThrows(ArithmeticException.class, () -> maxDays.plus(1));
  }

  @Test
  void equals_comparesAmount() {
    assertEquals(Days.days(7), Days.days(7));
  }

  private static Object roundTripSerialize(Object original) throws Exception {
    java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
    try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    try (java.io.ObjectInputStream in =
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
      return in.readObject();
    }
  }

  // -- exhaustive cached-value coverage ----------------------------------------
  @Test
  void weeks_factory_allCachedValues() {
    assertSame(Weeks.ZERO, Weeks.weeks(0));
    assertSame(Weeks.ONE, Weeks.weeks(1));
    assertSame(Weeks.TWO, Weeks.weeks(2));
    assertSame(Weeks.THREE, Weeks.weeks(3));
    assertSame(Weeks.MAX_VALUE, Weeks.weeks(Integer.MAX_VALUE));
    assertSame(Weeks.MIN_VALUE, Weeks.weeks(Integer.MIN_VALUE));
  }

  @Test
  void months_factory_allCachedValues() {
    assertSame(Months.ZERO, Months.months(0));
    assertSame(Months.ONE, Months.months(1));
    assertSame(Months.TWO, Months.months(2));
    assertSame(Months.THREE, Months.months(3));
    assertSame(Months.FOUR, Months.months(4));
    assertSame(Months.FIVE, Months.months(5));
    assertSame(Months.SIX, Months.months(6));
    assertSame(Months.SEVEN, Months.months(7));
    assertSame(Months.EIGHT, Months.months(8));
    assertSame(Months.NINE, Months.months(9));
    assertSame(Months.TEN, Months.months(10));
    assertSame(Months.ELEVEN, Months.months(11));
    assertSame(Months.TWELVE, Months.months(12));
  }

  // -- null-argument fast paths (mirrors Days/Weeks/Months/Years alike) -----------
  @Test
  void plusMinusIsLessIsGreater_nullArgument_fastPaths() {
    assertSame(Days.THREE, Days.THREE.plus((Days) null));
    assertSame(Days.THREE, Days.THREE.minus((Days) null));
    assertEquals(Days.THREE.getDays() > 0, Days.THREE.isGreaterThan(null));
    assertEquals(Days.THREE.getDays() < 0, Days.THREE.isLessThan(null));

    assertSame(Weeks.THREE, Weeks.THREE.plus((Weeks) null));
    assertSame(Weeks.THREE, Weeks.THREE.minus((Weeks) null));
    assertEquals(Weeks.THREE.getWeeks() > 0, Weeks.THREE.isGreaterThan(null));
    assertEquals(Weeks.THREE.getWeeks() < 0, Weeks.THREE.isLessThan(null));

    assertSame(Months.THREE, Months.THREE.plus((Months) null));
    assertSame(Months.THREE, Months.THREE.minus((Months) null));
    assertEquals(Months.THREE.getMonths() > 0, Months.THREE.isGreaterThan(null));
    assertEquals(Months.THREE.getMonths() < 0, Months.THREE.isLessThan(null));

    assertSame(Years.THREE, Years.THREE.plus((Years) null));
    assertSame(Years.THREE, Years.THREE.minus((Years) null));
    assertEquals(Years.THREE.getYears() > 0, Years.THREE.isGreaterThan(null));
    assertEquals(Years.THREE.getYears() < 0, Years.THREE.isLessThan(null));
  }

  @Test
  void dividedByOne_returnsSameInstance() {
    assertSame(Days.THREE, Days.THREE.dividedBy(1));
    assertSame(Weeks.THREE, Weeks.THREE.dividedBy(1));
    assertSame(Months.THREE, Months.THREE.dividedBy(1));
    assertSame(Years.THREE, Years.THREE.dividedBy(1));
  }

  @Test
  void multipliedBy_everyType() {
    assertEquals(Days.days(6), Days.days(2).multipliedBy(3));
    assertEquals(Months.months(6), Months.months(2).multipliedBy(3));
    assertEquals(Years.years(6), Years.years(2).multipliedBy(3));
  }

  @Test
  void yearsBetween_nonLocalDatePartials_usesGenericFieldDifference() {
    YearMonth start = new YearMonth(2018, 1);
    YearMonth end = new YearMonth(2021, 1);
    assertEquals(3, Years.yearsBetween(start, end).getYears());
  }
}
