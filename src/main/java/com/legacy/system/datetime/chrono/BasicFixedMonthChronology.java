/*
 *  Copyright 2001-2005 Stephen Colebourne
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
import com.legacy.system.datetime.DateTimeConstants;
import com.legacy.system.datetime.DateTimeField;
import com.legacy.system.datetime.field.SkipDateTimeField;

/**
 * Abstract implementation of a calendar system based around fixed length months.
 *
 * <p>As the month length is fixed various calculations can be optimised. This implementation
 * assumes any additional days after twelve months fall into a thirteenth month.
 *
 * <p>BasicFixedMonthChronology is thread-safe and immutable, and all subclasses must be as well.
 *
 * @author Brian S O'Neill
 * @author Stephen Colebourne
 * @since 1.2, refactored from CopticChronology
 */
abstract class BasicFixedMonthChronology extends BasicChronology {

  /** Serialization lock */
  private static final long serialVersionUID = 261387371998L;

  /** The length of the month. */
  static final int MONTH_LENGTH = 30;

  /** The typical millis per year. */
  static final long MILLIS_PER_YEAR = (long) (365.25 * DateTimeConstants.MILLIS_PER_DAY);

  /** The length of the month in millis. */
  static final long MILLIS_PER_MONTH = ((long) MONTH_LENGTH) * DateTimeConstants.MILLIS_PER_DAY;

  /**
   * Number of days between January 1st of {@link #iEpochYear} and this calendar system's Java epoch
   * reference date (1970-01-01 Gregorian), shared by every fixed-month calendar in this codebase
   * since each one's reference date was chosen to fall the same distance into the year.
   */
  private static final long EPOCH_REFERENCE_DAY_OF_YEAR = 112;

  private final int iMinYear;
  private final int iMaxYear;

  /**
   * The year, relative to which {@link #calculateFirstDayOfYearMillis(int)} and {@link
   * #getApproxMillisAtEpochDividedByTwo()} measure elapsed days; chosen close to this calendar's
   * Java epoch reference date.
   */
  private final int iEpochYear;

  // Not serializable (see BasicSingleEraDateTimeField); harmless to drop since every concrete
  // subclass's readResolve() discards the raw deserialized instance for a freshly-built one.
  private final transient DateTimeField iEraField;

  // -----------------------------------------------------------------------
  /**
   * Restricted constructor.
   *
   * @param base the base chronology
   * @param param the init parameter
   * @param minDaysInFirstWeek the minimum days in the first week
   * @param minYear the lowest year that can be fully supported
   * @param maxYear the highest year that can be fully supported
   * @param epochYear the year relative to which epoch millis are calculated; see {@link
   *     #iEpochYear}
   * @param eraField this calendar's singleton era field
   */
  BasicFixedMonthChronology(
      Chronology base,
      Object param,
      int minDaysInFirstWeek,
      int minYear,
      int maxYear,
      int epochYear,
      DateTimeField eraField) {
    super(base, param, minDaysInFirstWeek);
    iMinYear = minYear;
    iMaxYear = maxYear;
    iEpochYear = epochYear;
    iEraField = eraField;
  }

  // -----------------------------------------------------------------------
  @Override
  long setYear(long instant, int year) {
    // optimsed implementation of set, due to fixed months
    int thisYear = getYear(instant);
    int dayOfYear = getDayOfYear(instant, thisYear);
    int millisOfDay = getMillisOfDay(instant);

    // Current year is leap, and day is leap.
    if (dayOfYear > 365 && !isLeapYear(year)) {
      // Moving to a non-leap year, leap day doesn't exist.
      dayOfYear--;
    }

    return setYearDayMillis(year, dayOfYear, millisOfDay);
  }

  // -----------------------------------------------------------------------
  @Override
  long getTotalMillisByYearMonth(int year, int month) {
    return ((month - 1) * MILLIS_PER_MONTH);
  }

  // -----------------------------------------------------------------------
  @Override
  int getDayOfMonth(long millis) {
    // optimised for fixed months
    return (getDayOfYear(millis) - 1) % MONTH_LENGTH + 1;
  }

