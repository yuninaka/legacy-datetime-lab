package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YearMonthTest {

  // -- withField --------------------------------------------------------
  @Test
  void withField_changesSupportedFieldValue() {
    YearMonth ym = new YearMonth(2023, 6);
    YearMonth result = ym.withField(DateTimeFieldType.monthOfYear(), 3);
    assertEquals(2023, result.getYear());
    assertEquals(3, result.getMonthOfYear());
    assertEquals(6, ym.getMonthOfYear(), "original is unchanged (immutable)");
  }

  @Test
  void withField_sameValue_returnsSameInstance() {
    YearMonth ym = new YearMonth(2023, 6);
    assertSame(ym, ym.withField(DateTimeFieldType.monthOfYear(), 6));
  }

  @Test
  void withField_unsupportedField_throwsIllegalArgumentException() {
    YearMonth ym = new YearMonth(2023, 6);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> ym.withField(DateTimeFieldType.dayOfMonth(), 5));
    assertTrue(ex.getMessage().contains("not supported"));
  }

  @Test
  void withField_invalidValueForField_throwsIllegalFieldValueException() {
    YearMonth ym = new YearMonth(2023, 6);
    assertThrows(
        IllegalFieldValueException.class, () -> ym.withField(DateTimeFieldType.monthOfYear(), 13));
  }

  // -- withFieldAdded -----------------------------------------------------
  @Test
  void withFieldAdded_withinRange_addsToValue() {
    YearMonth ym = new YearMonth(2023, 6);
    YearMonth result = ym.withFieldAdded(DurationFieldType.months(), 3);
    assertEquals(2023, result.getYear());
    assertEquals(9, result.getMonthOfYear());
  }

  @Test
  void withFieldAdded_zeroAmount_returnsSameInstance() {
    YearMonth ym = new YearMonth(2023, 6);
    assertSame(ym, ym.withFieldAdded(DurationFieldType.months(), 0));
  }

  @Test
  void withFieldAdded_overflowsIntoYear() {
    YearMonth ym = new YearMonth(2023, 1);
    YearMonth result = ym.withFieldAdded(DurationFieldType.months(), 13);
    assertEquals(2024, result.getYear());
    assertEquals(2, result.getMonthOfYear());
  }

  @Test
  void withFieldAdded_unsupportedField_throwsIllegalArgumentException() {
    YearMonth ym = new YearMonth(2023, 6);
    assertThrows(
        IllegalArgumentException.class, () -> ym.withFieldAdded(DurationFieldType.days(), 1));
  }

  // -- withPeriodAdded ------------------------------------------------------
  @Test
  void withPeriodAdded_addsAllSupportedFields() {
    YearMonth ym = new YearMonth(2023, 6);
    YearMonth result = ym.withPeriodAdded(Period.years(1).withMonths(2), 1);
    assertEquals(2024, result.getYear());
    assertEquals(8, result.getMonthOfYear());
  }

  @Test
  void withPeriodAdded_ignoresUnsupportedFieldsInPeriod() {
    YearMonth ym = new YearMonth(2023, 6);
    YearMonth result = ym.withPeriodAdded(Period.days(10).withMonths(2), 1);
    assertEquals(2023, result.getYear());
    assertEquals(8, result.getMonthOfYear());
  }

  @Test
  void withPeriodAdded_negativeScalar_subtracts() {
    YearMonth ym = new YearMonth(2023, 6);
    YearMonth result = ym.withPeriodAdded(Period.months(2), -1);
    assertEquals(4, result.getMonthOfYear());
  }

  @Test
  void withPeriodAdded_nullPeriod_returnsSameInstance() {
    YearMonth ym = new YearMonth(2023, 6);
    assertSame(ym, ym.withPeriodAdded(null, 1));
  }

  @Test
  void withPeriodAdded_zeroScalar_returnsSameInstance() {
    YearMonth ym = new YearMonth(2023, 6);
    assertSame(ym, ym.withPeriodAdded(Period.months(2), 0));
  }

  @Test
  void withPeriodAdded_scalarMultiplicationOverflow_throwsArithmeticException() {
    YearMonth ym = new YearMonth(2023, 6);
    assertThrows(
        ArithmeticException.class, () -> ym.withPeriodAdded(Period.months(Integer.MAX_VALUE), 2));
  }
}
