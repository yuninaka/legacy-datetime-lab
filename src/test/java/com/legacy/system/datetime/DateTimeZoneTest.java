package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DateTimeZoneTest {

    @Test
    void utc_hasZeroOffsetAndIdUtc() {
        assertEquals("UTC", DateTimeZone.UTC.getID());
        assertEquals(0, DateTimeZone.UTC.getOffset(0L));
    }

    @Test
    void forID_utc_returnsUtcSingleton() {
        assertEquals(DateTimeZone.UTC, DateTimeZone.forID("UTC"));
    }

    @Test
    void forID_unknownZone_throws() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeZone.forID("Not/AZone"));
    }

    @Test
    void forID_null_returnsDefaultZone() {
        assertEquals(DateTimeZone.getDefault(), DateTimeZone.forID(null));
    }

    @Test
    void forOffsetHours_buildsFixedOffsetZone() {
        DateTimeZone plusNine = DateTimeZone.forOffsetHours(9);
        assertEquals(9 * DateTimeConstants.MILLIS_PER_HOUR, plusNine.getOffset(0L));
    }

    @Test
    void forOffsetHours_outOfRange_throws() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeZone.forOffsetHours(24));
        assertThrows(IllegalArgumentException.class, () -> DateTimeZone.forOffsetHours(-24));
    }

    @Test
    void forOffsetHoursMinutes_combinesHoursAndMinutes() {
        DateTimeZone zone = DateTimeZone.forOffsetHoursMinutes(5, 30);
        assertEquals((5 * 60 + 30) * 60 * 1000, zone.getOffset(0L));
    }

    @Test
    void fixedOffsetZone_hasNoDaylightSavings() {
        DateTimeZone fixed = DateTimeZone.forOffsetHours(2);
        assertEquals(fixed.getOffset(0L), fixed.getOffset(Long.MAX_VALUE / 2));
        assertEquals(fixed.getStandardOffset(0L), fixed.getOffset(0L));
    }

    @Test
    void namedZone_tokyo_hasNoDstAndFixedNineHourOffset() {
        DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
        DateTime summer = new DateTime(2020, 7, 1, 0, 0, tokyo);
        DateTime winter = new DateTime(2020, 1, 1, 0, 0, tokyo);
        assertEquals(9 * DateTimeConstants.MILLIS_PER_HOUR, tokyo.getOffset(summer.getMillis()));
        assertEquals(9 * DateTimeConstants.MILLIS_PER_HOUR, tokyo.getOffset(winter.getMillis()));
    }

    @Test
    void namedZone_newYork_observesDaylightSavingsOffsetChange() {
        DateTimeZone newYork = DateTimeZone.forID("America/New_York");
        DateTime beforeDst = new DateTime(2018, 3, 10, 12, 0, newYork);
        DateTime afterDst = new DateTime(2018, 3, 12, 12, 0, newYork);
        assertEquals(-5 * DateTimeConstants.MILLIS_PER_HOUR, newYork.getOffset(beforeDst.getMillis()));
        assertEquals(-4 * DateTimeConstants.MILLIS_PER_HOUR, newYork.getOffset(afterDst.getMillis()));
    }

    @Test
    void equals_sameId_areEqual() {
        assertEquals(DateTimeZone.forID("Asia/Tokyo"), DateTimeZone.forID("Asia/Tokyo"));
    }

    @Test
    void toString_returnsId() {
        assertTrue(DateTimeZone.forID("Asia/Tokyo").toString().equals("Asia/Tokyo"));
    }
}
