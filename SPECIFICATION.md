# `com.legacy.system.datetime` Specification

This document is a **reverse-engineered specification** of the date/time library located at
`src/main/java/com/legacy/system/datetime`. No design document or test suite existed for this
code before this document was written; everything below was derived by static analysis of the
current source code, which is treated as the single source of truth.

The library is, functionally, the Joda-Time 2.14.3 codebase with its packages renamed from
`org.joda.time` to `com.legacy.system.datetime`. Its behaviour therefore follows the well known
Joda-Time semantics, and this document records that behaviour as observed in *this* checkout,
including the exact edge cases implemented by the source.

## 1. Package layout

| Package | Responsibility |
|---|---|
| `com.legacy.system.datetime` | Public API: `DateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `Instant`, `Duration`, `Period`, `Interval`, `DateTimeZone`, single-field periods (`Days`, `Weeks`, `Months`, `Years`, `Hours`, `Minutes`, `Seconds`, `Minutes`), constants, exceptions. |
| `com.legacy.system.datetime.base` | Abstract base classes shared by the public API (`AbstractInstant`, `AbstractPartial`, `AbstractInterval`, `BaseDateTime`, `BasePeriod`, `BaseDuration`, `BaseInterval`, `BaseLocal`, `BaseSingleFieldPeriod`). |
| `com.legacy.system.datetime.chrono` | `Chronology` implementations: `ISOChronology`, `GregorianChronology`, `JulianChronology`, `BuddhistChronology`, `CopticChronology`, `EthiopicChronology`, `IslamicChronology`, `GJChronology`, `ZonedChronology`, `LenientChronology`, `StrictChronology`, plus the `Basic*` abstract building blocks. |
| `com.legacy.system.datetime.field` | Reusable `DateTimeField`/`DurationField` implementations used by the chronologies. |
| `com.legacy.system.datetime.format` | Formatting/parsing (`DateTimeFormat`, `ISODateTimeFormat`, `PeriodFormat`, `ISOPeriodFormat`, builders). |
| `com.legacy.system.datetime.convert` | Converters that let API methods accept `Object` parameters (`Date`, `Calendar`, `String`, `Long`, other readable types). |
| `com.legacy.system.datetime.tz` | Time zone database (`DateTimeZoneBuilder`, `ZoneInfoCompiler`, zone providers). Compiled zone data (from `tz.database.version` = `2026cgtz`) ships in `com/legacy/system/datetime/tz/data`. |

## 2. Core concepts

### 2.1 Instant vs. Partial vs. Duration/Period

- **`ReadableInstant`** implementations (`Instant`, `DateTime`, `MutableDateTime`) represent an
  exact point on the time-line: a millisecond count since the epoch (`1970-01-01T00:00:00Z`) plus
  a `Chronology` (which carries a `DateTimeZone`).
- **`ReadablePartial`** implementations (`LocalDate`, `LocalDateTime`, `LocalTime`, `YearMonth`,
  `MonthDay`, `Partial`, and the deprecated `YearMonthDay`/`TimeOfDay`) represent a date/time with
  **no time zone**. Internally they store "local millis" (millis since epoch as if the zone were
  UTC) plus a `Chronology` whose zone has always been stripped to UTC (`chronology.withUTC()`).
  Once constructed, the time zone used to derive `now()` is irrelevant to the object's identity.
- **`Duration`** is an exact, fixed number of milliseconds — no calendar fields.
- **`Period`** is a *field-based* amount (years/months/weeks/days/hours/minutes/seconds/millis)
  interpreted according to a `PeriodType`. Adding a `Period` to an instant adds each field
  individually (so 1 day across a DST spring-forward transition can add 23h), whereas adding a
  `Duration` always adds an exact number of milliseconds.

### 2.2 Epoch reference point

`1970-01-01T00:00:00Z` is day-of-week **Thursday (`DateTimeConstants.THURSDAY` = 4)**; this
constant is hard-coded in `BasicChronology.getDayOfWeek(long)` and everything else derives from
it.

### 2.3 `DateTimeConstants`

- Months: `JANUARY=1 … DECEMBER=12`.
- Days of week: `MONDAY=1 … SUNDAY=7` (ISO-8601 ordering; there is no `0`).
- `AM=0`, `PM=1`; `BC=BCE=0`, `AD=CE=1`.
- `MILLIS_PER_SECOND=1000`, `SECONDS_PER_MINUTE=60`, `MINUTES_PER_HOUR=60`, `HOURS_PER_DAY=24`,
  `DAYS_PER_WEEK=7`, and derived constants computed from these (e.g. `MILLIS_PER_DAY =
  86_400_000`). These are *nominal* — a real day/week can be shorter or longer due to DST.

## 3. `GregorianChronology` (`com.legacy.system.datetime.chrono.GregorianChronology`)

A **pure proleptic Gregorian calendar** (the Gregorian leap-year rule is projected backwards
before 1582, and year 0 exists — there is no "1 BC directly followed by 1 AD" gap).

### 3.1 Obtaining an instance

- `GregorianChronology.getInstanceUTC()` — singleton, zone = UTC.
- `GregorianChronology.getInstance()` — default zone, `minDaysInFirstWeek = 4`.
- `GregorianChronology.getInstance(DateTimeZone zone)` — `zone == null` → default zone;
  `minDaysInFirstWeek = 4`.
- `GregorianChronology.getInstance(DateTimeZone zone, int minDaysInFirstWeek)` —
  `minDaysInFirstWeek` must be in `[1, 7]` or `IllegalArgumentException` is thrown
  ("Invalid min days in first week: N"). Instances are cached per `(zone, minDaysInFirstWeek)`.
- `withUTC()` returns the UTC singleton; `withZone(zone)` returns `this` if `zone == getZone()`,
  otherwise a (possibly newly created, cached) instance in that zone; `zone == null` means the
  default zone.

### 3.2 Leap years

```
isLeapYear(year) == (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0)
```
Implemented with `(year & 3) == 0` (works for negative years because of two's-complement),
so e.g. `1600`, `2000`, `2400` are leap, `1700/1800/1900/2100` are not, `2024/2028` are leap,
`0` (year zero) is leap, `-4` is leap.

### 3.3 Supported year range

- `getMinYear()` = **-292275054**
- `getMaxYear()` = **292278993**

Years outside `[minYear-1, maxYear+1]` fail `getDateTimeMillis(...)` /
`getDateMidnightMillis(...)` with `IllegalFieldValueException` (see §8).

### 3.4 Month lengths (`BasicGJChronology`, shared by Gregorian & Julian)

```
non-leap: 31 28 31 30 31 30 31 31 30 31 30 31
leap:     31 29 31 30 31 30 31 31 30 31 30 31
```
- `getDaysInMonthMax(month)` returns the leap-year (maximum) length for that month regardless of
  year (used as an upper bound), e.g. `getDaysInMonthMax(2) == 29`.
- `getDaysInYearMonth(year, month)` returns the length for the *actual* year.

### 3.5 Weeks

- ISO-8601 week rules: a week belongs to the year that owns its Thursday; `getMinimumDaysInFirstWeek()`
  (default 4) controls whether a year's first partial week counts as week 1 of the new year or
  the last week of the previous year.
- `weekyear` can differ from `year` for dates in the first/last days of January/December (e.g.
  `2016-01-01` is in `weekyear 2015`, week 53, because 2016-01-01 is a Friday).

### 3.6 Field construction & derived arithmetic (via `BasicChronology`/`BasicGJChronology`)

- `dayOfMonth().add(instant, months)` (i.e. `plusMonths`/`minusMonths`) **clamps** the day of
  month to the target month's length when it overflows: `2013-01-31 plusMonths(1)` → `2013-02-28`
  (or `29` in a leap year); `2000-03-31 minusMonths(1)` → `2000-02-29`.
- `year().setYear(instant, year)`: moving `Feb 29` to a non-leap target year decrements the
  day-of-year by one (lands on `Feb 28`); moving a `Mar 1..Dec 31` date onto a year where that
  day-of-year would now be `Feb 29` increments it.
- `getDayOfWeek` / `getMillisOfDay` are defined for negative millis as well as positive, so dates
  before 1970 and negative years behave correctly (no special-casing needed by callers).

### 3.7 Equality/identity

Two `BasicChronology`-derived chronologies (`equals`) are equal iff same runtime class, same
`getMinimumDaysInFirstWeek()`, and same `getZone()`. `hashCode` is
`getClass().getName().hashCode() * 11 + zone.hashCode() + minDaysInFirstWeek`.

## 4. `ISOChronology`

The **default chronology** used whenever a public constructor is passed `chronology == null` (or
no chronology argument at all). Structurally it is `GregorianChronology`'s calendar rules layered
under `ZonedChronology`, but decorated so that:
- Its "eras" field only recognises `BC`/`AD` boundary at year 0 the same way as `GregorianChronology`.
- `ISOChronology.getInstanceUTC()`, `.getInstance()`, `.getInstance(zone)` mirror `GregorianChronology`'s
  factory shape. `minDaysInFirstWeek` is fixed at 4 (not user-selectable, unlike `GregorianChronology`).
- Date-only classes (`LocalDate`, `LocalDateTime`, `LocalTime`, and every "no-chronology-argument"
  constructor of `DateTime`/`Instant`/`Period`/`Interval`) resolve to `ISOChronology` in either the
  default zone (`DateTime`) or forced UTC (`LocalDate` family, `Instant`).

## 5. `DateTime` (`com.legacy.system.datetime.DateTime`)

Immutable, zone-aware, chronology-aware point in time.

### 5.1 Construction

- `new DateTime()` — now, `ISOChronology` in the **default time zone**.
- `new DateTime(millis)` / `new DateTime(millis, zone)` / `new DateTime(millis, chronology)`.
- `new DateTime(year, month, day, hour, minute[, second[, millis]] [, zone|chronology])` — any
  combination down to `(year, month, day, hour, minute)`; missing trailing fields default to `0`.
  A `null` `zone`/`chronology` argument means "use the default"; passing an explicit `chronology`
  uses that chronology's own zone (not the default).
- `new DateTime(Object instant[, zone|chronology])` — accepts anything registered with
  `ConverterManager` (`java.util.Date`, `java.util.Calendar`, ISO8601 `String`, `Long`, another
  `ReadableInstant`, etc.).
- Field values out of range throw `IllegalFieldValueException` (e.g. month 13, day 32, hour 24).

### 5.2 Equality, ordering (`AbstractInstant`)

- **`equals(Object)`**: `true` only if `getMillis()` is equal **and** `getChronology()` is equal
  (same calendar system **and** same zone). Two `DateTime`s at the same instant but different
  zones are **not** `.equals()`.
- **`isEqual` / `isBefore` / `isAfter` / `compareTo`**: compare **only** the millisecond instant,
  ignoring chronology and zone entirely. So `dt.isEqual(dt.withZone(otherZone))` is always `true`
  even though `dt.equals(dt.withZone(otherZone))` is `false`.
- `hashCode()` = `(int)(millis ^ (millis >>> 32)) + chronology.hashCode()`.
- `toString()` renders ISO-8601 (`yyyy-MM-dd'T'HH:mm:ss.SSSZZ`), using the zone offset at that
  instant (e.g. `+09:00`).

### 5.3 Field arithmetic

- `plusXxx(n)` / `minusXxx(n)` (`Years`, `Months`, `Weeks`, `Days`, `Hours`, `Minutes`, `Seconds`,
  `Millis`) add via the chronology's field, so they respect DST and month-length clamping (see
  §3.6). `plus(0)`/`minus(0)` return `this` unchanged (no new object).
- `plus(ReadableDuration)` / `plus(ReadablePeriod)` add an exact duration or a field-based
  period respectively; the two can differ across a DST boundary.
- `withZone(zone)` keeps the instant fixed and changes only the zone used to *display* fields
  (`getHourOfDay()` etc. changes; `getMillis()` does not).
- `withZoneRetainFields(zone)` keeps the **local field values** the same and recomputes the
  millisecond instant for the new zone (`getMillis()` changes; local wall-clock fields do not).
- `withXxx(n)` methods ("set field") clamp analogous to `plusMonths`; setting `dayOfMonth` beyond
  the current month's max throws `IllegalFieldValueException` instead of clamping (unlike
  `plusMonths`, which clamps).

## 6. `Period` (`com.legacy.system.datetime.Period`)

Immutable, field-based amount of time. Default field set is `PeriodType.standard()` = `years,
months, weeks, days, hours, minutes, seconds, millis` (any field not requested by a narrower
`PeriodType` is simply forced to `0`).

### 6.1 Construction

- Factory helpers `Period.years(n)`, `.months(n)`, `.weeks(n)`, `.days(n)`, `.hours(n)`,
  `.minutes(n)`, `.seconds(n)`, `.millis(n)` each create a *standard*-type period with only that
  one field non-zero; chain with `.withXxx(n)` to add more fields (e.g.
  `Period.years(2).withMonths(6)` → `P2Y6M`).
- `new Period(years, months, weeks, days, hours, minutes, seconds, millis)` — explicit 8-field
  constructor, standard type.
- `new Period(long durationMillis)` — decomposes an exact millisecond duration into
  hours/minutes/seconds/millis **only** (uses `PeriodType.standard()`, but years/months/weeks/days
  end up `0` because the conversion is done through the millisecond (precise) fields, not calendar
  fields) — equivalent to `new Period(millis, PeriodType.standard())`.
- `new Period(long startInstant, long endInstant[, PeriodType][, Chronology])` and
  `new Period(ReadableInstant start, ReadableInstant end[, PeriodType])` — calculates the *calendar
  field difference* using the given chronology (default: `ISOChronology` in default zone). The
  algorithm fills the **largest fields first** and prefers advancing the month field over the day
  field when start-day > end-month-length:
  - `2013-01-31` → `2013-02-28` = **`P1M`** exactly (treated as "one whole month").
  - `2013-01-31` → `2013-03-30` = **`P1M4W2D`** (one month, then the remaining 30 days are
    expressed as 4 weeks + 2 days because `weeks` is part of the standard type).
- `Period.fieldDifference(ReadablePartial start, ReadablePartial end)` — subtracts field-by-field
  **without borrowing/wrapping** between fields; the two partials must have exactly the matching,
  non-overlapping field types. E.g. day-of-month 27 → day-of-month 2 of the next month yields
  `P1M-25D`, not `P6D`.
- `Period.parse(String)` parses ISO-8601 period format (`PnYnMnWnDTnHnMnS`) via
  `ISOPeriodFormat.standard()`.

### 6.2 Equality

Two periods are `.equals()` only if they have the **same `PeriodType`** and the **same value in
every field of that type** — `Period.days(1)` is **not** equal to `Period.hours(24)`, and
`Period.hours(1)` is **not** equal to `Period.minutes(60)`, even though both pairs represent the
same *duration* under normal circumstances. Use `toDuration()`/`toStandardDuration()` (needs a
reference instant for calendar-based periods) to compare actual elapsed time.

### 6.3 Normalization

- `normalizedStandard()` (and its `PeriodType`-argument overload) redistributes field values into
  the standard type using *fixed* (non-calendar) conversion ratios (`1 year = 12 months`, `1 week
  = 7 days`) — it does **not** know about variable month/year lengths, so it is only safe for
  periods that were not calculated from real calendar dates spanning irregular months.
- Arithmetic (`plus`, `minus`, `multipliedBy`, `negated`) is purely on the stored integer field
  values; it does not re-derive from any instant.

### 6.4 Applying a `Period`

`instant.plus(period)` (on `DateTime`/`MutableDateTime`) adds each field of the period in
**largest-to-smallest field order**, using the chronology's field arithmetic for each step (so DST
and month-length rules from §3.6/§5.3 apply per field, and the overall result can differ from
naively converting the period to milliseconds).

## 7. Other value types (used together with the above)

### 7.1 `Instant`

- No time zone concept; internally always resolves fields via `ISOChronology` in **UTC**
  (`getChronology()` returns `ISOChronology.getInstanceUTC()`).
- `Instant.EPOCH` = `1970-01-01T00:00:00.000Z`.
- `equals`/`compareTo`/`isBefore`/`isAfter` behave like any other `ReadableInstant` (see §5.2);
  since chronology is always the UTC ISO singleton, `equals` and `isEqual` coincide for two
  `Instant`s.
- `toDateTime()` is **overridden** (it does not use the inherited `AbstractInstant.toDateTime()`,
  which would keep UTC): because an `Instant` has no zone of its own to "retain", `instant.toDateTime()`
  returns `new DateTime(getMillis(), ISOChronology.getInstance())`, i.e. the millis are reinterpreted
  in the **JVM default zone**, not UTC. This is a deliberate, documented exception to the
  otherwise-uniform `toDateTime()` contract shared by `DateTime`/`MutableDateTime` (which *do*
  retain their zone).

### 7.2 `LocalDate` / `LocalDateTime` / `LocalTime`

- No time zone. `new LocalDate(year, month, day)` (etc.) always uses `ISOChronology.getInstanceUTC()`;
  supplying a different `Chronology` only affects field-arithmetic *rules* (leap years, month
  lengths), not any real-world zone.
- `new LocalDate()` (no-arg) captures `DateTimeUtils.currentTimeMillis()` **read through the
  default time zone**, then discards the zone — "today" is evaluated once, at construction time,
  in the default zone, and is fixed thereafter.
- `equals()` (`AbstractPartial`) requires the **same set of field types**, the **same values**,
  and the **same chronology**; `compareTo` compares field values in largest-to-smallest order and
  throws `ClassCastException` if the other partial's field types don't match exactly (e.g.
  comparing a `LocalDate` to a `YearMonthDay` with different field types).
- `plusMonths`/`minusMonths`/`plusYears` clamp the day-of-month exactly as `DateTime` does (§3.6).
- `LocalDate.toString()` → `yyyy-MM-dd`; `LocalTime.toString()` → `HH:mm:ss.SSS`;
  `LocalDateTime.toString()` → `yyyy-MM-dd'T'HH:mm:ss.SSS` (no zone offset, since there is no zone).

### 7.3 `Duration`

- Immutable exact millisecond count (`long`). `Duration.ZERO`.
- `standardDays/Hours/Minutes/Seconds(n)` multiply `n` by the *nominal* constant from
  `DateTimeConstants` (e.g. `86_400_000` per day) via `FieldUtils.safeMultiply` (throws
  `ArithmeticException` on `long` overflow).
- `getStandardDays()`/`getStandardHours()`/etc. divide `getMillis()` by the nominal constant using
  **truncating integer division** — `Duration.standardSeconds(2).getMillis()==2000`, but
  `new Duration(2999).getStandardSeconds() == 2` (not 3), and `new Duration(-2999).getStandardSeconds()
  == -2` (truncation toward zero, not floor).
- `dividedBy(long)` truncates toward zero by default; `dividedBy(long, RoundingMode)` allows other
  rounding; both throw `ArithmeticException` on divide-by-zero (`FieldUtils.safeDivide`).
- `negated()` throws `ArithmeticException` if `getMillis() == Long.MIN_VALUE` (cannot negate).

### 7.4 `Interval`

- Half-open `[start, end)`. Constructors throw `IllegalArgumentException` ("The end instant must
  be greater than the start instant") if `end < start`; `start == end` is a valid **zero-length**
  interval.
- `contains(instant)`: `start <= instant < end`; a zero-length interval contains nothing (not even
  its own boundary instant).
- `contains(ReadableInterval other)`: `thisStart <= otherStart && otherStart < thisEnd && otherEnd
  <= thisEnd` — an interval always contains itself and any sub-interval, but never a
  zero-length interval located exactly at its own end.
- `overlaps(other)`: `thisStart < otherEnd && otherStart < thisEnd`; **abutting** intervals (one's
  end == the other's start) do **not** overlap. `[09:00,10:00)` and `[10:00,11:00)` abut and do
  not overlap; `[09:00,10:00)` and `[09:30,09:30)` (zero-length, inside) **do** "overlap" per this
  formula even though a zero-length interval "contains nothing" — `contains` and `overlaps` are
  independent predicates, not complements.
- `isEqual(other)` compares only start/end millis, ignoring chronology (unlike `Object.equals`,
  which — per `BaseInterval` — also compares chronology).
- `toPeriod()`/`toDuration()` convert to a calendar-based `Period` or exact `Duration`
  respectively.

### 7.5 `DateTimeZone`

- `DateTimeZone.UTC` — the fixed-offset zero-offset singleton, `getID() == "UTC"`.
- `DateTimeZone.forID(String id)` — `"UTC"` or any Olson id present in the compiled zone database
  (`com/legacy/system/datetime/tz/data`, version `2026cgtz` in this build) or a fixed-offset id of
  the form `"+HH:mm"`/`"-HH:mm"`; throws `IllegalArgumentException` for unknown ids.
- `DateTimeZone.forOffsetHours(n)` / `forOffsetHoursMinutes(h, m)` / `forOffsetMillis(ms)` build
  **fixed**, never-changing offset zones (no DST); `forOffsetHours` requires `-23 <= n <= 23` or
  throws `IllegalArgumentException`.
- `DateTimeZone.getDefault()` reads the JVM default (`java.util.TimeZone.getDefault()` the first
  time it is queried, or the `user.timezone` system property / `DateTimeZone.setDefault` override).

### 7.6 Single-field periods: `Days`, `Weeks`, `Months`, `Years`, `Hours`, `Minutes`, `Seconds`

- Immutable wrappers around a single `int` amount with a fixed, single-field `PeriodType`.
  `Years.years(n)` etc.; results are cached for small values (`ONE`, `TWO`, `THREE`, `ZERO`, …).
- `Days.daysBetween(start, end)` / `Weeks.weeksBetween` / `Months.monthsBetween` /
  `Years.yearsBetween` accept two `ReadableInstant`s or two `ReadablePartial`s and compute the
  largest whole number of that unit between them (truncating, calendar-aware — e.g.
  `Months.monthsBetween(2020-01-31, 2020-03-01)` is `0` months because Jan-31 + 1 month clamps to
  Feb-29/28, which is still on/after Mar-01 is false... i.e. the count is the number of times the
  unit can be added to `start` without exceeding `end`).
- Arithmetic methods (`plus`, `minus`, `multipliedBy`, `dividedBy`, `negated`) return a new
  instance of the same wrapper type and throw `ArithmeticException` on `int` overflow
  (`FieldUtils.safeAdd`/`safeMultiply`/`safeToInt`).

### 7.7 `IllegalFieldValueException`

Thrown whenever a value is set outside the bounds a `DateTimeField` allows (e.g. day-of-month 32,
month 13, hour 24, or a `GregorianChronology` year outside `[minYear-1, maxYear+1]`). Carries the
offending field type/name, the illegal value, and (when known) the lower/upper bound, and formats
a human-readable message such as
`"Value 32 for dayOfMonth must be in the range [1,31]"`.

## 8. Notable, easy-to-miss behaviours captured by the tests

1. `DateTime.equals` requires matching **chronology** (including zone); `isEqual` does not.
2. `Period.equals` requires matching **`PeriodType`**; a day is never `.equals()` to 24 hours.
3. `Interval.overlaps` treats abutting intervals (touching boundary) as **not** overlapping, while
   `contains(ReadableInterval)` **does** consider a sub-interval that touches the far boundary as
   contained as long as its end does not exceed this interval's end.
4. `plusMonths`/`plusYears` **clamp** an out-of-range resulting day-of-month down to the month's
   last valid day; `withDayOfMonth` (a direct field "set") instead **throws**
   `IllegalFieldValueException` for a day that does not exist in the target month.
5. `Duration.getStandardXxx()` truncates toward zero, including for negative durations.
6. `Period(start, end)` calendar-difference calculation favors months over days when going from a
   long month's high day-of-month to a shorter following month (`Jan 31 → Feb 28` = exactly
   `P1M`), matching "the start date plus the calculated period equals the end date".
7. `GregorianChronology`'s Gregorian leap rule applies **proleptically** to all supported years,
   including negative (BC-side, in the CE/AD numbering with year 0) years.
8. `LocalDate`/`LocalDateTime`/`LocalTime` are chronology-bearing but always **zone-stripped to
   UTC** internally — passing different `DateTimeZone`s to their `now()`-style constructors only
   changes which "today" is captured, not how the resulting object stores or compares dates
   afterward.
9. `Instant.toDateTime()` switches to the **JVM default zone**, even though every other
   `ReadableInstant.toDateTime()` implementation (`DateTime`, `MutableDateTime`) **retains** its
   own zone — because a bare `Instant` has no zone to retain in the first place.
