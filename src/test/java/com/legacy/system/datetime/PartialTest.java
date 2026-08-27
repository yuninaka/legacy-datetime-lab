package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PartialTest {

  private static Partial monthDayPartial(int month, int day) {
    return new Partial(
        new DateTimeFieldType[] {DateTimeFieldType.monthOfYear(), DateTimeFieldType.dayOfMonth()},
        new int[] {month, day});
  }

  private static Partial dayOnlyPartial(int day) {
    return new Partial(DateTimeFieldType.dayOfMonth(), day);
  }

  // -- withField --------------------------------------------------------
  @Test
  void withField_changesSupportedFieldValue() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withField(DateTimeFieldType.monthOfYear(), 3);
    assertEquals(3, result.getValue(0));
    assertEquals(15, result.getValue(1));
    assertEquals(6, p.getValue(0), "original is unchanged (immutable)");
  }

  @Test
  void withField_sameValue_returnsSameInstance() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withField(DateTimeFieldType.monthOfYear(), 6);
    assertSame(p, result);
  }

  @Test
  void withField_unsupportedField_throwsIllegalArgumentException() {
    Partial p = monthDayPartial(6, 15);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> p.withField(DateTimeFieldType.hourOfDay(), 5));
    assertTrue(ex.getMessage().contains("not supported"));
  }

  @Test
  void withField_invalidValueForField_throwsIllegalFieldValueException() {
    Partial p = monthDayPartial(6, 15);
    assertThrows(
        IllegalFieldValueException.class, () -> p.withField(DateTimeFieldType.monthOfYear(), 13));
  }

  // -- withFieldAdded (no wrap; overflows into a larger present field, or throws if none) --
  @Test
  void withFieldAdded_withinRange_addsToValue() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withFieldAdded(DurationFieldType.days(), 5);
    assertEquals(20, result.getValue(1));
    assertEquals(6, result.getValue(0));
  }

  @Test
  void withFieldAdded_zeroAmount_returnsSameInstance() {
    Partial p = monthDayPartial(6, 15);
    assertSame(p, p.withFieldAdded(DurationFieldType.days(), 0));
  }

  @Test
  void withFieldAdded_overflowsIntoLargerPresentField() {
    // June has 30 days: day 28 + 5 = 33 -> overflows into monthOfYear (July 3rd).
    Partial p = monthDayPartial(6, 28);
    Partial result = p.withFieldAdded(DurationFieldType.days(), 5);
    assertEquals(7, result.getValue(0));
    assertEquals(3, result.getValue(1));
  }

  @Test
  void withFieldAdded_noLargerFieldToOverflowInto_throwsWhenExceedingMax() {
    // dayOfMonth is the only (and therefore top-level) field; 25 + 10 = 35 exceeds the
    // 31-day max with nowhere to carry into, so this must fail rather than wrap.
    Partial p = dayOnlyPartial(25);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> p.withFieldAdded(DurationFieldType.days(), 10));
    assertTrue(ex.getMessage().contains("Maximum value exceeded"));
  }

  @Test
  void withFieldAdded_unsupportedField_throwsIllegalArgumentException() {
    Partial p = monthDayPartial(6, 15);
    assertThrows(
        IllegalArgumentException.class, () -> p.withFieldAdded(DurationFieldType.hours(), 1));
  }

  // -- withFieldAddWrapped (wraps at the top instead of overflowing/throwing) --
  @Test
  void withFieldAddWrapped_withinRange_addsToValue() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withFieldAddWrapped(DurationFieldType.days(), 5);
    assertEquals(20, result.getValue(1));
  }

  @Test
  void withFieldAddWrapped_zeroAmount_returnsSameInstance() {
    Partial p = monthDayPartial(6, 15);
    assertSame(p, p.withFieldAddWrapped(DurationFieldType.days(), 0));
  }

  @Test
  void withFieldAddWrapped_noLargerFieldToOverflowInto_wrapsInsteadOfThrowing() {
    // Same scenario that throws for withFieldAdded above (25 + 10 on a day-only Partial):
    // here it wraps mod 31 back to 4, instead of failing.
    Partial p = dayOnlyPartial(25);
    Partial result = p.withFieldAddWrapped(DurationFieldType.days(), 10);
    assertEquals(4, result.getValue(0));
  }

  @Test
  void withFieldAddWrapped_unsupportedField_throwsIllegalArgumentException() {
    Partial p = monthDayPartial(6, 15);
    assertThrows(
        IllegalArgumentException.class, () -> p.withFieldAddWrapped(DurationFieldType.hours(), 1));
  }

  // -- withPeriodAdded ----------------------------------------------------
  @Test
  void withPeriodAdded_addsAllSupportedFields() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withPeriodAdded(Period.months(1).withDays(3), 1);
    assertEquals(7, result.getValue(0));
    assertEquals(18, result.getValue(1));
  }

  @Test
  void withPeriodAdded_ignoresUnsupportedFieldsInPeriod() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withPeriodAdded(Period.years(1).withDays(3), 1);
    // years() is not supported by this Partial: only the days component should apply.
    assertEquals(6, result.getValue(0));
    assertEquals(18, result.getValue(1));
  }

  @Test
  void withPeriodAdded_negativeScalar_subtracts() {
    Partial p = monthDayPartial(6, 15);
    Partial result = p.withPeriodAdded(Period.days(5), -1);
    assertEquals(10, result.getValue(1));
  }

  @Test
  void withPeriodAdded_nullPeriod_returnsSameInstance() {
    Partial p = monthDayPartial(6, 15);
    assertSame(p, p.withPeriodAdded(null, 1));
  }

  @Test
  void withPeriodAdded_zeroScalar_returnsSameInstance() {
    Partial p = monthDayPartial(6, 15);
    assertSame(p, p.withPeriodAdded(Period.days(5), 0));
  }

  @Test
  void withPeriodAdded_scalarMultiplicationOverflow_throwsArithmeticException() {
    // withPeriodAddedValues computes FieldUtils.safeMultiply(period.getValue(i), scalar)
    // before ever touching the field; Integer.MAX_VALUE * 2 overflows int range.
    Partial p = monthDayPartial(6, 15);
    assertThrows(
        ArithmeticException.class, () -> p.withPeriodAdded(Period.days(Integer.MAX_VALUE), 2));
  }
}
