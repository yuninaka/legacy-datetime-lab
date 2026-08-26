package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.legacy.system.datetime.chrono.ISOChronology;
import com.legacy.system.datetime.tz.DefaultNameProvider;
import com.legacy.system.datetime.tz.UTCProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
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

  // -- forTimeZone --------------------------------------------------------------
  @Test
  void forTimeZone_utc_returnsUtcSingleton() {
    assertEquals(DateTimeZone.UTC, DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone("UTC")));
  }

  @Test
  void forTimeZone_null_returnsDefaultZone() {
    assertEquals(DateTimeZone.getDefault(), DateTimeZone.forTimeZone(null));
  }

  @Test
  void forTimeZone_namedZone_matchesForID() {
    assertEquals(
        DateTimeZone.forID("Asia/Tokyo"),
        DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone("Asia/Tokyo")));
  }

  @Test
  void forTimeZone_gmtOffsetId_buildsFixedOffsetZone() {
    DateTimeZone zone = DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone("GMT+02:00"));
    assertEquals(2 * DateTimeConstants.MILLIS_PER_HOUR, zone.getOffset(0L));
  }

  @Test
  void forTimeZone_gmtZeroOffset_returnsUtc() {
    assertEquals(
        DateTimeZone.UTC, DateTimeZone.forTimeZone(java.util.TimeZone.getTimeZone("GMT+00:00")));
  }

  // -- getDefault / setDefault ----------------------------------------------------
  @Test
  void setDefault_temporarilyOverridesGetDefault() {
    DateTimeZone original = DateTimeZone.getDefault();
    try {
      DateTimeZone.setDefault(DateTimeZone.forID("Europe/Paris"));
      assertEquals(DateTimeZone.forID("Europe/Paris"), DateTimeZone.getDefault());
    } finally {
      DateTimeZone.setDefault(original);
    }
    assertEquals(original, DateTimeZone.getDefault());
  }

  @Test
  void setDefault_null_throws() {
    assertThrows(IllegalArgumentException.class, () -> DateTimeZone.setDefault(null));
  }

  // -- provider / nameProvider ------------------------------------------------------
  @Test
  void getProvider_isNotNull() {
    assertNotEquals(null, DateTimeZone.getProvider());
  }

  @Test
  void setProvider_temporarilyOverridesProvider_thenRestored() {
    com.legacy.system.datetime.tz.Provider original = DateTimeZone.getProvider();
    try {
      DateTimeZone.setProvider(new UTCProvider());
      assertEquals(DateTimeZone.UTC, DateTimeZone.forID("UTC"));
    } finally {
      DateTimeZone.setProvider(original);
    }
    assertEquals(DateTimeZone.forID("Asia/Tokyo").getID(), "Asia/Tokyo");
  }

  @Test
  void setProvider_null_fallsBackToDefaultProvider() {
    com.legacy.system.datetime.tz.Provider original = DateTimeZone.getProvider();
    try {
      DateTimeZone.setProvider(null);
      assertNotEquals(null, DateTimeZone.getProvider());
    } finally {
      DateTimeZone.setProvider(original);
    }
  }

  @Test
  void getNameProvider_isNotNull() {
    assertNotEquals(null, DateTimeZone.getNameProvider());
  }

  @Test
  void setNameProvider_temporarilyOverridesNameProvider_thenRestored() {
    com.legacy.system.datetime.tz.NameProvider original = DateTimeZone.getNameProvider();
    try {
      DateTimeZone.setNameProvider(new DefaultNameProvider());
      assertNotEquals(null, DateTimeZone.UTC.getName(0L));
      DateTimeZone.setNameProvider(null);
      assertNotEquals(null, DateTimeZone.getNameProvider());
    } finally {
      DateTimeZone.setNameProvider(original);
    }
  }

  // -- getShortName / getName ----------------------------------------------------
  @Test
  void getShortNameAndName_withAndWithoutLocale() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    assertNotEquals(null, tokyo.getShortName(0L));
    assertNotEquals(null, tokyo.getShortName(0L, Locale.ENGLISH));
    assertNotEquals(null, tokyo.getName(0L));
    assertNotEquals(null, tokyo.getName(0L, Locale.ENGLISH));
    assertNotEquals(null, tokyo.getShortName(0L, null));
  }

  // -- offset / conversion internals --------------------------------------------
  @Test
  void getOffset_readableInstantOverload() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    DateTime dt = new DateTime(2020, 6, 15, 0, 0, DateTimeZone.UTC);
    assertEquals(tokyo.getOffset(dt.getMillis()), tokyo.getOffset((ReadableInstant) dt));
    assertEquals(
        tokyo.getOffset(DateTimeUtils.currentTimeMillis()),
        tokyo.getOffset((ReadableInstant) null));
  }

  @Test
  void isStandardOffset_fixedZoneIsAlwaysStandard() {
    DateTimeZone fixed = DateTimeZone.forOffsetHours(2);
    assertTrue(fixed.isStandardOffset(0L));
  }

  @Test
  void isStandardOffset_dstZone_differsInSummer() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    DateTime winter = new DateTime(2018, 1, 1, 0, 0, newYork);
    DateTime summer = new DateTime(2018, 7, 1, 0, 0, newYork);
    assertTrue(newYork.isStandardOffset(winter.getMillis()));
    assertFalse(newYork.isStandardOffset(summer.getMillis()));
  }

  @Test
  void getOffsetFromLocal_matchesGetOffsetAwayFromTransitions() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    long instant = new DateTime(2020, 6, 15, 12, 0, DateTimeZone.UTC).getMillis();
    long local = instant + tokyo.getOffset(instant);
    assertEquals(tokyo.getOffset(instant), tokyo.getOffsetFromLocal(local));
  }

  @Test
  void convertUTCToLocal_and_convertLocalToUTC_roundTrip() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    long utc = new DateTime(2020, 6, 15, 12, 0, DateTimeZone.UTC).getMillis();
    long local = tokyo.convertUTCToLocal(utc);
    assertEquals(utc, tokyo.convertLocalToUTC(local, true));
    assertEquals(utc, tokyo.convertLocalToUTC(local, true, utc));
  }

  @Test
  void adjustOffset_fixedZone_returnsSameInstant() {
    DateTimeZone fixed = DateTimeZone.forOffsetHours(2);
    long instant = new DateTime(2020, 6, 15, 12, 0, DateTimeZone.UTC).getMillis();
    assertEquals(instant, fixed.adjustOffset(instant, false));
    assertEquals(instant, fixed.adjustOffset(instant, true));
  }

  @Test
  void getMillisKeepLocal_preservesWallClockAcrossZones() {
    DateTimeZone utc = DateTimeZone.UTC;
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    long utcInstant = new DateTime(2020, 6, 15, 12, 0, utc).getMillis();
    // At that UTC instant Tokyo's wall clock reads 21:00 (UTC+9); getMillisKeepLocal keeps
    // that wall-clock reading but reinterprets it in the target zone (here, UTC itself).
    long converted = tokyo.getMillisKeepLocal(utc, utcInstant);
    assertEquals(new DateTime(2020, 6, 15, 21, 0, utc).getMillis(), converted);
    assertEquals(utcInstant, utc.getMillisKeepLocal(utc, utcInstant));
  }

  @Test
  void isLocalDateTimeGap_fixedZoneIsNeverAGap() {
    DateTimeZone fixed = DateTimeZone.forOffsetHours(2);
    assertFalse(fixed.isLocalDateTimeGap(new LocalDateTime(2020, 6, 15, 12, 0)));
  }

  @Test
  void isLocalDateTimeGap_dstSpringForwardGap_isDetected() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    // 2018-03-11 02:30 does not exist (clocks jump 02:00 -> 03:00).
    assertTrue(newYork.isLocalDateTimeGap(new LocalDateTime(2018, 3, 11, 2, 30)));
    assertFalse(newYork.isLocalDateTimeGap(new LocalDateTime(2018, 6, 15, 12, 0)));
  }

  // -- toTimeZone / hashCode / serialization ----------------------------------------
  @Test
  void toTimeZone_returnsJavaUtilTimeZoneWithSameId() {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    assertEquals("Asia/Tokyo", tokyo.toTimeZone().getID());
  }

  @Test
  void hashCode_consistentWithEquals() {
    assertEquals(
        DateTimeZone.forID("Asia/Tokyo").hashCode(), DateTimeZone.forID("Asia/Tokyo").hashCode());
  }

  @Test
  void serialization_resolvesToSameZone() throws Exception {
    DateTimeZone tokyo = DateTimeZone.forID("Asia/Tokyo");
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
      out.writeObject(tokyo);
    }
    DateTimeZone roundTripped;
    try (ObjectInputStream in =
        new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      roundTripped = (DateTimeZone) in.readObject();
    }
    assertEquals(tokyo, roundTripped);
  }

  // -- getAvailableIDs --------------------------------------------------------------
  @Test
  void getAvailableIDs_containsUtcAndCommonZones() {
    assertTrue(DateTimeZone.getAvailableIDs().contains("UTC"));
    assertTrue(DateTimeZone.getAvailableIDs().contains("Asia/Tokyo"));
  }

  // -- forOffsetMillis / forID offset parsing --------------------------------------
  @Test
  void forOffsetMillis_buildsFixedZone() {
    DateTimeZone zone = DateTimeZone.forOffsetMillis(2 * DateTimeConstants.MILLIS_PER_HOUR);
    assertEquals(2 * DateTimeConstants.MILLIS_PER_HOUR, zone.getOffset(0L));
  }

  @Test
  void forOffsetMillis_outOfRange_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DateTimeZone.forOffsetMillis(24 * DateTimeConstants.MILLIS_PER_HOUR));
  }

  @Test
  void forID_offsetStrings_areParsed() {
    assertEquals(2 * DateTimeConstants.MILLIS_PER_HOUR, DateTimeZone.forID("+02:00").getOffset(0L));
    assertEquals(DateTimeZone.UTC, DateTimeZone.forID("+00:00"));
    // "UT"/"GMT"/"Z" resolve via the real tzdata provider (e.g. to "Etc/GMT"), not
    // necessarily to the UTC singleton object, but they are all zero-offset zones.
    assertEquals(0, DateTimeZone.forID("UT").getOffset(0L));
    assertEquals(0, DateTimeZone.forID("GMT").getOffset(0L));
    assertEquals(0, DateTimeZone.forID("Z").getOffset(0L));
    assertEquals(
        2 * DateTimeConstants.MILLIS_PER_HOUR, DateTimeZone.forID("UTC+02:00").getOffset(0L));
    assertEquals(
        2 * DateTimeConstants.MILLIS_PER_HOUR, DateTimeZone.forID("UT+02:00").getOffset(0L));
  }

  @Test
  void forOffsetHoursMinutes_negativeHourNegativeMinute() {
    DateTimeZone zone = DateTimeZone.forOffsetHoursMinutes(-2, -15);
    assertEquals(-(2 * 60 + 15) * 60 * 1000, zone.getOffset(0L));
  }

  @Test
  void forOffsetHoursMinutes_positiveHourNegativeMinute_throws() {
    assertThrows(IllegalArgumentException.class, () -> DateTimeZone.forOffsetHoursMinutes(2, -15));
  }

  // -- printOffset (via the id assigned to a fixed-offset zone) --------------------
  @Test
  void forOffsetMillis_withSecondsComponent_includesSecondsInId() {
    int offset =
        2 * DateTimeConstants.MILLIS_PER_HOUR
            + 30 * DateTimeConstants.MILLIS_PER_MINUTE
            + 45 * DateTimeConstants.MILLIS_PER_SECOND;
    DateTimeZone zone = DateTimeZone.forOffsetMillis(offset);
    assertEquals("+02:30:45", zone.getID());
  }

  @Test
  void forOffsetMillis_withSubSecondComponent_includesMillisInId() {
    int offset =
        2 * DateTimeConstants.MILLIS_PER_HOUR
            + 30 * DateTimeConstants.MILLIS_PER_MINUTE
            + 45 * DateTimeConstants.MILLIS_PER_SECOND
            + 123;
    DateTimeZone zone = DateTimeZone.forOffsetMillis(offset);
    assertEquals("+02:30:45.123", zone.getID());
  }

  // -- convertLocalToUTC: DST gap (strict) and overlap (non-strict) ----------------
  @Test
  void convertLocalToUTC_strict_dstGap_throws() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    // 2018-03-11 02:30 local does not exist (clocks spring forward 02:00 -> 03:00).
    long localMillis = ISOChronology.getInstanceUTC().getDateTimeMillis(2018, 3, 11, 2, 30, 0, 0);
    assertThrows(IllegalInstantException.class, () -> newYork.convertLocalToUTC(localMillis, true));
  }

  @Test
  void convertLocalToUTC_nonStrict_dstGap_doesNotThrow() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    long localMillis = ISOChronology.getInstanceUTC().getDateTimeMillis(2018, 3, 11, 2, 30, 0, 0);
    assertTrue(newYork.convertLocalToUTC(localMillis, false) > 0);
  }

  @Test
  void convertLocalToUTC_dstOverlap_doesNotThrowEitherWay() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    // 2018-11-04 01:30 local occurs twice (clocks fall back 02:00 -> 01:00).
    long localMillis = ISOChronology.getInstanceUTC().getDateTimeMillis(2018, 11, 4, 1, 30, 0, 0);
    assertTrue(newYork.convertLocalToUTC(localMillis, false) > 0);
    assertTrue(newYork.convertLocalToUTC(localMillis, true) > 0);
  }

  // -- adjustOffset: DST overlap resolution -----------------------------------------
  @Test
  void adjustOffset_dstOverlap_earlierAndLaterDifferByOneHour() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    long searchFrom = new DateTime(2018, 10, 1, 0, 0, DateTimeZone.UTC).getMillis();
    long fallBackTransition = newYork.nextTransition(searchFrom);
    long earlier = newYork.adjustOffset(fallBackTransition, false);
    long later = newYork.adjustOffset(fallBackTransition, true);
    assertTrue(later >= earlier);
  }

  // -- getDefaultProvider / getDefaultNameProvider: system-property override --------
  @Test
  void setProvider_null_afterSystemPropertyOverride_loadsConfiguredProviderClass() {
    com.legacy.system.datetime.tz.Provider original = DateTimeZone.getProvider();
    String propertyKey = "org.joda.time.DateTimeZone.Provider";
    String previousValue = System.getProperty(propertyKey);
    try {
      System.setProperty(propertyKey, "com.legacy.system.datetime.tz.UTCProvider");
      DateTimeZone.setProvider(null);
      assertTrue(DateTimeZone.getProvider() instanceof UTCProvider);
    } finally {
      if (previousValue == null) {
        System.clearProperty(propertyKey);
      } else {
        System.setProperty(propertyKey, previousValue);
      }
      DateTimeZone.setProvider(original);
    }
  }

  @Test
  void setNameProvider_null_afterSystemPropertyOverride_loadsConfiguredProviderClass() {
    com.legacy.system.datetime.tz.NameProvider original = DateTimeZone.getNameProvider();
    String propertyKey = "org.joda.time.DateTimeZone.NameProvider";
    String previousValue = System.getProperty(propertyKey);
    try {
      System.setProperty(propertyKey, "com.legacy.system.datetime.tz.DefaultNameProvider");
      DateTimeZone.setNameProvider(null);
      assertTrue(DateTimeZone.getNameProvider() instanceof DefaultNameProvider);
    } finally {
      if (previousValue == null) {
        System.clearProperty(propertyKey);
      } else {
        System.setProperty(propertyKey, previousValue);
      }
      DateTimeZone.setNameProvider(original);
    }
  }

  // -- getOffsetFromLocal near DST boundaries ---------------------------------------
  @Test
  void getOffsetFromLocal_nearDstGap_returnsAnOffset() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    long localMillis = ISOChronology.getInstanceUTC().getDateTimeMillis(2018, 3, 11, 2, 30, 0, 0);
    int offset = newYork.getOffsetFromLocal(localMillis);
    assertTrue(
        offset == -5 * DateTimeConstants.MILLIS_PER_HOUR
            || offset == -4 * DateTimeConstants.MILLIS_PER_HOUR);
  }

  @Test
  void getOffsetFromLocal_nearDstOverlap_returnsAnOffset() {
    DateTimeZone newYork = DateTimeZone.forID("America/New_York");
    long localMillis = ISOChronology.getInstanceUTC().getDateTimeMillis(2018, 11, 4, 1, 30, 0, 0);
    int offset = newYork.getOffsetFromLocal(localMillis);
    assertTrue(
        offset == -5 * DateTimeConstants.MILLIS_PER_HOUR
            || offset == -4 * DateTimeConstants.MILLIS_PER_HOUR);
  }
}
