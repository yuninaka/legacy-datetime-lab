package com.legacy.system.datetime.chrono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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
}
