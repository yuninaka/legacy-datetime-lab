package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DurationTest {

    @Test
    void zero_hasZeroMillis() {
        assertEquals(0L, Duration.ZERO.getMillis());
    }

    @Test
    void standardDays_multipliesByMillisPerDay() {
        Duration d = Duration.standardDays(2);
        assertEquals(2L * DateTimeConstants.MILLIS_PER_DAY, d.getMillis());
    }

    @Test
    void standardDaysZero_returnsZeroSingleton() {
        assertEquals(Duration.ZERO, Duration.standardDays(0));
    }

    @Test
    void constructor_fromInstants_computesDifference() {
        DateTime start = new DateTime(2020, 1, 1, 0, 0, DateTimeZone.UTC);
        DateTime end = new DateTime(2020, 1, 2, 0, 0, DateTimeZone.UTC);
        Duration d = new Duration(start, end);
        assertEquals(DateTimeConstants.MILLIS_PER_DAY, d.getMillis());
    }

    @Test
    void getStandardSeconds_truncatesTowardZero_forPositiveMillis() {
        Duration d = new Duration(2999L);
        assertEquals(2L, d.getStandardSeconds());
    }

    @Test
    void getStandardSeconds_truncatesTowardZero_forNegativeMillis() {
        Duration d = new Duration(-2999L);
        assertEquals(-2L, d.getStandardSeconds());
    }

    @Test
    void getStandardDays_truncatesTowardZero() {
        Duration d = new Duration(DateTimeConstants.MILLIS_PER_DAY + 1);
        assertEquals(1L, d.getStandardDays());
    }

    @Test
    void toStandardDays_returnsDaysObject() {
        Duration d = Duration.standardDays(3);
        assertEquals(Days.days(3), d.toStandardDays());
    }

    @Test
    void plus_addsMillis() {
        Duration a = new Duration(1000L);
        Duration b = new Duration(2000L);
        assertEquals(3000L, a.plus(b).getMillis());
    }

    @Test
    void minus_subtractsMillis() {
        Duration a = new Duration(3000L);
        Duration b = new Duration(1000L);
        assertEquals(2000L, a.minus(b).getMillis());
    }

    @Test
    void multipliedBy_scalesMillis() {
        Duration d = new Duration(1000L);
        assertEquals(3000L, d.multipliedBy(3).getMillis());
    }

    @Test
    void dividedBy_truncatesTowardZero() {
        Duration d = new Duration(10L);
        assertEquals(3L, d.dividedBy(3).getMillis());
    }

    @Test
    void dividedBy_zero_throwsArithmeticException() {
        Duration d = new Duration(10L);
        assertThrows(ArithmeticException.class, () -> d.dividedBy(0));
    }

    @Test
    void negated_flipsSign() {
        Duration d = new Duration(1000L);
        assertEquals(-1000L, d.negated().getMillis());
    }

    @Test
    void negated_minValue_throwsArithmeticException() {
        Duration d = new Duration(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, d::negated);
    }

    @Test
    void abs_returnsNonNegativeDuration() {
        assertEquals(1000L, new Duration(-1000L).abs().getMillis());
        assertEquals(1000L, new Duration(1000L).abs().getMillis());
    }

    @Test
    void toStandardDuration_isSelf() {
        Duration d = new Duration(500L);
        assertEquals(d, d.toDuration());
    }

    @Test
    void equals_comparesMillisValue() {
        assertEquals(new Duration(1000L), new Duration(1000L));
    }
}
