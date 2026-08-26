package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntervalTest {

    private static DateTime dt(int hour, int minute) {
        return new DateTime(2020, 1, 1, hour, minute, DateTimeZone.UTC);
    }

    @Test
    void constructor_endBeforeStart_throws() {
        assertThrows(IllegalArgumentException.class, () -> new Interval(dt(10, 0), dt(9, 0)));
    }

    @Test
    void constructor_zeroLengthInterval_isValid() {
        Interval interval = new Interval(dt(9, 0), dt(9, 0));
        assertEquals(0, interval.toDurationMillis());
    }

    // -- contains(instant): [start, end) -------------------------------------
    @Test
    void contains_instant_startInclusiveEndExclusive() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertFalse(interval.contains(dt(8, 59)));
        assertTrue(interval.contains(dt(9, 0)));
        assertTrue(interval.contains(dt(9, 59)));
        assertFalse(interval.contains(dt(10, 0)));
        assertFalse(interval.contains(dt(10, 1)));
    }

    @Test
    void contains_instant_zeroDurationIntervalContainsNothing() {
        Interval interval = new Interval(dt(14, 0), dt(14, 0));
        assertFalse(interval.contains(dt(14, 0)));
    }

    // -- contains(interval) ----------------------------------------------------
    @Test
    void contains_interval_examples() {
        Interval nineToTen = new Interval(dt(9, 0), dt(10, 0));
        assertTrue(nineToTen.contains(new Interval(dt(9, 0), dt(10, 0))));
        assertTrue(nineToTen.contains(new Interval(dt(9, 0), dt(9, 30))));
        assertTrue(nineToTen.contains(new Interval(dt(9, 30), dt(10, 0))));
        assertTrue(nineToTen.contains(new Interval(dt(9, 15), dt(9, 45))));
        assertTrue(nineToTen.contains(new Interval(dt(9, 0), dt(9, 0))));

        assertFalse(nineToTen.contains(new Interval(dt(8, 59), dt(10, 0))));
        assertFalse(nineToTen.contains(new Interval(dt(9, 0), dt(10, 1))));
        assertFalse(nineToTen.contains(new Interval(dt(10, 0), dt(10, 0))));
    }

    // -- overlaps: abutting intervals do NOT overlap ---------------------------
    @Test
    void overlaps_examples() {
        Interval nineToTen = new Interval(dt(9, 0), dt(10, 0));
        assertFalse(nineToTen.overlaps(new Interval(dt(8, 0), dt(8, 30))));
        assertFalse(nineToTen.overlaps(new Interval(dt(8, 0), dt(9, 0)))); // abuts before
        assertTrue(nineToTen.overlaps(new Interval(dt(8, 0), dt(9, 30))));
        assertTrue(nineToTen.overlaps(new Interval(dt(8, 0), dt(10, 0))));
        assertTrue(nineToTen.overlaps(new Interval(dt(9, 30), dt(11, 0))));
        assertFalse(nineToTen.overlaps(new Interval(dt(10, 0), dt(10, 0)))); // abuts after
        assertFalse(nineToTen.overlaps(new Interval(dt(10, 0), dt(11, 0)))); // abuts after
        assertFalse(nineToTen.overlaps(new Interval(dt(10, 30), dt(11, 0))));
    }

    @Test
    void isEqual_ignoresChronology_comparesMillisOnly() {
        Interval utc = new Interval(dt(9, 0), dt(10, 0));
        Interval tokyo = new Interval(
                dt(9, 0).withZone(DateTimeZone.forID("Asia/Tokyo")),
                dt(10, 0).withZone(DateTimeZone.forID("Asia/Tokyo")));
        assertTrue(utc.isEqual(tokyo));
        assertNotEquals(utc, tokyo);
    }

    @Test
    void toDuration_isExactElapsedMillis() {
        Interval interval = new Interval(dt(9, 0), dt(11, 30));
        Duration duration = interval.toDuration();
        assertEquals(2L * 60 * 60 * 1000 + 30L * 60 * 1000, duration.getMillis());
    }

    @Test
    void toPeriod_convertsToCalendarFields() {
        Interval interval = new Interval(dt(9, 0), dt(11, 30));
        Period period = interval.toPeriod();
        assertEquals(2, period.getHours());
        assertEquals(30, period.getMinutes());
    }

    @Test
    void withStart_and_withEnd_replaceEndpoint() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        Interval extended = interval.withEnd(dt(11, 0));
        assertEquals(dt(9, 0), extended.getStart());
        assertEquals(dt(11, 0), extended.getEnd());
    }

    @Test
    void withEnd_beforeStart_throws() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertThrows(IllegalArgumentException.class, () -> interval.withEnd(dt(8, 0)));
    }
}
