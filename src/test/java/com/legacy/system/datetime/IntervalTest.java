package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    // -- constructors -----------------------------------------------------------
    @Test
    void constructor_startEndMillis() {
        Interval interval = new Interval(dt(9, 0).getMillis(), dt(10, 0).getMillis());
        assertEquals(dt(9, 0).getMillis(), interval.getStartMillis());
        assertEquals(dt(10, 0).getMillis(), interval.getEndMillis());
    }

    @Test
    void constructor_startEndMillis_withZone() {
        Interval interval = new Interval(dt(9, 0).getMillis(), dt(10, 0).getMillis(), DateTimeZone.forID("Asia/Tokyo"));
        assertEquals(DateTimeZone.forID("Asia/Tokyo"), interval.getChronology().getZone());
    }

    @Test
    void constructor_startEndMillis_withChronology() {
        Interval interval = new Interval(dt(9, 0).getMillis(), dt(10, 0).getMillis(),
                (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC(), interval.getChronology());
    }

    @Test
    void constructor_startPlusDuration() {
        Interval interval = new Interval((ReadableInstant) dt(9, 0), (ReadableDuration) Duration.standardHours(1));
        assertEquals(dt(10, 0), interval.getEnd());
    }

    @Test
    void constructor_durationPlusEnd() {
        Interval interval = new Interval((ReadableDuration) Duration.standardHours(1), (ReadableInstant) dt(10, 0));
        assertEquals(dt(9, 0), interval.getStart());
    }

    @Test
    void constructor_startPlusPeriod() {
        Interval interval = new Interval((ReadableInstant) dt(9, 0), (ReadablePeriod) Period.hours(1));
        assertEquals(dt(10, 0), interval.getEnd());
    }

    @Test
    void constructor_periodPlusEnd() {
        Interval interval = new Interval((ReadablePeriod) Period.hours(1), (ReadableInstant) dt(10, 0));
        assertEquals(dt(9, 0), interval.getStart());
    }

    @Test
    void constructor_fromObject_parsesIsoIntervalString() {
        Interval interval = new Interval((Object) "2020-01-01T09:00:00.000Z/2020-01-01T10:00:00.000Z");
        assertEquals(dt(9, 0).getMillis(), interval.getStartMillis());
        assertEquals(dt(10, 0).getMillis(), interval.getEndMillis());
    }

    @Test
    void constructor_fromObject_withChronology() {
        Interval interval = new Interval((Object) "2020-01-01T09:00:00.000Z/2020-01-01T10:00:00.000Z",
                (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC(), interval.getChronology());
    }

    // -- parse / parseWithOffset -------------------------------------------------
    @Test
    void parse_datetimeSlashDatetime() {
        Interval interval = Interval.parse("2020-01-01T09:00:00.000Z/2020-01-01T10:00:00.000Z");
        assertEquals(dt(9, 0).getMillis(), interval.getStartMillis());
        assertEquals(dt(10, 0).getMillis(), interval.getEndMillis());
    }

    @Test
    void parseWithOffset_datetimeSlashPeriod_usesParsedOffset() {
        Interval interval = Interval.parseWithOffset("2020-01-01T09:00:00.000+02:00/PT1H");
        assertEquals(DateTimeZone.forOffsetHours(2), interval.getChronology().getZone());
        assertEquals(60L * 60 * 1000, interval.toDurationMillis());
    }

    @Test
    void parseWithOffset_periodSlashDatetime() {
        Interval interval = Interval.parseWithOffset("PT1H/2020-01-01T10:00:00.000Z");
        assertEquals(dt(9, 0).getMillis(), interval.getStartMillis());
        assertEquals(dt(10, 0).getMillis(), interval.getEndMillis());
    }

    @Test
    void parseWithOffset_missingSeparator_throws() {
        assertThrows(IllegalArgumentException.class, () -> Interval.parseWithOffset("2020-01-01T09:00:00.000Z"));
    }

    // -- overlap / gap / abuts ---------------------------------------------------
    @Test
    void overlap_returnsOverlappingPortion() {
        Interval a = new Interval(dt(9, 0), dt(11, 0));
        Interval b = new Interval(dt(10, 0), dt(12, 0));
        Interval overlap = a.overlap(b);
        assertEquals(dt(10, 0), overlap.getStart());
        assertEquals(dt(11, 0), overlap.getEnd());
    }

    @Test
    void overlap_nonOverlapping_returnsNull() {
        Interval a = new Interval(dt(9, 0), dt(10, 0));
        Interval b = new Interval(dt(11, 0), dt(12, 0));
        assertEquals(null, a.overlap(b));
    }

    @Test
    void overlap_nullArgument_treatedAsNow() {
        Interval spansNow = new Interval(dt(9, 0).minusYears(50), dt(9, 0).plusYears(50));
        Interval overlap = spansNow.overlap(null);
        assertEquals(0L, overlap.toDurationMillis());
        assertTrue(spansNow.contains(overlap.getStart()));
    }

    @Test
    void gap_returnsGapBetweenNonOverlappingIntervals() {
        Interval a = new Interval(dt(9, 0), dt(10, 0));
        Interval b = new Interval(dt(11, 0), dt(12, 0));
        Interval gap = a.gap(b);
        assertEquals(dt(10, 0), gap.getStart());
        assertEquals(dt(11, 0), gap.getEnd());
    }

    @Test
    void gap_overlappingIntervals_returnsNull() {
        Interval a = new Interval(dt(9, 0), dt(11, 0));
        Interval b = new Interval(dt(10, 0), dt(12, 0));
        assertEquals(null, a.gap(b));
    }

    @Test
    void abuts_examples() {
        Interval nineToTen = new Interval(dt(9, 0), dt(10, 0));
        assertTrue(nineToTen.abuts(new Interval(dt(8, 0), dt(9, 0))));
        assertTrue(nineToTen.abuts(new Interval(dt(10, 0), dt(11, 0))));
        assertFalse(nineToTen.abuts(new Interval(dt(8, 0), dt(9, 30))));
    }

    // -- with* copy methods -------------------------------------------------------
    @Test
    void withChronology_changesChronologyKeepsMillis() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        Interval result = interval.withChronology(com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC());
        assertEquals(interval.getStartMillis(), result.getStartMillis());
        assertEquals(com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC(), result.getChronology());
    }

    @Test
    void withStartMillis_and_withStart() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertEquals(dt(8, 0), interval.withStartMillis(dt(8, 0).getMillis()).getStart());
        assertEquals(dt(8, 0), interval.withStart(dt(8, 0)).getStart());
    }

    @Test
    void withDurationAfterStart_and_beforeEnd() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertEquals(dt(11, 0), interval.withDurationAfterStart(Duration.standardHours(2)).getEnd());
        assertEquals(dt(8, 0), interval.withDurationBeforeEnd(Duration.standardHours(2)).getStart());
    }

    @Test
    void withPeriodAfterStart_and_beforeEnd() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertEquals(dt(11, 0), interval.withPeriodAfterStart(Period.hours(2)).getEnd());
        assertEquals(dt(8, 0), interval.withPeriodBeforeEnd(Period.hours(2)).getStart());
    }

    @Test
    void toInterval_returnsEqualInterval() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertEquals(interval, interval.toInterval());
    }

    @Test
    void gap_overlappingOrAbutting_returnsNull() {
        Interval a = new Interval(dt(9, 0), dt(11, 0));
        Interval overlapping = new Interval(dt(10, 0), dt(12, 0));
        assertEquals(null, a.gap(overlapping));
        Interval abutting = new Interval(dt(11, 0), dt(12, 0));
        assertEquals(null, a.gap(abutting));
    }

    @Test
    void abuts_nullArgument_treatedAsNow() {
        Interval spansNow = new Interval(dt(9, 0).minusYears(50), dt(9, 0).plusYears(50));
        assertFalse(spansNow.abuts(null));
    }

    @Test
    void sameValue_fastPaths_returnSameInstance() {
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        assertSame(interval, interval.withChronology(interval.getChronology()));
        assertSame(interval, interval.withStartMillis(interval.getStartMillis()));
        assertSame(interval, interval.withEndMillis(interval.getEndMillis()));
        assertSame(interval, interval.withDurationAfterStart(interval.toDuration()));
        assertSame(interval, interval.withDurationBeforeEnd(interval.toDuration()));
    }

    @Test
    void withPeriodAfterStart_and_beforeEnd_nullPeriod_delegatesToZeroDuration() {
        // A null period is treated as a zero duration (not "no change"), so the result is a
        // zero-length interval anchored at the original start (for AfterStart) or end (for BeforeEnd).
        Interval interval = new Interval(dt(9, 0), dt(10, 0));
        Interval afterStart = interval.withPeriodAfterStart(null);
        assertEquals(dt(9, 0), afterStart.getStart());
        assertEquals(dt(9, 0), afterStart.getEnd());
        Interval beforeEnd = interval.withPeriodBeforeEnd(null);
        assertEquals(dt(10, 0), beforeEnd.getStart());
        assertEquals(dt(10, 0), beforeEnd.getEnd());
    }

    @Test
    void parseWithOffset_emptyLeftOrRight_throws() {
        assertThrows(IllegalArgumentException.class, () -> Interval.parseWithOffset("/2020-01-01T09:00:00.000Z"));
        assertThrows(IllegalArgumentException.class, () -> Interval.parseWithOffset("2020-01-01T09:00:00.000Z/"));
    }
}
