package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MonthDayTest {

  // -- withField --------------------------------------------------------
  @Test
  void withField_changesSupportedFieldValue() {
    MonthDay md = new MonthDay(6, 15);
    MonthDay result = md.withField(DateTimeFieldType.dayOfMonth(), 20);
    assertEquals(6, result.getMonthOfYear());
    assertEquals(20, result.getDayOfMonth());
    assertEquals(15, md.getDayOfMonth(), "original is unchanged (immutable)");
  }

  @Test
  void withField_sameValue_returnsSameInstance() {
    MonthDay md = new MonthDay(6, 15);
    assertSame(md, md.withField(DateTimeFieldType.dayOfMonth(), 15));
  }

  @Test
  void withField_unsupportedField_throwsIllegalArgumentException() {
    MonthDay md = new MonthDay(6, 15);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> md.withField(DateTimeFieldType.year(), 2020));
    assertTrue(ex.getMessage().contains("not supported"));
  }

  @Test
  void withField_invalidValueForField_throwsIllegalFieldValueException() {
    MonthDay md = new MonthDay(6, 15);
    assertThrows(
        IllegalFieldValueException.class, () -> md.withField(DateTimeFieldType.monthOfYear(), 13));
  }

  // -- withFieldAdded -----------------------------------------------------
  @Test
  void withFieldAdded_withinRange_addsToValue() {
    MonthDay md = new MonthDay(6, 15);
    MonthDay result = md.withFieldAdded(DurationFieldType.days(), 5);
    assertEquals(6, result.getMonthOfYear());
    assertEquals(20, result.getDayOfMonth());
  }

  @Test
  void withFieldAdded_zeroAmount_returnsSameInstance() {
    MonthDay md = new MonthDay(6, 15);
    assertSame(md, md.withFieldAdded(DurationFieldType.days(), 0));
  }

  @Test
  void withFieldAdded_overflowsIntoMonth() {
    // January has 31 days: day 31 + 1 carries into February.
    MonthDay md = new MonthDay(1, 31);
    MonthDay result = md.withFieldAdded(DurationFieldType.days(), 1);
    assertEquals(2, result.getMonthOfYear());
    assertEquals(1, result.getDayOfMonth());
  }

  @Test
  void withFieldAdded_unsupportedField_throwsIllegalArgumentException() {
    MonthDay md = new MonthDay(6, 15);
    assertThrows(
        IllegalArgumentException.class, () -> md.withFieldAdded(DurationFieldType.years(), 1));
  }

  // -- withPeriodAdded ------------------------------------------------------
  @Test
  void withPeriodAdded_addsAllSupportedFields() {
    MonthDay md = new MonthDay(6, 15);
    MonthDay result = md.withPeriodAdded(Period.months(1).withDays(3), 1);
    assertEquals(7, result.getMonthOfYear());
    assertEquals(18, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_ignoresUnsupportedFieldsInPeriod() {
    MonthDay md = new MonthDay(6, 15);
    MonthDay result = md.withPeriodAdded(Period.years(1).withDays(3), 1);
    assertEquals(6, result.getMonthOfYear());
    assertEquals(18, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_negativeScalar_subtracts() {
    MonthDay md = new MonthDay(6, 15);
    MonthDay result = md.withPeriodAdded(Period.days(5), -1);
    assertEquals(10, result.getDayOfMonth());
  }

  @Test
  void withPeriodAdded_nullPeriod_returnsSameInstance() {
    MonthDay md = new MonthDay(6, 15);
    assertSame(md, md.withPeriodAdded(null, 1));
  }

  @Test
  void withPeriodAdded_zeroScalar_returnsSameInstance() {
    MonthDay md = new MonthDay(6, 15);
    assertSame(md, md.withPeriodAdded(Period.days(5), 0));
  }

  @Test
  void withPeriodAdded_scalarMultiplicationOverflow_throwsArithmeticException() {
    MonthDay md = new MonthDay(6, 15);
    assertThrows(
        ArithmeticException.class, () -> md.withPeriodAdded(Period.days(Integer.MAX_VALUE), 2));
  }
}
