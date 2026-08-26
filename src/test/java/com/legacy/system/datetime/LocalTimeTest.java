package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LocalTimeTest {

    @Test
    void constructor_fieldValues() {
        LocalTime time = new LocalTime(10, 30, 45, 123);
        assertEquals(10, time.getHourOfDay());
        assertEquals(30, time.getMinuteOfHour());
        assertEquals(45, time.getSecondOfMinute());
        assertEquals(123, time.getMillisOfSecond());
    }

    @Test
    void constructor_hour24_throws() {
        assertThrows(IllegalFieldValueException.class, () -> new LocalTime(24, 0, 0, 0));
    }

    @Test
    void constructor_minute60_throws() {
        assertThrows(IllegalFieldValueException.class, () -> new LocalTime(0, 60, 0, 0));
    }

    @Test
    void midnight_isValid() {
        LocalTime midnight = new LocalTime(0, 0, 0, 0);
        assertEquals(0, midnight.getHourOfDay());
    }

    @Test
    void plusHours_wrapsAroundMidnightWithoutDateInformation() {
        LocalTime time = new LocalTime(23, 0, 0, 0);
        LocalTime result = time.plusHours(2);
        assertEquals(1, result.getHourOfDay());
    }

    @Test
    void equals_requiresSameFields() {
        LocalTime a = new LocalTime(10, 0, 0, 0);
        LocalTime b = new LocalTime(10, 0, 0, 0);
        LocalTime c = new LocalTime(10, 0, 0, 1);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void compareTo_ordersByFieldValues() {
        LocalTime earlier = new LocalTime(9, 0, 0, 0);
        LocalTime later = new LocalTime(9, 0, 1, 0);
        assertTrue(earlier.compareTo(later) < 0);
    }

    @Test
    void toString_isHHmmssSSS() {
        LocalTime time = new LocalTime(9, 5, 3, 7);
        assertEquals("09:05:03.007", time.toString());
    }
}
