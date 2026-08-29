/*
 *  Copyright 2001-2014 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.legacy.system.datetime.chrono;

import com.legacy.system.datetime.Chronology;
import com.legacy.system.datetime.DateTime;
import com.legacy.system.datetime.DateTimeConstants;
import com.legacy.system.datetime.DateTimeField;
import com.legacy.system.datetime.DateTimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the Coptic calendar system, which defines every fourth year as leap, much like the
 * Julian calendar. The year is broken down into 12 months, each 30 days in length. An extra period
 * at the end of the year is either 5 or 6 days in length. In this implementation, it is considered
 * a 13th month.
 *
 * <p>Year 1 in the Coptic calendar began on August 29, 284 CE (Julian), thus Coptic years do not
 * begin at the same time as Julian years. This chronology is not proleptic, as it does not allow
 * dates before the first Coptic year.
 *
 * <p>This implementation defines a day as midnight to midnight exactly as per the ISO chronology.
 * Some references indicate that a coptic day starts at sunset on the previous ISO day, but this has
 * not been confirmed and is not implemented.
 *
 * <p>CopticChronology is thread-safe and immutable.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Coptic_calendar">Wikipedia</a>
 * @see JulianChronology
 * @author Brian S O'Neill
 * @since 1.0
 */
public final class CopticChronology extends BasicFixedMonthChronology {

  /** Serialization lock */
  private static final long serialVersionUID = -5972804258688333942L;

  /**
   * Constant value for 'Anno Martyrum' or 'Era of the Martyrs', equivalent to the value returned
   * for AD/CE.
   */
  public static final int AM = DateTimeConstants.CE;

  /** A singleton era field. */
  private static final DateTimeField ERA_FIELD = new BasicSingleEraDateTimeField("AM");

  /** The lowest year that can be fully supported. */
  private static final int MIN_YEAR = -292269337;

  /** The highest year that can be fully supported. */
  private static final int MAX_YEAR = 292272708;

  /**
   * Year 1687 is the closest Coptic year, relative to which 1970-01-01 (Gregorian), the Java epoch,
   * falls 1686-04-23 (Coptic).
   */
  private static final int EPOCH_YEAR = 1687;

  /** Cache of zone to chronology arrays */
  private static final ConcurrentHashMap<DateTimeZone, CopticChronology[]> cCache =
      new ConcurrentHashMap<DateTimeZone, CopticChronology[]>();

  /** Singleton instance of a UTC CopticChronology */
  private static final CopticChronology INSTANCE_UTC;

  static {
    // init after static fields
    INSTANCE_UTC = getInstance(DateTimeZone.UTC);
  }

  // -----------------------------------------------------------------------
  /**
   * Gets an instance of the CopticChronology. The time zone of the returned instance is UTC.
   *
   * @return a singleton UTC instance of the chronology
   */
  public static CopticChronology getInstanceUTC() {
    return INSTANCE_UTC;
  }

  /**
   * Gets an instance of the CopticChronology in the default time zone.
   *
   * @return a chronology in the default time zone
   */
  public static CopticChronology getInstance() {
    return getInstance(DateTimeZone.getDefault(), 4);
  }

  /**
   * Gets an instance of the CopticChronology in the given time zone.
   *
   * @param zone the time zone to get the chronology in, null is default
   * @return a chronology in the specified time zone
   */
  public static CopticChronology getInstance(DateTimeZone zone) {
    return getInstance(zone, 4);
  }

  /**
   * Gets an instance of the CopticChronology in the given time zone.
   *
   * @param zone the time zone to get the chronology in, null is default
   * @param minDaysInFirstWeek minimum number of days in first week of the year; default is 4
   * @return a chronology in the specified time zone
   */
  public static CopticChronology getInstance(DateTimeZone zone, int minDaysInFirstWeek) {
    if (zone == null) {
      zone = DateTimeZone.getDefault();
    }
    CopticChronology chrono;
    CopticChronology[] chronos = cCache.get(zone);
    if (chronos == null) {
      chronos = new CopticChronology[7];
      // CPD-OFF: near-identical assemble()/field-setup code across distinct concrete
      // Chronology implementations (different calendar systems, or wrapper Chronologies
      // like Limit/Zoned/Lenient/Strict). This codebase deliberately keeps each calendar
      // system as its own type (see BasicChronology.equals()'s getClass() check: two
      // different chronologies must never be considered equal), so merging this setup
      // code risks blurring that boundary or hard-coding one calendar's constants into
      // a shared path used by another.
      CopticChronology[] oldChronos = cCache.putIfAbsent(zone, chronos);
      if (oldChronos != null) {
        chronos = oldChronos;
      }
    }
    try {
      chrono = chronos[minDaysInFirstWeek - 1];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Invalid min days in first week: " + minDaysInFirstWeek);
    }
    if (chrono == null) {
      synchronized (chronos) {
        chrono = chronos[minDaysInFirstWeek - 1];
        if (chrono == null) {
          if (zone.equals(DateTimeZone.UTC)) {
            // First create without a lower limit.
            chrono =
                new CopticChronology(
                    null, null, minDaysInFirstWeek, MIN_YEAR, MAX_YEAR, EPOCH_YEAR, ERA_FIELD);
            // CPD-ON
            // Impose lower limit and make another CopticChronology.
            var lowerLimit = new DateTime(1, 1, 1, 0, 0, 0, 0, chrono);
            chrono =
                new CopticChronology(
                    LimitChronology.getInstance(chrono, lowerLimit, null),
                    null,
                    minDaysInFirstWeek,
                    MIN_YEAR,
                    MAX_YEAR,
                    EPOCH_YEAR,
                    ERA_FIELD);
          } else {
            chrono = getInstance(DateTimeZone.UTC, minDaysInFirstWeek);
            chrono =
                new CopticChronology(
                    ZonedChronology.getInstance(chrono, zone),
                    null,
                    minDaysInFirstWeek,
                    MIN_YEAR,
                    MAX_YEAR,
                    EPOCH_YEAR,
                    ERA_FIELD);
          }
          chronos[minDaysInFirstWeek - 1] = chrono;
        }
      }
    }
    return chrono;
  }

  // Constructors and instance variables
  // -----------------------------------------------------------------------
  /** Restricted constructor. */
  CopticChronology(
      Chronology base,
      Object param,
      int minDaysInFirstWeek,
      int minYear,
      int maxYear,
      int epochYear,
      DateTimeField eraField) {
    super(base, param, minDaysInFirstWeek, minYear, maxYear, epochYear, eraField);
  }

  /** Serialization singleton. */
  private Object readResolve() {
    return resolveByZoneAndMinDays(CopticChronology::getInstance);
  }

  // Conversion
  // -----------------------------------------------------------------------
  @Override
  protected Chronology getCachedInstanceUTC() {
    return INSTANCE_UTC;
  }

  @Override
  protected Chronology getCachedInstance(DateTimeZone zone) {
    return getInstance(zone);
  }
}
