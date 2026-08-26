package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class LocalTimeTest {

    @Test
    void constructor_fieldValues() {
        LocalTime time = new LocalTime(10, 30, 45, 123);
        assertEquals(10, time.getHourOfDay());
        assertEquals(30, time.getMinuteOfHour());
        assertEquals(45, time.getSecondOfMinute());
        assertEquals(123, time.getMillisOfSecond());
    }

    @Test
    void constructor_hour24_throws() {
        assertThrows(IllegalFieldValueException.class, () -> new LocalTime(24, 0, 0, 0));
    }

    @Test
    void constructor_minute60_throws() {
        assertThrows(IllegalFieldValueException.class, () -> new LocalTime(0, 60, 0, 0));
    }

    @Test
    void midnight_isValid() {
        LocalTime midnight = new LocalTime(0, 0, 0, 0);
        assertEquals(0, midnight.getHourOfDay());
    }

    @Test
    void plusHours_wrapsAroundMidnightWithoutDateInformation() {
        LocalTime time = new LocalTime(23, 0, 0, 0);
        LocalTime result = time.plusHours(2);
        assertEquals(1, result.getHourOfDay());
    }

    @Test
    void equals_requiresSameFields() {
        LocalTime a = new LocalTime(10, 0, 0, 0);
        LocalTime b = new LocalTime(10, 0, 0, 0);
        LocalTime c = new LocalTime(10, 0, 0, 1);
        assertEquals(a, b);
        assertNotEquals(a, c);
    }

    @Test
    void compareTo_ordersByFieldValues() {
        LocalTime earlier = new LocalTime(9, 0, 0, 0);
        LocalTime later = new LocalTime(9, 0, 1, 0);
        assertTrue(earlier.compareTo(later) < 0);
    }

    @Test
    void toString_isHHmmssSSS() {
        LocalTime time = new LocalTime(9, 5, 3, 7);
        assertEquals("09:05:03.007", time.toString());
    }

    // -- constructors ---------------------------------------------------------
    @Test
    void noArgConstructors_areCloseToNow() {
        LocalTime now = new LocalTime();
        LocalTime now2 = new LocalTime(DateTimeZone.UTC);
        LocalTime now3 = new LocalTime((Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstance());
        assertNotEquals(null, now);
        assertNotEquals(null, now2);
        assertNotEquals(null, now3);
    }

    @Test
    void millisConstructors() {
        // No-zone overload reads the millis through the default zone, same as new DateTime(0L).
        assertEquals(new DateTime(0L).getHourOfDay(), new LocalTime(0L).getHourOfDay());
        assertEquals(0, new LocalTime(0L, DateTimeZone.UTC).getHourOfDay());
        assertEquals(0, new LocalTime(0L, (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC()).getHourOfDay());
    }

    @Test
    void threeIntConstructor() {
        LocalTime time = new LocalTime(9, 5, 3);
        assertEquals(0, time.getMillisOfSecond());
    }

    @Test
    void objectConstructors_parseIsoStrings() {
        LocalTime expected = new LocalTime(10, 30, 0, 0);
        assertEquals(expected, new LocalTime((Object) "10:30:00"));
        assertEquals(expected, new LocalTime((Object) "10:30:00", DateTimeZone.UTC));
        assertEquals(expected, new LocalTime((Object) "10:30:00", (Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC()));
    }

    @Test
    void fromMillisOfDay_bothOverloads() {
        LocalTime time = LocalTime.fromMillisOfDay(3_723_000L);
        assertEquals(1, time.getHourOfDay());
        assertEquals(2, time.getMinuteOfHour());
        assertEquals(3, time.getSecondOfMinute());
        LocalTime time2 = LocalTime.fromMillisOfDay(3_723_000L, com.legacy.system.datetime.chrono.ISOChronology.getInstanceUTC());
        assertEquals(time, time2);
    }

    @Test
    void fromCalendarFields_and_fromDateFields() {
        Calendar cal = new GregorianCalendar(2020, Calendar.JUNE, 15, 10, 30, 0);
        assertEquals(10, LocalTime.fromCalendarFields(cal).getHourOfDay());
        Date date = cal.getTime();
        assertEquals(10, LocalTime.fromDateFields(date).getHourOfDay());
    }

    @Test
    void now_withZoneOrChronology() {
        assertNotEquals(null, LocalTime.now(DateTimeZone.UTC));
        assertNotEquals(null, LocalTime.now((Chronology) com.legacy.system.datetime.chrono.ISOChronology.getInstance()));
    }

    @Test
    void parse_withAndWithoutFormatter() {
        LocalTime a = LocalTime.parse("10:30:00");
        LocalTime b = LocalTime.parse("10-30-00", com.legacy.system.datetime.format.DateTimeFormat.forPattern("HH-mm-ss"));
        assertEquals(a, b);
    }

    // -- size / get / isSupported / getValue -----------------------------------------
    @Test
    void size_and_get_and_isSupported() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(4, time.size());
        assertEquals(10, time.get(DateTimeFieldType.hourOfDay()));
        assertTrue(time.isSupported(DateTimeFieldType.hourOfDay()));
        assertFalse(time.isSupported(DateTimeFieldType.dayOfMonth()));
        assertTrue(time.isSupported(DurationFieldType.hours()));
        assertFalse(time.isSupported(DurationFieldType.days()));
        assertEquals(10, time.getValue(0));
    }

    @Test
    void equals_selfAndOtherType() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(time, time);
        assertNotEquals(time, "not a time");
        assertEquals(time.hashCode(), new LocalTime(10, 30).hashCode());
    }

    // -- withFields / withField / withFieldAdded / withPeriodAdded -----------------
    @Test
    void withFields_and_withField_and_withFieldAdded() {
        LocalTime time = new LocalTime(10, 30);
        LocalTime result = time.withFields(new LocalTime(1, 2, 3, 4));
        assertEquals(1, result.getHourOfDay());

        LocalTime fieldResult = time.withField(DateTimeFieldType.hourOfDay(), 5);
        assertEquals(5, fieldResult.getHourOfDay());

        LocalTime addedResult = time.withFieldAdded(DurationFieldType.hours(), 2);
        assertEquals(12, addedResult.getHourOfDay());
    }

    @Test
    void withPeriodAdded_and_plus_and_minus() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(11, time.withPeriodAdded(Period.hours(1), 1).getHourOfDay());
        assertEquals(11, time.plus(Period.hours(1)).getHourOfDay());
        assertEquals(9, time.minus(Period.hours(1)).getHourOfDay());
    }

    // -- plus/minus every field ------------------------------------------------------
    @Test
    void plusAndMinus_everyField() {
        LocalTime time = new LocalTime(10, 30, 30, 500);
        assertEquals(11, time.plusHours(1).getHourOfDay());
        assertEquals(31, time.plusMinutes(1).getMinuteOfHour());
        assertEquals(31, time.plusSeconds(1).getSecondOfMinute());
        assertEquals(501, time.plusMillis(1).getMillisOfSecond());

        assertEquals(9, time.minusHours(1).getHourOfDay());
        assertEquals(29, time.minusMinutes(1).getMinuteOfHour());
        assertEquals(29, time.minusSeconds(1).getSecondOfMinute());
        assertEquals(499, time.minusMillis(1).getMillisOfSecond());
    }

    // -- getters/withers for every field ----------------------------------------------
    @Test
    void gettersAndWithers_everyField() {
        LocalTime time = new LocalTime(10, 30, 45, 123);
        assertEquals(10, time.getHourOfDay());
        assertEquals(30, time.getMinuteOfHour());
        assertEquals(45, time.getSecondOfMinute());
        assertEquals(123, time.getMillisOfSecond());
        assertTrue(time.getMillisOfDay() > 0);

        assertEquals(5, time.withHourOfDay(5).getHourOfDay());
        assertEquals(5, time.withMinuteOfHour(5).getMinuteOfHour());
        assertEquals(5, time.withSecondOfMinute(5).getSecondOfMinute());
        assertEquals(5, time.withMillisOfSecond(5).getMillisOfSecond());
        assertEquals(5, time.withMillisOfDay(5).getMillisOfDay());
    }

    // -- toDateTimeToday ----------------------------------------------------------
    @Test
    void toDateTimeToday_bothOverloads() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(10, time.toDateTimeToday().getHourOfDay());
        DateTime withZone = time.toDateTimeToday(DateTimeZone.UTC);
        assertEquals(10, withZone.getHourOfDay());
        assertEquals(DateTimeZone.UTC, withZone.getZone());
    }

    // -- toString(pattern) --------------------------------------------------------
    @Test
    void toString_withPattern() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals("10-30", time.toString("HH-mm"));
        assertEquals("10-30", time.toString("HH-mm", Locale.ENGLISH));
    }

    // -- property accessors ----------------------------------------------------------
    @Test
    void propertyAccessors_returnPropertiesForEveryField() {
        LocalTime time = new LocalTime(10, 30, 45, 123);
        assertEquals(10, time.hourOfDay().get());
        assertEquals(30, time.minuteOfHour().get());
        assertEquals(45, time.secondOfMinute().get());
        assertEquals(123, time.millisOfSecond().get());
    }

    @Test
    void property_byFieldType_matchesDirectAccessor() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(time.hourOfDay().get(), time.property(DateTimeFieldType.hourOfDay()).get());
    }

    // -- LocalTime.Property -----------------------------------------------------------
    @Test
    void property_addCopy_and_setCopy_leaveOriginalUnchanged() {
        LocalTime time = new LocalTime(10, 30, 45, 123);
        LocalTime.Property secondProperty = time.secondOfMinute();

        assertEquals(time, secondProperty.getLocalTime());
        assertEquals(time.getChronology(), secondProperty.getChronology());
        assertTrue(secondProperty.getMillis() >= 0);

        assertEquals(50, secondProperty.addCopy(5).getSecondOfMinute());
        assertEquals(50, secondProperty.addCopy(5L).getSecondOfMinute());
        assertEquals(5, secondProperty.addWrapFieldToCopy(20).getSecondOfMinute());
        assertEquals(5, secondProperty.setCopy(5).getSecondOfMinute());
        assertEquals(45, time.getSecondOfMinute());
    }

    @Test
    void property_addNoWrapToCopy_throwsWhenOutOfRange() {
        LocalTime time = new LocalTime(23, 30);
        LocalTime.Property hourProperty = time.hourOfDay();
        assertEquals(23, hourProperty.addNoWrapToCopy(0).getHourOfDay());
        assertThrows(IllegalArgumentException.class, () -> hourProperty.addNoWrapToCopy(5));
    }

    @Test
    void property_setCopyByText() {
        LocalTime time = new LocalTime(10, 30);
        LocalTime.Property hourProperty = time.hourOfDay();
        assertEquals(10, hourProperty.setCopy("10").getHourOfDay());
        assertEquals(10, hourProperty.setCopy("10", Locale.ENGLISH).getHourOfDay());
    }

    @Test
    void property_roundCopyVariants() {
        LocalTime time = new LocalTime(10, 30, 45, 500);
        LocalTime.Property secondProperty = time.secondOfMinute();
        assertEquals(45, secondProperty.roundFloorCopy().getSecondOfMinute());
        assertEquals(46, secondProperty.roundCeilingCopy().getSecondOfMinute());
        assertEquals(45, secondProperty.roundHalfFloorCopy().getSecondOfMinute());
        assertEquals(46, secondProperty.roundHalfCeilingCopy().getSecondOfMinute());
        assertEquals(46, secondProperty.roundHalfEvenCopy().getSecondOfMinute());
    }

    @Test
    void property_withMinimumAndMaximumValue() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(0, time.hourOfDay().withMinimumValue().getHourOfDay());
        assertEquals(23, time.hourOfDay().withMaximumValue().getHourOfDay());
    }

    @Test
    void property_serializationRoundTrip_preservesValue() throws Exception {
        LocalTime time = new LocalTime(10, 30);
        LocalTime.Property original = time.hourOfDay();

        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        LocalTime.Property roundTripped;
        try (java.io.ObjectInputStream in =
                new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            roundTripped = (LocalTime.Property) in.readObject();
        }
        assertEquals(original.get(), roundTripped.get());
        assertEquals(original.getLocalTime(), roundTripped.getLocalTime());
    }

    // -- zero-amount / null-argument fast paths --------------------------------------
    @Test
    void zeroAndNull_fastPaths_returnSameInstance() {
        LocalTime time = new LocalTime(10, 30);
        assertSame(time, time.plusHours(0));
        assertSame(time, time.plusMinutes(0));
        assertSame(time, time.plusSeconds(0));
        assertSame(time, time.plusMillis(0));
        assertSame(time, time.minusHours(0));
        assertSame(time, time.minusMinutes(0));
        assertSame(time, time.minusSeconds(0));
        assertSame(time, time.minusMillis(0));
        assertSame(time, time.withFieldAdded(DurationFieldType.hours(), 0));
        assertSame(time, time.withFields(null));
        assertSame(time, time.withPeriodAdded(null, 1));
    }

    @Test
    void isSupported_and_property_nullOrUnsupportedFields() {
        LocalTime time = new LocalTime(10, 30);
        assertFalse(time.isSupported((DateTimeFieldType) null));
        assertFalse(time.isSupported((DurationFieldType) null));
        assertFalse(time.isSupported(DateTimeFieldType.dayOfMonth()));
        assertFalse(time.isSupported(DurationFieldType.days()));
        assertThrows(IllegalArgumentException.class, () -> time.property(null));
        assertThrows(IllegalArgumentException.class, () -> time.property(DateTimeFieldType.dayOfMonth()));
        assertThrows(IllegalArgumentException.class, () -> time.withFieldAdded(null, 1));
        assertThrows(IllegalArgumentException.class, () -> time.withFieldAdded(DurationFieldType.days(), 1));
    }

    // -- now() / compareTo / toString(pattern) / fromDateFields ------------------------
    @Test
    void now_noArg_isNotNull() {
        assertNotEquals(null, LocalTime.now());
    }

    @Test
    void compareTo_differentChronology_fallsBackToGenericComparison() {
        LocalTime iso = new LocalTime(10, 30);
        LocalTime gregorian = new LocalTime(10, 30, 0, 0,
                com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC());
        assertEquals(0, iso.compareTo(gregorian));
    }

    @Test
    void toString_nullPattern_fallsBackToDefaultToString() {
        LocalTime time = new LocalTime(10, 30);
        assertEquals(time.toString(), time.toString((String) null));
    }

    @Test
    void fromDateFields_and_fromCalendarFields_useOnlyTimeComponents() {
        java.util.Calendar cal = new java.util.GregorianCalendar(2020, java.util.Calendar.JUNE, 15, 10, 30, 45);
        assertEquals(new LocalTime(10, 30, 45), LocalTime.fromCalendarFields(cal));
        assertEquals(new LocalTime(10, 30, 45), LocalTime.fromDateFields(cal.getTime()));
    }
}
