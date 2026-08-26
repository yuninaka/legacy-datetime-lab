package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.ISOChronology;

import org.junit.jupiter.api.Test;

class LocalDateTest {

    @Test
    void constructor_usesIsoChronologyUtc_regardlessOfSystemZone() {
        LocalDate date = new LocalDate(2020, 6, 15);
        assertEquals(ISOChronology.getInstanceUTC(), date.getChronology());
    }

    @Test
    void constructor_invalidDay_throws() {
        assertThrows(IllegalFieldValueException.class, () -> new LocalDate(2021, 2, 29));
    }

    @Test
    void constructor_leapDay_isValidInLeapYear() {
        LocalDate date = new LocalDate(2020, 2, 29);
        assertEquals(29, date.getDayOfMonth());
    }

    @Test
    void equals_requiresSameFieldsAndChronology() {
        LocalDate a = new LocalDate(2020, 6, 15);
        LocalDate b = new LocalDate(2020, 6, 15);
        LocalDate c = new LocalDate(2020, 6, 16);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void compareTo_ordersLargestFieldFirst() {
        LocalDate earlier = new LocalDate(2020, 1, 1);
        LocalDate later = new LocalDate(2020, 1, 2);
        assertTrue(earlier.compareTo(later) < 0);
        assertTrue(later.compareTo(earlier) > 0);
        assertEquals(0, earlier.compareTo(new LocalDate(2020, 1, 1)));
    }

    @Test
    void compareTo_mismatchedPartialFields_throwsClassCastException() {
        LocalDate date = new LocalDate(2020, 1, 1);
        YearMonth yearMonth = new YearMonth(2020, 1);
        assertThrows(ClassCastException.class, () -> date.compareTo(yearMonth));
    }

    @Test
    void plusMonths_clampsDayOfMonth() {
        LocalDate jan31 = new LocalDate(2013, 1, 31);
        LocalDate result = jan31.plusMonths(1);
        assertEquals(2013, result.getYear());
        assertEquals(2, result.getMonthOfYear());
        assertEquals(28, result.getDayOfMonth());
    }

    @Test
    void plusYears_leapDayOntoNonLeapYear_clampsToFeb28() {
        LocalDate leapDay = new LocalDate(2000, 2, 29);
        LocalDate result = leapDay.plusYears(1);
        assertEquals(2001, result.getYear());
        assertEquals(2, result.getMonthOfYear());
        assertEquals(28, result.getDayOfMonth());
    }

    @Test
    void plusDays_rollsOverMonthAndYearBoundaries() {
        LocalDate dec31 = new LocalDate(2020, 12, 31);
        LocalDate result = dec31.plusDays(1);
        assertEquals(2021, result.getYear());
        assertEquals(1, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void getDayOfWeek_epochIsThursday() {
        LocalDate epoch = new LocalDate(1970, 1, 1);
        assertEquals(DateTimeConstants.THURSDAY, epoch.getDayOfWeek());
    }

    @Test
    void toString_isIsoDateFormat() {
        LocalDate date = new LocalDate(2020, 6, 5);
        assertEquals("2020-06-05", date.toString());
    }

    @Test
    void toDateTimeAtStartOfDay_combinesWithZone() {
        LocalDate date = new LocalDate(2020, 6, 15);
        DateTime dt = date.toDateTimeAtStartOfDay(DateTimeZone.UTC);
        assertEquals(0, dt.getMillisOfDay());
        assertEquals(2020, dt.getYear());
        assertEquals(6, dt.getMonthOfYear());
        assertEquals(15, dt.getDayOfMonth());
    }

    @Test
    void isLeapYearField_reflectsGregorianRule() {
        assertTrue(new LocalDate(2000, 1, 1).year().isLeap());
        assertFalse(new LocalDate(1900, 1, 1).year().isLeap());
    }

    @Test
    void withField_outOfRange_throws() {
        LocalDate feb1 = new LocalDate(2021, 2, 1);
        assertThrows(IllegalFieldValueException.class, () -> feb1.withDayOfMonth(29));
    }
}
