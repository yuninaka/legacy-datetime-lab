package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.ISOChronology;

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
        assertThrows(IllegalFieldValueException.class,
                () -> new DateTime(2020, 13, 1, 0, 0, DateTimeZone.UTC));
    }

    @Test
    void constructor_invalidDayOfMonth_throws() {
        assertThrows(IllegalFieldValueException.class,
                () -> new DateTime(2021, 2, 29, 0, 0, DateTimeZone.UTC));
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
        DateTime custom = new DateTime(2020, 6, 15, 0, 0,
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
}
