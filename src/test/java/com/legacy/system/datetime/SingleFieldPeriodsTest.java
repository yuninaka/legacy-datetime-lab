package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SingleFieldPeriodsTest {

    @Test
    void days_factory_smallValuesAreCached() {
        assertSame(Days.ONE, Days.days(1));
        assertSame(Days.ZERO, Days.days(0));
    }

    @Test
    void daysBetween_instants_countsWholeDays() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2020, 1, 5, 12, 0, DateTimeZone.UTC);
        assertEquals(4, Days.daysBetween(start, end).getDays());
    }

    @Test
    void daysBetween_localDates_countsWholeDays() {
        LocalDate start = new LocalDate(2020, 1, 1);
        LocalDate end = new LocalDate(2020, 3, 1);
        assertEquals(60, Days.daysBetween(start, end).getDays());
    }

    @Test
    void weeksBetween_countsWholeWeeks() {
        LocalDate start = new LocalDate(2020, 1, 1);
        LocalDate end = new LocalDate(2020, 1, 22);
        assertEquals(3, Weeks.weeksBetween(start, end).getWeeks());
    }

    @Test
    void monthsBetween_countsWholeCalendarMonths() {
        LocalDate start = new LocalDate(2020, 1, 31);
        LocalDate end = new LocalDate(2020, 3, 1);
        // Jan 31 + 1 month clamps to Feb 29 (leap year), which is before Mar 1, so 1 whole month fits.
        assertEquals(1, Months.monthsBetween(start, end).getMonths());
    }

    @Test
    void yearsBetween_countsWholeCalendarYears() {
        // Feb 29 clamped forward by 1 year lands on Feb 28 of a non-leap year (see DateTimeTest),
        // so reaching exactly that clamped date already counts as one whole year.
        LocalDate leapDay = new LocalDate(2000, 2, 29);
        assertEquals(0, Years.yearsBetween(leapDay, new LocalDate(2001, 2, 27)).getYears());
        assertEquals(1, Years.yearsBetween(leapDay, new LocalDate(2001, 2, 28)).getYears());
    }

    @Test
    void toPeriod_hasOnlyThatSingleField() {
        Period p = Days.days(5).toPeriod();
        assertEquals(5, p.getDays());
        assertEquals(0, p.getWeeks());
        assertEquals(0, p.getMonths());
    }

    @Test
    void plus_and_minus_returnSameTypeWithAdjustedAmount() {
        assertEquals(Days.days(5), Days.days(2).plus(3));
        assertEquals(Days.days(2), Days.days(5).minus(3));
    }

    @Test
    void multipliedBy_and_dividedBy() {
        assertEquals(Weeks.weeks(6), Weeks.weeks(2).multipliedBy(3));
        assertEquals(Weeks.weeks(2), Weeks.weeks(6).dividedBy(3));
    }

    @Test
    void negated_flipsSign() {
        assertEquals(Months.months(-3), Months.months(3).negated());
    }

    @Test
    void getFieldType_matchesDurationField() {
        assertEquals(DurationFieldType.days(), Days.days(1).getFieldType());
        assertEquals(DurationFieldType.weeks(), Weeks.weeks(1).getFieldType());
        assertEquals(DurationFieldType.months(), Months.months(1).getFieldType());
        assertEquals(DurationFieldType.years(), Years.years(1).getFieldType());
    }

    @Test
    void addingLargeValues_thatOverflowInt_throwsArithmeticException() {
        Days maxDays = Days.days(Integer.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> maxDays.plus(1));
    }

    @Test
    void equals_comparesAmount() {
        assertEquals(Days.days(7), Days.days(7));
    }
}
