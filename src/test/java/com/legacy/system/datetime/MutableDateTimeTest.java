package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MutableDateTimeTest {

    @Test
    void setMillis_mutatesInPlace() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.setMillis(before + DateTimeConstants.MILLIS_PER_DAY);
        assertEquals(2, mdt.getDayOfMonth());
    }

    @Test
    void addDays_mutatesSameInstance() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 31, 0, 0, 0, 0, DateTimeZone.UTC);
        mdt.addDays(1);
        assertEquals(2, mdt.getMonthOfYear());
        assertEquals(1, mdt.getDayOfMonth());
    }

    @Test
    void setYear_clampsLeapDayOnNonLeapTarget() {
        MutableDateTime mdt = new MutableDateTime(2000, 2, 29, 0, 0, 0, 0, DateTimeZone.UTC);
        mdt.setYear(2001);
        assertEquals(2, mdt.getMonthOfYear());
        assertEquals(28, mdt.getDayOfMonth());
    }

    @Test
    void copy_isIndependentInstance() {
        MutableDateTime original = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        MutableDateTime copy = original.copy();
        copy.addDays(1);
        assertEquals(1, original.getDayOfMonth());
        assertEquals(2, copy.getDayOfMonth());
        assertNotSame(original, copy);
    }

    @Test
    void toDateTime_createsImmutableSnapshot() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        DateTime snapshot = mdt.toDateTime();
        mdt.addDays(5);
        assertEquals(1, snapshot.getDayOfMonth());
        assertEquals(6, mdt.getDayOfMonth());
    }

    @Test
    void setDayOfMonth_outOfRange_throws() {
        MutableDateTime mdt = new MutableDateTime(2021, 2, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        assertThrows(IllegalFieldValueException.class, () -> mdt.setDayOfMonth(29));
    }
}
