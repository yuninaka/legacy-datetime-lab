package com.legacy.system.datetime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import com.legacy.system.datetime.chrono.ISOChronology;

import org.junit.jupiter.api.Test;

class MutableDateTimeTest {

    @Test
    void setMillis_mutatesInPlace() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.setMillis(before + DateTimeConstants.MILLIS_PER_DAY);
        assertEquals(2, mdt.getDayOfMonth());
    }

    @Test
    void addDays_mutatesSameInstance() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 31, 0, 0, 0, 0, DateTimeZone.UTC);
        mdt.addDays(1);
        assertEquals(2, mdt.getMonthOfYear());
        assertEquals(1, mdt.getDayOfMonth());
    }

    @Test
    void setYear_clampsLeapDayOnNonLeapTarget() {
        MutableDateTime mdt = new MutableDateTime(2000, 2, 29, 0, 0, 0, 0, DateTimeZone.UTC);
        mdt.setYear(2001);
        assertEquals(2, mdt.getMonthOfYear());
        assertEquals(28, mdt.getDayOfMonth());
    }

    @Test
    void copy_isIndependentInstance() {
        MutableDateTime original = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        MutableDateTime copy = original.copy();
        copy.addDays(1);
        assertEquals(1, original.getDayOfMonth());
        assertEquals(2, copy.getDayOfMonth());
        assertNotSame(original, copy);
    }

    @Test
    void toDateTime_createsImmutableSnapshot() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        DateTime snapshot = mdt.toDateTime();
        mdt.addDays(5);
        assertEquals(1, snapshot.getDayOfMonth());
        assertEquals(6, mdt.getDayOfMonth());
    }

    @Test
    void setDayOfMonth_outOfRange_throws() {
        MutableDateTime mdt = new MutableDateTime(2021, 2, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        assertThrows(IllegalFieldValueException.class, () -> mdt.setDayOfMonth(29));
    }

    // -- constructors ---------------------------------------------------------
    @Test
    void noArgConstructors_areCloseToNow() {
        MutableDateTime now = new MutableDateTime();
        MutableDateTime now2 = new MutableDateTime(DateTimeZone.UTC);
        MutableDateTime now3 = new MutableDateTime((Chronology) ISOChronology.getInstance());
        assertTrue(Math.abs(now.getYear() - now2.getYear()) <= 1);
        assertEquals(now.getChronology(), now3.getChronology());
    }

    @Test
    void millisConstructors() {
        assertEquals(1970, new MutableDateTime(0L).getYear());
        assertEquals(1970, new MutableDateTime(0L, DateTimeZone.UTC).getYear());
        assertEquals(1970, new MutableDateTime(0L, (Chronology) ISOChronology.getInstanceUTC()).getYear());
    }

    @Test
    void objectConstructors_parseIsoStrings() {
        DateTime expected = new DateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        assertEquals(expected.getMillis(), new MutableDateTime((Object) "2020-06-15T10:30:00.000Z").getMillis());
        assertEquals(expected.getMillis(), new MutableDateTime((Object) "2020-06-15T10:30:00.000Z", DateTimeZone.UTC).getMillis());
        assertEquals(expected.getMillis(),
                new MutableDateTime((Object) "2020-06-15T10:30:00.000Z", (Chronology) ISOChronology.getInstanceUTC()).getMillis());
    }

    @Test
    void chronologyConstructor_defaultsToIso() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, (Chronology) ISOChronology.getInstanceUTC());
        assertEquals(ISOChronology.getInstanceUTC(), mdt.getChronology());
    }

    @Test
    void now_withZoneOrChronology() {
        MutableDateTime now = new MutableDateTime();
        assertTrue(Math.abs(MutableDateTime.now(DateTimeZone.UTC).getYear() - now.getYear()) <= 1);
        assertTrue(Math.abs(MutableDateTime.now((Chronology) ISOChronology.getInstance()).getYear() - now.getYear()) <= 1);
    }

    @Test
    void parse_withAndWithoutFormatter() {
        MutableDateTime a = MutableDateTime.parse("2020-06-15T10:30:00.000Z");
        MutableDateTime b = MutableDateTime.parse("2020-06-15",
                com.legacy.system.datetime.format.ISODateTimeFormat.date().withZoneUTC());
        assertEquals(15, a.getDayOfMonth());
        assertEquals(15, b.getDayOfMonth());
    }

    // -- rounding ---------------------------------------------------------------
    @Test
    void setRounding_singleArg_defaultsToFloor() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 500, DateTimeZone.UTC);
        mdt.setRounding(mdt.getChronology().secondOfMinute());
        assertEquals(MutableDateTime.ROUND_FLOOR, mdt.getRoundingMode());
        assertEquals(45, mdt.getSecondOfMinute());
        assertEquals(0, mdt.getMillisOfSecond());
        mdt.setRounding(null);
        assertEquals(null, mdt.getRoundingField());
    }

    @Test
    void setRounding_withMode_roundsImmediately() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 500, DateTimeZone.UTC);
        mdt.setRounding(mdt.getChronology().secondOfMinute(), MutableDateTime.ROUND_CEILING);
        assertEquals(MutableDateTime.ROUND_CEILING, mdt.getRoundingMode());
        assertEquals(46, mdt.getSecondOfMinute());
    }

    // -- add(duration/period, scalar) --------------------------------------------
    @Test
    void add_durationAndPeriod_bothOverloads() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 0, 0, 0, DateTimeZone.UTC);
        mdt.add(Duration.standardHours(1));
        assertEquals(11, mdt.getHourOfDay());
        mdt.add(Duration.standardHours(1), -1);
        assertEquals(10, mdt.getHourOfDay());
        mdt.add(Period.days(1));
        assertEquals(16, mdt.getDayOfMonth());
        mdt.add(Period.days(1), -1);
        assertEquals(15, mdt.getDayOfMonth());
        mdt.add(3600000L);
        assertEquals(11, mdt.getHourOfDay());
    }

    // -- setChronology / setZone / setZoneRetainFields -----------------------------
    @Test
    void setChronology_changesChronologyKeepsMillis() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.setChronology(com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC());
        assertEquals(before, mdt.getMillis());
        assertEquals(com.legacy.system.datetime.chrono.GregorianChronology.getInstanceUTC(), mdt.getChronology());
    }

    @Test
    void setZone_keepsInstantFixed() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 12, 0, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.setZone(DateTimeZone.forID("Asia/Tokyo"));
        assertEquals(before, mdt.getMillis());
        assertEquals(21, mdt.getHourOfDay());
    }

    @Test
    void setZoneRetainFields_keepsLocalFields() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 12, 0, 0, 0, DateTimeZone.UTC);
        mdt.setZoneRetainFields(DateTimeZone.forID("Asia/Tokyo"));
        assertEquals(12, mdt.getHourOfDay());
    }

    // -- set / add by DateTimeFieldType / DurationFieldType -------------------------
    @Test
    void set_and_add_byFieldType() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.set(DateTimeFieldType.dayOfMonth(), 20);
        assertEquals(20, mdt.getDayOfMonth());
        mdt.add(DurationFieldType.days(), 2);
        assertEquals(22, mdt.getDayOfMonth());
    }

    // -- add/set for every field --------------------------------------------------
    @Test
    void addAndSet_everyField() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 30, 500, DateTimeZone.UTC);
        mdt.addYears(1);
        assertEquals(2021, mdt.getYear());
        mdt.addWeekyears(1);
        assertEquals(2022, mdt.getWeekyear());
        mdt.addMonths(1);
        assertEquals(7, mdt.getMonthOfYear());
        mdt.addWeeks(1);
        mdt.addHours(1);
        mdt.addMinutes(1);
        mdt.addSeconds(1);
        mdt.addMillis(1);

        mdt.setWeekyear(2020);
        assertEquals(2020, mdt.getWeekyear());
        mdt.setWeekOfWeekyear(2);
        assertEquals(2, mdt.getWeekOfWeekyear());
        mdt.setSecondOfMinute(5);
        assertEquals(5, mdt.getSecondOfMinute());
        mdt.setSecondOfDay(100);
        assertEquals(100, mdt.getSecondOfDay());
        mdt.setMonthOfYear(3);
        assertEquals(3, mdt.getMonthOfYear());
        mdt.setMinuteOfHour(5);
        assertEquals(5, mdt.getMinuteOfHour());
        mdt.setMinuteOfDay(100);
        assertEquals(100, mdt.getMinuteOfDay());
        mdt.setMillisOfSecond(5);
        assertEquals(5, mdt.getMillisOfSecond());
        mdt.setMillisOfDay(100);
        assertEquals(100, mdt.getMillisOfDay());
        mdt.setHourOfDay(5);
        assertEquals(5, mdt.getHourOfDay());
        mdt.setDayOfYear(10);
        assertEquals(10, mdt.getDayOfYear());
        mdt.setDayOfWeek(DateTimeConstants.MONDAY);
        assertEquals(DateTimeConstants.MONDAY, mdt.getDayOfWeek());
    }

    // -- setDate / setTime / setDateTime overloads ----------------------------------
    @Test
    void setDate_allOverloads() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.setDate(1999, 3, 3);
        assertEquals(1999, mdt.getYear());
        assertEquals(10, mdt.getHourOfDay());

        MutableDateTime other = new MutableDateTime(2005, 7, 7, 1, 1, 0, 0, DateTimeZone.UTC);
        mdt.setDate(other.getMillis());
        assertEquals(2005, mdt.getYear());
        assertEquals(10, mdt.getHourOfDay());

        mdt.setDate((ReadableInstant) other);
        assertEquals(2005, mdt.getYear());
        assertEquals(10, mdt.getHourOfDay());
    }

    @Test
    void setTime_allOverloads() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.setTime(1, 2, 3, 4);
        assertEquals(1, mdt.getHourOfDay());
        assertEquals(2020, mdt.getYear());

        MutableDateTime other = new MutableDateTime(2005, 7, 7, 5, 6, 0, 0, DateTimeZone.UTC);
        mdt.setTime(other.getMillis());
        assertEquals(5, mdt.getHourOfDay());
        assertEquals(2020, mdt.getYear());

        mdt.setTime((ReadableInstant) other);
        assertEquals(5, mdt.getHourOfDay());
        assertEquals(2020, mdt.getYear());
    }

    @Test
    void setDateTime_allSevenFields() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.setDateTime(1999, 3, 3, 1, 2, 3, 4);
        assertEquals(1999, mdt.getYear());
        assertEquals(3, mdt.getMonthOfYear());
        assertEquals(1, mdt.getHourOfDay());
        assertEquals(4, mdt.getMillisOfSecond());
    }

    // -- clone --------------------------------------------------------------------
    @Test
    void clone_isIndependentInstance() {
        MutableDateTime original = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        MutableDateTime clone = (MutableDateTime) original.clone();
        clone.addDays(1);
        assertEquals(1, original.getDayOfMonth());
        assertEquals(2, clone.getDayOfMonth());
    }

    // -- property accessors ----------------------------------------------------------
    @Test
    void propertyAccessors_returnPropertiesForEveryField() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
        assertEquals(2020, mdt.year().get());
        assertEquals(20, mdt.yearOfCentury().get());
        assertEquals(2020, mdt.yearOfEra().get());
        assertEquals(2020, mdt.weekyear().get());
        assertEquals(6, mdt.monthOfYear().get());
        assertEquals(25, mdt.weekOfWeekyear().get());
        assertEquals(15, mdt.dayOfMonth().get());
        assertEquals(1, mdt.dayOfWeek().get());
        assertEquals(167, mdt.dayOfYear().get());
        assertEquals(1, mdt.era().get());
        assertEquals(20, mdt.centuryOfEra().get());
        assertEquals(10, mdt.hourOfDay().get());
        assertEquals(30, mdt.minuteOfHour().get());
        assertEquals(45, mdt.secondOfMinute().get());
        assertEquals(123, mdt.millisOfSecond().get());
        assertTrue(mdt.millisOfDay().get() > 0);
        assertTrue(mdt.secondOfDay().get() > 0);
        assertTrue(mdt.minuteOfDay().get() > 0);
    }

    @Test
    void property_byFieldType_matchesDirectAccessor() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        assertEquals(mdt.dayOfMonth().get(), mdt.property(DateTimeFieldType.dayOfMonth()).get());
    }

    // -- MutableDateTime.Property (mutates in place, unlike DateTime.Property) --------
    @Test
    void property_addAndSet_mutatesOriginal() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 123, DateTimeZone.UTC);
        MutableDateTime.Property secondProperty = mdt.secondOfMinute();

        assertEquals(mdt.getMillis(), secondProperty.getMillis());
        assertEquals(mdt, secondProperty.getMutableDateTime());
        assertEquals(mdt.getChronology(), secondProperty.getChronology());
        assertEquals(mdt.getChronology().secondOfMinute().getType(), secondProperty.getField().getType());

        assertSame(mdt, secondProperty.add(5));
        assertEquals(50, mdt.getSecondOfMinute());
        assertSame(mdt, secondProperty.add(5L));
        assertEquals(55, mdt.getSecondOfMinute());
        assertSame(mdt, secondProperty.addWrapField(10));
        assertEquals(5, mdt.getSecondOfMinute());
        assertSame(mdt, secondProperty.set(45));
        assertEquals(45, mdt.getSecondOfMinute());
    }

    @Test
    void property_setByText() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.monthOfYear().set("December");
        assertEquals(12, mdt.getMonthOfYear());
        mdt.monthOfYear().set("March", Locale.ENGLISH);
        assertEquals(3, mdt.getMonthOfYear());
    }

    @Test
    void property_roundVariants_mutateInPlace() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 500, DateTimeZone.UTC);
        mdt.secondOfMinute().roundFloor();
        assertEquals(45, mdt.getSecondOfMinute());
        assertEquals(0, mdt.getMillisOfSecond());

        mdt.setMillisOfSecond(500);
        mdt.secondOfMinute().roundCeiling();
        assertEquals(46, mdt.getSecondOfMinute());

        mdt.setSecondOfMinute(45);
        mdt.setMillisOfSecond(500);
        mdt.secondOfMinute().roundHalfFloor();
        assertEquals(45, mdt.getSecondOfMinute());

        mdt.setSecondOfMinute(45);
        mdt.setMillisOfSecond(500);
        mdt.secondOfMinute().roundHalfCeiling();
        assertEquals(46, mdt.getSecondOfMinute());

        mdt.setSecondOfMinute(45);
        mdt.setMillisOfSecond(500);
        mdt.secondOfMinute().roundHalfEven();
        assertEquals(46, mdt.getSecondOfMinute());
    }

    @Test
    void property_serializationRoundTrip_preservesValue() throws Exception {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        MutableDateTime.Property original = mdt.dayOfMonth();

        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        MutableDateTime.Property roundTripped;
        try (java.io.ObjectInputStream in =
                new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes.toByteArray()))) {
            roundTripped = (MutableDateTime.Property) in.readObject();
        }
        assertEquals(original.get(), roundTripped.get());
    }

    @Test
    void setRounding_everyMode_roundsOnEverySubsequentSetMillis() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 500, DateTimeZone.UTC);
        DateTimeField second = mdt.getChronology().secondOfMinute();

        mdt.setRounding(second, MutableDateTime.ROUND_HALF_FLOOR);
        mdt.setMillis(mdt.getMillis());
        assertEquals(0, mdt.getMillisOfSecond());

        mdt.setRounding(second, MutableDateTime.ROUND_HALF_CEILING);
        mdt.setMillisOfSecond(500);
        mdt.setMillis(mdt.getMillis());
        assertEquals(0, mdt.getMillisOfSecond());

        mdt.setRounding(second, MutableDateTime.ROUND_HALF_EVEN);
        mdt.setMillisOfSecond(500);
        mdt.setMillis(mdt.getMillis());
        assertEquals(0, mdt.getMillisOfSecond());

        mdt.setRounding(null, MutableDateTime.ROUND_NONE);
    }

    @Test
    void setMillis_readableInstantOverload() {
        MutableDateTime mdt = new MutableDateTime(2020, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        DateTime other = new DateTime(2020, 6, 15, 0, 0, DateTimeZone.UTC);
        mdt.setMillis((ReadableInstant) other);
        assertEquals(other.getMillis(), mdt.getMillis());
    }

    @Test
    void setZoneRetainFields_sameZone_isNoOp() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.setZoneRetainFields(DateTimeZone.UTC);
        assertEquals(before, mdt.getMillis());
    }

    @Test
    void setAndAdd_byFieldType_nullField_throws() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        assertThrows(IllegalArgumentException.class, () -> mdt.set(null, 1));
        assertThrows(IllegalArgumentException.class, () -> mdt.add((DurationFieldType) null, 1));
    }

    @Test
    void add_byFieldType_zeroAmount_isNoOp() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        long before = mdt.getMillis();
        mdt.add(DurationFieldType.days(), 0);
        assertEquals(before, mdt.getMillis());
    }

    @Test
    void setDayOfMonth_directCall() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        mdt.setDayOfMonth(20);
        assertEquals(20, mdt.getDayOfMonth());
    }

    @Test
    void sevenIntConstructor_noZoneOrChronology() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 45, 500);
        assertEquals(2020, mdt.getYear());
    }

    @Test
    void property_nullField_throws() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        assertThrows(IllegalArgumentException.class, () -> mdt.property(null));
    }

    @Test
    void clone_producesEqualButDistinctInstance() {
        MutableDateTime mdt = new MutableDateTime(2020, 6, 15, 10, 30, 0, 0, DateTimeZone.UTC);
        MutableDateTime cloned = (MutableDateTime) mdt.clone();
        assertEquals(mdt, cloned);
    }

    @Test
    void now_withZoneOrChronology_nonNull() {
        assertNotEquals(null, MutableDateTime.now(DateTimeZone.UTC));
        assertNotEquals(null, MutableDateTime.now((Chronology) ISOChronology.getInstance()));
    }
}