  // -----------------------------------------------------------------------
  @Override
  boolean isLeapYear(int year) {
    return (year & 3) == 3;
  }

  // -----------------------------------------------------------------------
  @Override
  int getDaysInYearMonth(int year, int month) {
    return (month != 13) ? MONTH_LENGTH : (isLeapYear(year) ? 6 : 5);
  }

  // -----------------------------------------------------------------------
  @Override
  int getDaysInMonthMax() {
    return MONTH_LENGTH;
  }

  // -----------------------------------------------------------------------
  @Override
  int getDaysInMonthMax(int month) {
    return (month != 13 ? MONTH_LENGTH : 6);
  }

  // -----------------------------------------------------------------------
  @Override
  int getMonthOfYear(long millis) {
    return (getDayOfYear(millis) - 1) / MONTH_LENGTH + 1;
  }

  // -----------------------------------------------------------------------
  @Override
  int getMonthOfYear(long millis, int year) {
    long monthZeroBased = (millis - getYearMillis(year)) / MILLIS_PER_MONTH;
    return ((int) monthZeroBased) + 1;
  }

  // -----------------------------------------------------------------------
  @Override
  int getMaxMonth() {
    return 13;
  }

  // -----------------------------------------------------------------------
  @Override
  long getAverageMillisPerYear() {
    return MILLIS_PER_YEAR;
  }

  // -----------------------------------------------------------------------
  @Override
  long getAverageMillisPerYearDividedByTwo() {
    return MILLIS_PER_YEAR / 2;
  }

  // -----------------------------------------------------------------------
  @Override
  long getAverageMillisPerMonth() {
    return MILLIS_PER_MONTH;
  }

  // -----------------------------------------------------------------------
  @Override
  int getMinYear() {
    return iMinYear;
  }

  // -----------------------------------------------------------------------
  @Override
  int getMaxYear() {
    return iMaxYear;
  }

  // -----------------------------------------------------------------------
  @Override
  long getApproxMillisAtEpochDividedByTwo() {
    return ((long) (iEpochYear - 1) * MILLIS_PER_YEAR
            + EPOCH_REFERENCE_DAY_OF_YEAR * DateTimeConstants.MILLIS_PER_DAY)
        / 2;
  }

  // -----------------------------------------------------------------------
  @Override
  long calculateFirstDayOfYearMillis(int year) {
    // Calculate relative to the nearest leap year and account for the difference later.
    int relativeYear = year - iEpochYear;
    int leapYears;
    if (relativeYear <= 0) {
      // Add 3 before shifting right since /4 and >>2 behave differently on negative numbers.
      leapYears = (relativeYear + 3) >> 2;
    } else {
      leapYears = relativeYear >> 2;
      // An adjustment is needed after the epoch year, as Jan 1st is before the leap day.
      if (!isLeapYear(year)) {
        leapYears++;
      }
    }

    long millis = (relativeYear * 365L + leapYears) * (long) DateTimeConstants.MILLIS_PER_DAY;

    // Adjust to account for the difference between Jan 1st of the epoch year and this
    // calendar's Java epoch reference date.
    return millis + (365L - EPOCH_REFERENCE_DAY_OF_YEAR) * DateTimeConstants.MILLIS_PER_DAY;
  }

  // -----------------------------------------------------------------------
  @Override
  boolean isLeapDay(long instant) {
    return dayOfMonth().get(instant) == 6 && monthOfYear().isLeap(instant);
  }

  // -----------------------------------------------------------------------
  @Override
  protected void assemble(Fields fields) {
    if (getBase() == null) {
      super.assemble(fields);

      // Fixed-month calendars in this codebase have no year zero.
      fields.year = new SkipDateTimeField(this, fields.year);
      fields.weekyear = new SkipDateTimeField(this, fields.weekyear);

      fields.era = iEraField;
      fields.monthOfYear = new BasicMonthOfYearDateTimeField(this, 13);
      fields.months = fields.monthOfYear.getDurationField();
    }
  }
}
