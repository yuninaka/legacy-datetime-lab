package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.ISOChronology;

import org.junit.jupiter.api.Test;

class InstantTest {

    @Test
    void epochConstant_isEpochZero() {
        assertEquals(0L, Instant.EPOCH.getMillis());
    }

    @Test
    void ofEpochMilli_and_ofEpochSecond() {
        assertEquals(5000L, Instant.ofEpochMilli(5000L).getMillis());
        assertEquals(5000L, Instant.ofEpochSecond(5).getMillis());
    }

    @Test
    void chronology_isAlwaysIsoUtc() {
        Instant instant = new Instant(123456789L);
        assertEquals(ISOChronology.getInstanceUTC(), instant.getChronology());
    }

    @Test
    void getZone_isAlwaysUtc() {
        Instant instant = new Instant(0L);
        assertEquals(DateTimeZone.UTC, instant.getZone());
    }

    @Test
    void equals_comparesMillisAndChronology() {
        Instant a = new Instant(1000L);
        Instant b = new Instant(1000L);
        Instant c = new Instant(2000L);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void isEqual_matchesEqualsForInstant() {
        Instant a = new Instant(1000L);
        Instant b = new Instant(1000L);
        assertTrue(a.isEqual(b));
        assertEquals(a.equals(b), a.isEqual(b));
    }

    @Test
    void compareTo_ordersByMillis() {
        Instant a = new Instant(1000L);
        Instant b = new Instant(2000L);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(new Instant(1000L)));
    }

    @Test
    void toDateTime_usesSameMillis_butSwitchesToDefaultZone() {
        // Instant overrides AbstractInstant.toDateTime(): since an Instant has no zone of its
        // own to "retain", the result uses the JVM default zone rather than UTC.
        Instant instant = new Instant(0L);
        DateTime dt = instant.toDateTime();
        assertEquals(0L, dt.getMillis());
        assertEquals(DateTimeZone.getDefault(), dt.getZone());
    }

    @Test
    void withMillis_returnsNewInstantWithSameChronology() {
        Instant a = new Instant(1000L);
        Instant b = a.withMillis(2000L);
        assertEquals(2000L, b.getMillis());
        assertEquals(a.getChronology(), b.getChronology());
    }

    @Test
    void toString_isIso8601UtcFormat() {
        Instant epoch = Instant.EPOCH;
        assertEquals("1970-01-01T00:00:00.000Z", epoch.toString());
    }
}
