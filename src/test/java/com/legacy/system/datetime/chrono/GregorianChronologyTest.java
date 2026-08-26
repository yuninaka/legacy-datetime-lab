package com.legacy.system.datetime.chrono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.Chronology;
import com.legacy.system.datetime.DateTimeZone;
import com.legacy.system.datetime.IllegalFieldValueException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GregorianChronologyTest {

  private static Object roundTripSerialize(Object original)
      throws IOException, ClassNotFoundException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(original);
    }
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      return in.readObject();
    }
  }

  @Test
  void getInstanceUTC_isUtcSingleton() {
    GregorianChronology chrono = GregorianChronology.getInstanceUTC();
    assertEquals(DateTimeZone.UTC, chrono.getZone());
    assertSame(chrono, GregorianChronology.getInstanceUTC());
  }

  @Test
  void getInstance_withZone_isCachedPerZone() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    GregorianChronology chrono1 = GregorianChronology.getInstance(tokyo);
    GregorianChronology chrono2 = GregorianChronology.getInstance(tokyo);
    assertSame(chrono1, chrono2);
    assertEquals(tokyo, chrono1.getZone());
    assertNotSame(chrono1, GregorianChronology.getInstanceUTC());
  }

  @Test
  void getInstance_nullZone_usesDefaultZone() {
    GregorianChronology chrono = GregorianChronology.getInstance(null);
    assertEquals(DateTimeZone.getDefault(), chrono.getZone());
  }

  @ParameterizedTest
  @CsvSource({"0", "8", "-1", "100"})
  void getInstance_invalidMinDaysInFirstWeek_throws(int invalid) {
    assertThrows(
        IllegalArgumentException.class,
        () -> GregorianChronology.getInstance(DateTimeZone.UTC, invalid));
  }

  @Test
  void getInstance_minDaysInFirstWeek_isStored() {
    GregorianChronology chrono = GregorianChronology.getInstance(DateTimeZone.UTC, 7);
    assertEquals(7, chrono.getMinimumDaysInFirstWeek());
  }

  @Test
  void withUTC_returnsUtcSingleton() {
    GregorianChronology zoned = GregorianChronology.getInstance(DateTimeZone.forID("Asia/Tokyo"));
    assertSame(GregorianChronology.getInstanceUTC(), zoned.withUTC());
  }

  @Test
  void withZone_sameZone_returnsSameInstance() {
    GregorianChronology chrono = GregorianChronology.getInstanceUTC();
    assertSame(chrono, chrono.withZone(DateTimeZone.UTC));
  }

  @Test
  void withZone_nullZone_usesDefault() {
    GregorianChronology chrono = GregorianChronology.getInstanceUTC();
    assertEquals(DateTimeZone.getDefault(), chrono.withZone(null).getZone());
  }

  // -- leap year rule: divisible by 4, unless by 100 and not by 400 --------
  @ParameterizedTest
  @CsvSource({
    "2000, true",
    "1600, true",
    "2400, true",
    "2024, true",
    "2028, true",
    "1700, false",
    "1800, false",
    "1900, false",
    "2100, false",
    "2023, false",
    "0, true",
    "-4, true",
    "-100, false",
    "-400, true",
  })
  void isLeapYear(int year, boolean expectedLeap) {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    long jan1 = chrono.getDateTimeMillis(year, 1, 1, 0);
    assertEquals(expectedLeap, chrono.year().isLeap(jan1));
  }

  @Test
  void februaryLength_matchesLeapYearRule() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    assertEquals(29, chrono.dayOfMonth().getMaximumValue(chrono.getDateTimeMillis(2000, 2, 1, 0)));
    assertEquals(28, chrono.dayOfMonth().getMaximumValue(chrono.getDateTimeMillis(1900, 2, 1, 0)));
    assertEquals(29, chrono.dayOfMonth().getMaximumValue(chrono.getDateTimeMillis(2024, 2, 1, 0)));
    assertEquals(28, chrono.dayOfMonth().getMaximumValue(chrono.getDateTimeMillis(2023, 2, 1, 0)));
  }

  @Test
  void minAndMaxYear() {
    GregorianChronology chrono = GregorianChronology.getInstanceUTC();
    assertEquals(-292275054, chrono.year().getMinimumValue());
    assertEquals(292278993, chrono.year().getMaximumValue());
  }

  @Test
  void yearBeyondMaxPlusOne_throwsIllegalFieldValueException() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    assertThrows(
        IllegalFieldValueException.class, () -> chrono.getDateTimeMillis(292278995, 1, 1, 0));
  }

  @Test
  void invalidDayOfMonth_throwsIllegalFieldValueException() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    IllegalFieldValueException ex =
        assertThrows(
            IllegalFieldValueException.class, () -> chrono.getDateTimeMillis(2023, 2, 30, 0));
    assertTrue(ex.getMessage().contains("dayOfMonth"));
  }

  @Test
  void invalidMonth_throwsIllegalFieldValueException() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    assertThrows(IllegalFieldValueException.class, () -> chrono.getDateTimeMillis(2023, 13, 1, 0));
  }

  @Test
  void invalidHour_throwsIllegalFieldValueException() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    assertThrows(
        IllegalFieldValueException.class, () -> chrono.getDateTimeMillis(2023, 1, 1, 24, 0, 0, 0));
  }

  // -- day of week: epoch (1970-01-01) is a Thursday ------------------------
  @Test
  void epoch_isThursday() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    assertEquals(4, chrono.dayOfWeek().get(0L));
  }

  @Test
  void dayOfWeek_beforeEpoch_isConsistent() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    // 1969-12-31 is a Wednesday.
    long millis = chrono.getDateTimeMillis(1969, 12, 31, 0);
    assertEquals(3, chrono.dayOfWeek().get(millis));
  }

  @Test
  void monthAdd_clampsDayOfMonthToTargetMonthLength() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    long jan31 = chrono.getDateTimeMillis(2013, 1, 31, 0);
    long result = chrono.monthOfYear().add(jan31, 1);
    assertEquals(2013, chrono.year().get(result));
    assertEquals(2, chrono.monthOfYear().get(result));
    assertEquals(28, chrono.dayOfMonth().get(result));
  }

  @Test
  void monthAdd_leapYearFebruary_clampsTo29() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    long mar31 = chrono.getDateTimeMillis(2000, 3, 31, 0);
    long result = chrono.monthOfYear().add(mar31, -1);
    assertEquals(2000, chrono.year().get(result));
    assertEquals(2, chrono.monthOfYear().get(result));
    assertEquals(29, chrono.dayOfMonth().get(result));
  }

  @Test
  void setYear_leapDayMovedToNonLeapYear_becomesFeb28() {
    Chronology chrono = GregorianChronology.getInstanceUTC();
    long feb29_2000 = chrono.getDateTimeMillis(2000, 2, 29, 0);
    long result = chrono.year().set(feb29_2000, 2001);
    assertEquals(2, chrono.monthOfYear().get(result));
    assertEquals(28, chrono.dayOfMonth().get(result));
  }

  @Test
  void equalsAndHashCode_considerZoneAndMinDaysInFirstWeek() {
    GregorianChronology utc1 = GregorianChronology.getInstanceUTC();
    GregorianChronology utc2 = GregorianChronology.getInstance(DateTimeZone.UTC, 4);
    GregorianChronology utc7 = GregorianChronology.getInstance(DateTimeZone.UTC, 7);
    assertEquals(utc1, utc2);
    assertEquals(utc1.hashCode(), utc2.hashCode());
    assertFalse(utc1.equals(utc7));
  }

  @Test
  void toString_containsZoneId() {
    GregorianChronology chrono = GregorianChronology.getInstanceUTC();
    assertTrue(chrono.toString().contains("UTC"));
  }

  @Test
  void getInstance_noArgs_usesDefaultZone() {
    GregorianChronology chrono = GregorianChronology.getInstance();
    assertEquals(DateTimeZone.getDefault(), chrono.getZone());
    assertEquals(4, chrono.getMinimumDaysInFirstWeek());
  }

  @Test
  void getInstance_nullZoneWithMinDays_usesDefaultZone() {
    GregorianChronology chrono = GregorianChronology.getInstance(null, 5);
    assertEquals(DateTimeZone.getDefault(), chrono.getZone());
    assertEquals(5, chrono.getMinimumDaysInFirstWeek());
  }

  @Test
  void getInstance_zonedTwice_returnsSameCachedInstance() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    assertSame(
        GregorianChronology.getInstance(tokyo, 6), GregorianChronology.getInstance(tokyo, 6));
  }

  @Test
  void serialization_resolvesToSameCachedInstance() throws Exception {
    GregorianChronology original =
        GregorianChronology.getInstance(DateTimeZone.forID("Asia/Tokyo"), 6);
    GregorianChronology roundTripped = (GregorianChronology) roundTripSerialize(original);
    assertSame(GregorianChronology.getInstance(DateTimeZone.forID("Asia/Tokyo"), 6), roundTripped);
  }

  @Test
  void serialization_utcInstance_resolvesToUtcSingleton() throws Exception {
    GregorianChronology roundTripped =
        (GregorianChronology) roundTripSerialize(GregorianChronology.getInstanceUTC());
    assertSame(GregorianChronology.getInstanceUTC(), roundTripped);
  }
}
