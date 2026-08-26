package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.RoundingMode;
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

  @Test
  void standardHoursMinutesSeconds_and_millis_factories() {
    assertEquals(2L * DateTimeConstants.MILLIS_PER_HOUR, Duration.standardHours(2).getMillis());
    assertEquals(2L * DateTimeConstants.MILLIS_PER_MINUTE, Duration.standardMinutes(2).getMillis());
    assertEquals(2L * DateTimeConstants.MILLIS_PER_SECOND, Duration.standardSeconds(2).getMillis());
    assertEquals(2000L, Duration.millis(2000L).getMillis());
    assertEquals(Duration.ZERO, Duration.standardHours(0));
    assertEquals(Duration.ZERO, Duration.standardMinutes(0));
    assertEquals(Duration.ZERO, Duration.standardSeconds(0));
    assertEquals(Duration.ZERO, Duration.millis(0));
  }

  @Test
  void constructor_startEndLongs_computesDifference() {
    Duration d = new Duration(1000L, 3000L);
    assertEquals(2000L, d.getMillis());
  }

  @Test
  void constructor_fromObject_parsesIsoString() {
    Duration d = new Duration((Object) "PT1.5S");
    assertEquals(1500L, d.getMillis());
  }

  @Test
  void parse_parsesIsoString() {
    assertEquals(new Duration(1500L), Duration.parse("PT1.5S"));
  }

  @Test
  void getStandardMinutesAndHours() {
    Duration d =
        new Duration(2L * DateTimeConstants.MILLIS_PER_HOUR + DateTimeConstants.MILLIS_PER_MINUTE);
    assertEquals(2, d.getStandardHours());
    assertEquals(121, d.getStandardMinutes());
  }

  @Test
  void toStandardHoursMinutesSeconds() {
    Duration d = Duration.standardHours(3);
    assertEquals(Hours.hours(3), d.toStandardHours());
    assertEquals(Minutes.minutes(180), d.toStandardMinutes());
    assertEquals(Seconds.seconds(180 * 60), d.toStandardSeconds());
  }

  @Test
  void withMillis_returnsNewDuration() {
    Duration d = new Duration(1000L);
    assertEquals(2000L, d.withMillis(2000L).getMillis());
    assertEquals(d, d.withMillis(1000L));
  }

  @Test
  void withDurationAdded_readableDurationAndScalar() {
    Duration d = new Duration(1000L);
    assertEquals(1500L, d.withDurationAdded(Duration.millis(500L), 1).getMillis());
    assertEquals(d, d.withDurationAdded((ReadableDuration) null, 1));
    assertEquals(d, d.withDurationAdded(Duration.millis(500L), 0));
  }

  @Test
  void plus_zero_returnsSameInstance() {
    Duration d = new Duration(1000L);
    assertEquals(d, d.plus(0L));
  }

  @Test
  void minus_zero_returnsSameInstance() {
    Duration d = new Duration(1000L);
    assertEquals(d, d.minus(0L));
  }

  @Test
  void dividedBy_withRoundingMode() {
    Duration d = new Duration(10L);
    assertEquals(4L, d.dividedBy(3, RoundingMode.UP).getMillis());
    assertEquals(d, d.dividedBy(1, RoundingMode.UP));
  }

  @Test
  void nullArgument_and_sameValue_fastPaths() {
    Duration d = new Duration(1000L);
    assertEquals(d, d.plus((ReadableDuration) null));
    assertEquals(d, d.minus((ReadableDuration) null));
    assertEquals(d, d.multipliedBy(1L));
    assertEquals(d, d.dividedBy(1L));
  }
}
