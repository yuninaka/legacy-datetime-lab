package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimeOfDayTest {

  // -- withField --------------------------------------------------------
  @Test
  void withField_changesSupportedFieldValue() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    TimeOfDay result = t.withField(DateTimeFieldType.hourOfDay(), 5);
    assertEquals(5, result.getHourOfDay());
    assertEquals(20, result.getMinuteOfHour());
    assertEquals(10, t.getHourOfDay(), "original is unchanged (immutable)");
  }

  @Test
  void withField_sameValue_returnsSameInstance() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertSame(t, t.withField(DateTimeFieldType.hourOfDay(), 10));
  }

  @Test
  void withField_unsupportedField_throwsIllegalArgumentException() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> t.withField(DateTimeFieldType.dayOfMonth(), 5));
    assertTrue(ex.getMessage().contains("not supported"));
  }

  @Test
  void withField_invalidValueForField_throwsIllegalFieldValueException() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertThrows(
        IllegalFieldValueException.class, () -> t.withField(DateTimeFieldType.hourOfDay(), 24));
  }

  // -- withFieldAdded (wraps to a new day, per TimeOfDay's own semantics) --
  @Test
  void withFieldAdded_withinRange_addsToValue() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    TimeOfDay result = t.withFieldAdded(DurationFieldType.minutes(), 6);
    assertEquals(10, result.getHourOfDay());
    assertEquals(26, result.getMinuteOfHour());
  }

  @Test
  void withFieldAdded_zeroAmount_returnsSameInstance() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertSame(t, t.withFieldAdded(DurationFieldType.minutes(), 0));
  }

  @Test
  void withFieldAdded_overflowsIntoLargerField() {
    TimeOfDay t = new TimeOfDay(10, 55, 0, 0);
    TimeOfDay result = t.withFieldAdded(DurationFieldType.minutes(), 10);
    assertEquals(11, result.getHourOfDay());
    assertEquals(5, result.getMinuteOfHour());
  }

  @Test
  void withFieldAdded_wrapsAtEndOfDayInsteadOfThrowing() {
    // TimeOfDay.withFieldAdded always wraps (uses withFieldAddWrappedValues internally),
    // unlike Partial's withFieldAdded which would throw when there's no larger field to
    // carry into.
    TimeOfDay t = new TimeOfDay(23, 59, 59, 999);
    TimeOfDay result = t.withFieldAdded(DurationFieldType.millis(), 1);
    assertEquals(0, result.getHourOfDay());
    assertEquals(0, result.getMinuteOfHour());
    assertEquals(0, result.getSecondOfMinute());
    assertEquals(0, result.getMillisOfSecond());
  }

  @Test
  void withFieldAdded_unsupportedField_throwsIllegalArgumentException() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertThrows(
        IllegalArgumentException.class, () -> t.withFieldAdded(DurationFieldType.days(), 1));
  }

  // -- withPeriodAdded (not extracted to AbstractPartial; uses addWrapPartial directly) --
  @Test
  void withPeriodAdded_addsAllSupportedFields() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    TimeOfDay result = t.withPeriodAdded(Period.hours(1).withMinutes(5), 1);
    assertEquals(11, result.getHourOfDay());
    assertEquals(25, result.getMinuteOfHour());
  }

  @Test
  void withPeriodAdded_ignoresUnsupportedFieldsInPeriod() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    TimeOfDay result = t.withPeriodAdded(Period.years(1).withMinutes(5), 1);
    assertEquals(10, result.getHourOfDay());
    assertEquals(25, result.getMinuteOfHour());
  }

  @Test
  void withPeriodAdded_negativeScalar_subtracts() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    TimeOfDay result = t.withPeriodAdded(Period.minutes(5), -1);
    assertEquals(15, result.getMinuteOfHour());
  }

  @Test
  void withPeriodAdded_nullPeriod_returnsSameInstance() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertSame(t, t.withPeriodAdded(null, 1));
  }

  @Test
  void withPeriodAdded_zeroScalar_returnsSameInstance() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertSame(t, t.withPeriodAdded(Period.minutes(5), 0));
  }

  @Test
  void withPeriodAdded_wrapsAtEndOfDayInsteadOfThrowing() {
    TimeOfDay t = new TimeOfDay(23, 59, 0, 0);
    TimeOfDay result = t.withPeriodAdded(Period.minutes(2), 1);
    assertEquals(0, result.getHourOfDay());
    assertEquals(1, result.getMinuteOfHour());
  }

  @Test
  void withPeriodAdded_scalarMultiplicationOverflow_throwsArithmeticException() {
    TimeOfDay t = new TimeOfDay(10, 20, 30, 0);
    assertThrows(
        ArithmeticException.class, () -> t.withPeriodAdded(Period.minutes(Integer.MAX_VALUE), 2));
  }
}
