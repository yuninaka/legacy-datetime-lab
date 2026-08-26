package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PeriodTest {

    @Test
    void factoryMethods_createSingleFieldStandardPeriod() {
        Period p = Period.years(2);
        assertEquals(2, p.getYears());
        assertEquals(0, p.getMonths());
        assertEquals(PeriodType.standard(), p.getPeriodType());
    }

    @Test
    void withXxx_addsAdditionalFields() {
        Period p = Period.years(2).withMonths(6);
        assertEquals(2, p.getYears());
        assertEquals(6, p.getMonths());
    }

    @Test
    void zero_hasAllFieldsZero() {
        Period zero = Period.ZERO;
        assertEquals(0, zero.getYears());
        assertEquals(0, zero.getMonths());
        assertEquals(0, zero.getDays());
        assertEquals(0, zero.getMillis());
    }

    // -- equals: field values AND period type must match ----------------------
    @Test
    void equals_oneDay_isNotEqualToTwentyFourHours() {
        Period oneDay = Period.days(1);
        Period twentyFourHours = Period.hours(24);
        assertNotEquals(oneDay, twentyFourHours);
    }

    @Test
    void equals_oneHour_isNotEqualToSixtyMinutes() {
        assertNotEquals(Period.hours(1), Period.minutes(60));
    }

    @Test
    void equals_samePeriodTypeAndFields_areEqual() {
        assertEquals(Period.years(1).withMonths(2), Period.years(1).withMonths(2));
    }

    // -- calendar-difference constructor: months preferred over days ----------
    @Test
    void instantDifference_januaryToFebruaryEndOfMonth_isExactlyOneMonth() {
        DateTime start = new DateTime(2013, 1, 31, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2013, 2, 28, 0, 0, DateTimeZone.UTC);
        Period p = new Period(start, end);
        assertEquals(1, p.getMonths());
        assertEquals(0, p.getWeeks());
        assertEquals(0, p.getDays());
        assertEquals(0, p.getYears());
    }

    @Test
    void instantDifference_januaryToMarch30_isOneMonthFourWeeksTwoDays() {
        DateTime start = new DateTime(2013, 1, 31, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2013, 3, 30, 0, 0, DateTimeZone.UTC);
        Period p = new Period(start, end);
        assertEquals(0, p.getYears());
        assertEquals(1, p.getMonths());
        assertEquals(4, p.getWeeks());
        assertEquals(2, p.getDays());
    }

    @Test
    void instantDifference_millisConstructor_decomposesIntoPreciseFieldsOnly() {
        long twoDaysThreeHoursMillis =
                2L * DateTimeConstants.MILLIS_PER_DAY + 3L * DateTimeConstants.MILLIS_PER_HOUR;
        Period p = new Period(twoDaysThreeHoursMillis);
        assertEquals(0, p.getYears());
        assertEquals(0, p.getMonths());
        assertEquals(0, p.getWeeks());
        assertEquals(0, p.getDays());
        assertEquals(51, p.getHours());
    }

    // -- fieldDifference: no borrowing between fields --------------------------
    @Test
    void fieldDifference_dayOfMonthWrap_doesNotBorrow() {
        LocalDate start = new LocalDate(2005, 6, 9);
        LocalDate end = new LocalDate(2007, 4, 12);
        Period p = Period.fieldDifference(start, end);
        assertEquals(2, p.getYears());
        assertEquals(-2, p.getMonths());
        assertEquals(3, p.getDays());
    }

    @Test
    void fieldDifference_mismatchedFieldSets_throws() {
        LocalDate date = new LocalDate(2020, 1, 1);
        LocalTime time = new LocalTime(10, 0);
        assertThrows(IllegalArgumentException.class, () -> Period.fieldDifference(date, time));
    }

    // -- parse / toString roundtrip --------------------------------------------
    @Test
    void parse_and_toString_roundtrip() {
        Period p = Period.parse("P1Y2M3W4D");
        assertEquals(1, p.getYears());
        assertEquals(2, p.getMonths());
        assertEquals(3, p.getWeeks());
        assertEquals(4, p.getDays());
        assertEquals("P1Y2M3W4D", p.toString());
    }

    @Test
    void toString_zeroPeriod_isPT0S() {
        assertEquals("PT0S", Period.ZERO.toString());
    }

    // -- normalizedStandard: fixed ratios, not calendar-aware -------------------
    @Test
    void normalizedStandard_usesFixedRatios() {
        Period p = new Period(0, 13, 0, 0, 25, 0, 0, 0); // 13 months, 25 hours
        Period normalized = p.normalizedStandard();
        assertEquals(1, normalized.getYears());
        assertEquals(1, normalized.getMonths());
        assertEquals(1, normalized.getDays());
        assertEquals(1, normalized.getHours());
    }

    // -- plus/minus/negated on stored field values -----------------------------
    @Test
    void plus_addsFieldsIndependently() {
        Period a = Period.years(1).withMonths(2);
        Period b = Period.years(3).withMonths(4);
        Period sum = a.plus(b);
        assertEquals(4, sum.getYears());
        assertEquals(6, sum.getMonths());
    }

    @Test
    void negated_negatesAllFields() {
        Period p = Period.years(1).withMonths(-2);
        Period negated = p.negated();
        assertEquals(-1, negated.getYears());
        assertEquals(2, negated.getMonths());
    }

    @Test
    void applyingPeriod_toDateTime_addsFieldsLargestFirst() {
        DateTime start = new DateTime(2000, 1, 31, 0, 0, DateTimeZone.UTC);
        Period p = Period.months(1).withDays(1);
        // Adding months first: Jan 31 + 1 month -> Feb 29 (leap year), then + 1 day -> Mar 1.
        DateTime result = start.plus(p);
        assertEquals(2000, result.getYear());
        assertEquals(3, result.getMonthOfYear());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    void toStandardDuration_convertsUsingFixedFieldLengths() {
        Duration d = Period.days(1).toStandardDuration();
        assertEquals(DateTimeConstants.MILLIS_PER_DAY, d.getMillis());
    }

    @Test
    void toStandardDuration_withVariableField_throws() {
        assertThrows(UnsupportedOperationException.class, () -> Period.months(1).toStandardDuration());
    }

    @Test
    void unsupportedFieldAccessor_returnsZeroNotException() {
        // Days-only PeriodType does not support years; getYears() on such a Period returns 0.
        Period days = new Period(0, 0, 0, 5, 0, 0, 0, 0, PeriodType.days());
        assertEquals(0, days.getYears());
        assertEquals(5, days.getDays());
    }

    @Test
    void constructorRejectsUnsupportedFieldValue() {
        // PeriodType.days() only supports the days field; a non-zero years value is rejected.
        assertThrows(IllegalArgumentException.class,
                () -> new Period(1, 0, 0, 0, 0, 0, 0, 0, PeriodType.days()));
    }

    @Test
    void isEqual_isSymmetricForEqualPeriods() {
        Period a = Period.days(2);
        Period b = Period.days(2);
        assertTrue(a.equals(b));
        assertFalse(a.equals(Period.days(3)));
    }

    // -- constructors ---------------------------------------------------------
    @Test
    void constructor_fourInts_isHoursMinutesSecondsMillis() {
        Period p = new Period(1, 2, 3, 4);
        assertEquals(1, p.getHours());
        assertEquals(2, p.getMinutes());
        assertEquals(3, p.getSeconds());
        assertEquals(4, p.getMillis());
    }

    @Test
    void constructor_startEndInstants_withPeriodType() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2020, 1, 3, 0, 0, DateTimeZone.UTC);
        Period p = new Period(start, end, PeriodType.days());
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_startInstantPlusDuration() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        Period p = new Period(start, Duration.standardDays(1));
        assertEquals(1, p.getDays());
    }

    @Test
    void constructor_durationPlusEndInstant() {
        DateTime end = new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC);
        Period p = new Period(Duration.standardDays(1), end);
        assertEquals(1, p.getDays());
    }

    @Test
    void constructor_startEndInstants_withoutType_usesStandardType() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2020, 1, 3, 0, 0, DateTimeZone.UTC);
        Period p = new Period((ReadableInstant) start, (ReadableInstant) end);
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_startPartialEndPartial() {
        LocalDate start = new LocalDate(2020, 1, 1);
        LocalDate end = new LocalDate(2020, 1, 3);
        Period p = new Period((ReadablePartial) start, (ReadablePartial) end);
        assertEquals(2, p.getDays());
        Period pTyped = new Period((ReadablePartial) start, (ReadablePartial) end, PeriodType.days());
        assertEquals(2, pTyped.getDays());
    }

    @Test
    void constructor_startEndInstants_withType() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2020, 1, 3, 0, 0, DateTimeZone.UTC);
        Period p = new Period((ReadableInstant) start, (ReadableInstant) end, PeriodType.days());
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_startInstantPlusDuration_withType() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        Period p = new Period((ReadableInstant) start, (ReadableDuration) Duration.standardDays(1), PeriodType.days());
        assertEquals(1, p.getDays());
    }

    @Test
    void constructor_durationPlusEndInstant_withType() {
        DateTime end = new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC);
        Period p = new Period((ReadableDuration) Duration.standardDays(1), (ReadableInstant) end, PeriodType.days());
        assertEquals(1, p.getDays());
    }

    @Test
    void constructor_millisWithType() {
        Period p = new Period(90_000L, PeriodType.standard());
        assertEquals(1, p.getMinutes());
        assertEquals(30, p.getSeconds());
    }

    @Test
    void constructor_millisWithChronology() {
        Period p = new Period(DateTimeConstants.MILLIS_PER_DAY, (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertTrue(p.getHours() > 0 || p.getDays() > 0);
    }

    @Test
    void constructor_startEndLongsWithChronology() {
        long start = 0L;
        long end = DateTimeConstants.MILLIS_PER_DAY * 2L;
        Period p = new Period(start, end, (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_startEndLongsWithTypeAndChronology() {
        long start = 0L;
        long end = DateTimeConstants.MILLIS_PER_DAY * 2L;
        Period p = new Period(start, end, PeriodType.days(), com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_fromObject_parsesIsoString() {
        Period p = new Period((Object) "P1Y2M3D");
        assertEquals(1, p.getYears());
        assertEquals(2, p.getMonths());
        assertEquals(3, p.getDays());
    }

    // -- withXxx per field ------------------------------------------------------
    @Test
    void withXxx_everyStandardField() {
        Period p = Period.ZERO;
        assertEquals(1, p.withYears(1).getYears());
        assertEquals(2, p.withMonths(2).getMonths());
        assertEquals(3, p.withWeeks(3).getWeeks());
        assertEquals(4, p.withDays(4).getDays());
        assertEquals(5, p.withHours(5).getHours());
        assertEquals(6, p.withMinutes(6).getMinutes());
        assertEquals(7, p.withSeconds(7).getSeconds());
        assertEquals(8, p.withMillis(8).getMillis());
    }

    @Test
    void withField_setsFieldByDurationType() {
        Period p = Period.ZERO.withField(DurationFieldType.days(), 5);
        assertEquals(5, p.getDays());
    }

    @Test
    void withFieldAdded_addsFieldByDurationType() {
        Period p = Period.days(2).withFieldAdded(DurationFieldType.days(), 3);
        assertEquals(5, p.getDays());
    }

    @Test
    void withPeriodType_changesSupportedFields() {
        Period p = Period.days(2).withPeriodType(PeriodType.days());
        assertEquals(PeriodType.days(), p.getPeriodType());
        assertEquals(2, p.getDays());
    }

    // -- plusXxx / minusXxx per field ----------------------------------------
    @Test
    void plusXxx_everyStandardField() {
        Period p = Period.ZERO;
        assertEquals(1, p.plusYears(1).getYears());
        assertEquals(1, p.plusMonths(1).getMonths());
        assertEquals(1, p.plusWeeks(1).getWeeks());
        assertEquals(1, p.plusDays(1).getDays());
        assertEquals(1, p.plusHours(1).getHours());
        assertEquals(1, p.plusMinutes(1).getMinutes());
        assertEquals(1, p.plusSeconds(1).getSeconds());
        assertEquals(1, p.plusMillis(1).getMillis());
    }

    @Test
    void minusXxx_everyStandardField() {
        Period p = Period.years(5).withMonths(5).withWeeks(5).withDays(5)
                .withHours(5).withMinutes(5).withSeconds(5).withMillis(5);
        assertEquals(4, p.minusYears(1).getYears());
        assertEquals(4, p.minusMonths(1).getMonths());
        assertEquals(4, p.minusWeeks(1).getWeeks());
        assertEquals(4, p.minusDays(1).getDays());
        assertEquals(4, p.minusHours(1).getHours());
        assertEquals(4, p.minusMinutes(1).getMinutes());
        assertEquals(4, p.minusSeconds(1).getSeconds());
        assertEquals(4, p.minusMillis(1).getMillis());
    }

    @Test
    void minus_readablePeriod_subtractsFieldsIndependently() {
        Period a = Period.years(5).withMonths(3);
        Period b = Period.years(2).withMonths(1);
        Period diff = a.minus(b);
        assertEquals(3, diff.getYears());
        assertEquals(2, diff.getMonths());
    }

    // -- toStandardXxx ------------------------------------------------------------
    @Test
    void toStandardMinutesHoursDaysWeeksSeconds() {
        Period p = new Period(0, 0, 0, 15, 0, 0, 0, 0); // 15 days, standard type
        assertEquals(2, p.toStandardWeeks().getWeeks());
        assertEquals(360, p.toStandardHours().getHours());
        assertEquals(360 * 60, p.toStandardMinutes().getMinutes());
        assertEquals(360 * 60 * 60, p.toStandardSeconds().getSeconds());
        assertEquals(15, p.toStandardDays().getDays());
    }

    // -- null-argument / zero-amount / same-value fast paths --------------------------
    @Test
    void nullAndZero_fastPaths_returnSameInstance() {
        Period p = Period.years(1).withMonths(2);
        assertSame(p, p.plus((ReadablePeriod) null));
        assertSame(p, p.minus((ReadablePeriod) null));
        assertSame(p, p.withFields(null));
        assertSame(p, p.withFieldAdded(DurationFieldType.years(), 0));
        assertSame(p, p.plusYears(0));
        assertSame(p, p.plusMonths(0));
        assertSame(p, p.plusWeeks(0));
        assertSame(p, p.plusDays(0));
        assertSame(p, p.plusHours(0));
        assertSame(p, p.plusMinutes(0));
        assertSame(p, p.plusSeconds(0));
        assertSame(p, p.plusMillis(0));
        assertSame(p, p.multipliedBy(1));
        assertSame(p, p.withPeriodType(p.getPeriodType()));
    }

    @Test
    void fieldDifference_nullArguments_throws() {
        LocalDate date = new LocalDate(2020, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> Period.fieldDifference(null, date));
        assertThrows(IllegalArgumentException.class, () -> Period.fieldDifference(date, null));
    }

    // -- never-called factories / constructors ----------------------------------------
    @Test
    void weeksSecondsMillisFactories() {
        assertEquals(2, Period.weeks(2).getWeeks());
        assertEquals(2, Period.seconds(2).getSeconds());
        assertEquals(2, Period.millis(2).getMillis());
    }

    @Test
    void toPeriod_returnsSameInstance() {
        Period p = Period.days(1);
        assertSame(p, p.toPeriod());
    }

    @Test
    void checkYearsAndMonths_yearsOnly_alsoThrows() {
        Period p = Period.years(1);
        assertThrows(UnsupportedOperationException.class, p::toStandardDuration);
    }

    @Test
    void constructor_objectWithTypeAndChronology() {
        Period p = new Period((Object) "P1Y2M3D", PeriodType.yearMonthDayTime(),
                (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(1, p.getYears());
        assertEquals(2, p.getMonths());
        assertEquals(3, p.getDays());
    }

    @Test
    void constructor_objectWithChronology() {
        Period p = new Period((Object) "P1Y2M3D", (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(1, p.getYears());
    }

    @Test
    void constructor_startEndLongs_withType() {
        Period p = new Period(0L, 2L * DateTimeConstants.MILLIS_PER_DAY, PeriodType.days());
        assertEquals(2, p.getDays());
    }

    @Test
    void constructor_startEndLongs_noType() {
        Period p = new Period(0L, 2L * DateTimeConstants.MILLIS_PER_DAY);
        assertEquals(2, p.getDays());
    }
}
