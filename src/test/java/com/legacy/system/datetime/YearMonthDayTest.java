package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YearMonthDayTest {

  // -- withField --------------------------------------------------------
  @Test
  void withField_changesSupportedFieldValue() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    YearMonthDay result = ymd.withField(DateTimeFieldType.dayOfMonth(), 20);
    assertEquals(2023, result.getYear());
    assertEquals(6, result.getMonthOfYear());
    assertEquals(20, result.getDayOfMonth());
    assertEquals(15, ymd.getDayOfMonth(), "original is unchanged (immutable)");
  }

  @Test
  void withField_sameValue_returnsSameInstance() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertSame(ymd, ymd.withField(DateTimeFieldType.dayOfMonth(), 15));
  }

  @Test
  void withField_unsupportedField_throwsIllegalArgumentException() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> ymd.withField(DateTimeFieldType.hourOfDay(), 5));
    assertTrue(ex.getMessage().contains("not supported"));
  }

  @Test
  void withField_invalidValueForField_throwsIllegalFieldValueException() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertThrows(
        IllegalFieldValueException.class, () -> ymd.withField(DateTimeFieldType.monthOfYear(), 13));
  }

  // -- withFieldAdded -----------------------------------------------------
  @Test
  void withFieldAdded_withinRange_addsToValue() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    YearMonthDay result = ymd.withFieldAdded(DurationFieldType.days(), 5);
    assertEquals(6, result.getMonthOfYear());
    assertEquals(20, result.getDayOfMonth());
  }

  @Test
  void withFieldAdded_zeroAmount_returnsSameInstance() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertSame(ymd, ymd.withFieldAdded(DurationFieldType.days(), 0));
  }

  @Test
  void withFieldAdded_overflowsIntoMonthAndYear() {
    // 2023-01-31 + 1 day carries into the month, and Dec 31 + 1 day carries into the year.
    YearMonthDay ymd = new YearMonthDay(2023, 12, 31);
    YearMonthDay result = ymd.withFieldAdded(DurationFieldType.days(), 1);
    assertEquals(2024, result.getYear());
    assertEquals(1, result.getMonthOfYear());
    assertEquals(1, result.getDayOfMonth());
  }

  @Test
  void withFieldAdded_unsupportedField_throwsIllegalArgumentException() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertThrows(
        IllegalArgumentException.class, () -> ymd.withFieldAdded(DurationFieldType.hours(), 1));
  }

  // -- withPeriodAdded ------------------------------------------------------
  @Test
  void withPeriodAdded_addsAllSupportedFields() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    YearMonthDay result = ymd.withPeriodAdded(Period.years(1).withMonths(1).withDays(3), 1);
    assertEquals(2024, result.getYear());
    assertEquals(7, result.getMonthOfYear());
    assertEquals(18, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_ignoresUnsupportedFieldsInPeriod() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    YearMonthDay result = ymd.withPeriodAdded(Period.hours(5).withDays(3), 1);
    assertEquals(18, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_negativeScalar_subtracts() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    YearMonthDay result = ymd.withPeriodAdded(Period.days(5), -1);
    assertEquals(10, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_nullPeriod_returnsSameInstance() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertSame(ymd, ymd.withPeriodAdded(null, 1));
  }

  @Test
  void withPeriodAdded_zeroScalar_returnsSameInstance() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertSame(ymd, ymd.withPeriodAdded(Period.days(5), 0));
  }

  @Test
  void withPeriodAdded_scalarMultiplicationOverflow_throwsArithmeticException() {
    YearMonthDay ymd = new YearMonthDay(2023, 6, 15);
    assertThrows(
        ArithmeticException.class, () -> ymd.withPeriodAdded(Period.days(Integer.MAX_VALUE), 2));
  }
}
