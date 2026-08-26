package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertThrows(IllegalFieldValueException.class,
                () -> new LocalDateTime(2020, 6, 15, 24, 0, 0, 0));
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
    }
}
