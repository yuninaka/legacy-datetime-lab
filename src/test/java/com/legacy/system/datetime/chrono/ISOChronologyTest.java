package com.legacy.system.datetime.chrono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.legacy.system.datetime.Chronology;
import com.legacy.system.datetime.DateTimeZone;

import org.junit.jupiter.api.Test;

class ISOChronologyTest {

    @Test
    void getInstanceUTC_isUtcSingleton() {
        assertEquals(DateTimeZone.UTC, ISOChronology.getInstanceUTC().getZone());
        assertSame(ISOChronology.getInstanceUTC(), ISOChronology.getInstanceUTC());
    }

    @Test
    void isLeapYear_matchesGregorianRule() {
        Chronology chrono = ISOChronology.getInstanceUTC();
        long y2000 = chrono.getDateTimeMillis(2000, 1, 1, 0);
        long y1900 = chrono.getDateTimeMillis(1900, 1, 1, 0);
        assertEquals(true, chrono.year().isLeap(y2000));
        assertEquals(false, chrono.year().isLeap(y1900));
    }

    @Test
    void withZone_changesZoneKeepsCalendarRules() {
        ISOChronology utc = ISOChronology.getInstanceUTC();
        Chronology tokyo = utc.withZone(DateTimeZone.forID("Asia/Tokyo"));
        assertEquals(DateTimeZone.forID("Asia/Tokyo"), tokyo.getZone());
    }

    @Test
    void epoch_dayOfWeek_isThursday() {
        Chronology chrono = ISOChronology.getInstanceUTC();
        assertEquals(4, chrono.dayOfWeek().get(0L));
    }

    @Test
    void weekyear_firstDaysOfJanuary_canBelongToPreviousWeekyear() {
        // 2016-01-01 is a Friday; ISO week rules put it in weekyear 2015, week 53.
        Chronology chrono = ISOChronology.getInstanceUTC();
        long jan1_2016 = chrono.getDateTimeMillis(2016, 1, 1, 0);
        assertEquals(2015, chrono.weekyear().get(jan1_2016));
        assertEquals(53, chrono.weekOfWeekyear().get(jan1_2016));
    }

    @Test
    void getInstance_nullZone_usesDefaultZone() {
        assertEquals(DateTimeZone.getDefault(), ISOChronology.getInstance(null).getZone());
    }

    @Test
    void withZone_sameZone_returnsSameInstance() {
        ISOChronology utc = ISOChronology.getInstanceUTC();
        assertSame(utc, utc.withZone(DateTimeZone.UTC));
    }

    @Test
    void withZone_nullZone_usesDefault() {
        assertEquals(DateTimeZone.getDefault(), ISOChronology.getInstanceUTC().withZone(null).getZone());
    }

    @Test
    void toString_utc_isBareName() {
        assertEquals("ISOChronology[UTC]", ISOChronology.getInstanceUTC().toString());
    }

    @Test
    void toString_zoned_includesZoneId() {
        Chronology tokyo = ISOChronology.getInstance(DateTimeZone.forID("Asia/Tokyo"));
        assertEquals("ISOChronology[Asia/Tokyo]", tokyo.toString());
    }

    @Test
    void equals_comparesZoneOnly_notOtherChronologyTypes() {
        ISOChronology utc1 = ISOChronology.getInstanceUTC();
        ISOChronology utc2 = ISOChronology.getInstance(DateTimeZone.UTC);
        assertTrue(utc1.equals(utc2));
        assertFalse(utc1.equals(GregorianChronology.getInstanceUTC()));
        assertFalse(utc1.equals("not a chronology"));
    }

    @Test
    void serialization_resolvesToSameCachedInstance() throws Exception {
        ISOChronology original = ISOChronology.getInstance(DateTimeZone.forID("Asia/Tokyo"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        Object roundTripped;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            roundTripped = in.readObject();
        }
        assertSame(ISOChronology.getInstance(DateTimeZone.forID("Asia/Tokyo")), roundTripped);
    }
}
