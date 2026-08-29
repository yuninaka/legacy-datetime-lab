/*
 *  Copyright 2001-2013 Stephen Colebourne
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
package com.legacy.system.datetime.base;

import com.legacy.system.datetime.DateTimeFieldType;
import com.legacy.system.datetime.DurationFieldType;

/**
 * BaseLocal is an abstract implementation of ReadablePartial that use a local milliseconds internal
 * representation.
 *
 * <p>This class should generally not be used directly by API users. The {@link
 * org.joda.time.ReadablePartial} interface should be used when different kinds of partial objects
 * are to be referenced.
 *
 * <p>BasePartial subclasses may be mutable and not thread-safe.
 *
 * @author Stephen Colebourne
 * @since 1.5
 */
public abstract class BaseLocal extends AbstractPartial {

  /** Serialization version */
  @SuppressWarnings("unused")
  private static final long serialVersionUID = 276453175381783L;

  // -----------------------------------------------------------------------
  /**
   * Constructs a partial with the current time, using ISOChronology in the default zone to extract
   * the fields.
   *
   * <p>The constructor uses the default time zone, resulting in the local time being initialised.
   * Once the constructor is complete, all further calculations are performed without reference to a
   * timezone (by switching to UTC).
   */
  protected BaseLocal() {
    super();
  }

  // -----------------------------------------------------------------------
  /**
   * Gets the local milliseconds from the Java epoch of 1970-01-01T00:00:00 (not fixed to any
   * specific time zone).
   *
   * <p>This method is useful in certain circumstances for high performance access to the datetime
   * fields.
   *
   * @return the number of milliseconds since 1970-01-01T00:00:00
   */
  protected abstract long getLocalMillis();

  // -----------------------------------------------------------------------
  /**
   * Checks if the duration type specified is supported by this partial and chronology.
   *
   * <p>Declared here (rather than left as a same-named-but-unrelated method on each subclass) so
   * that {@link #computeFieldAdded} can dispatch to it polymorphically.
   *
   * @param type the type to check, may be null which returns false
   * @return true if the field type is supported
   */
  public abstract boolean isSupported(DurationFieldType type);

  // -----------------------------------------------------------------------
  /**
   * Get the value of one of the fields of a datetime.
   *
   * <p>This method gets the value of the specified field, delegating to the subclass's own {@link
   * #isSupported(DateTimeFieldType)} to decide whether the field is available.
   *
   * @param fieldType a field type, usually obtained from DateTimeFieldType, not null
   * @return the value of that field
   * @throws IllegalArgumentException if the field type is null or unsupported
   */
  @Override
  public int get(DateTimeFieldType fieldType) {
    if (fieldType == null) {
      throw new IllegalArgumentException("The DateTimeFieldType must not be null");
    }
    if (isSupported(fieldType) == false) {
      throw new IllegalArgumentException("Field '" + fieldType + "' is not supported");
    }
    return fieldType.getField(getChronology()).get(getLocalMillis());
  }

  // -----------------------------------------------------------------------
  /**
   * Gets the value of the field at the specified index, delegating to the subclass's own {@link
   * AbstractPartial#getField(int, com.legacy.system.datetime.Chronology)} to resolve which field
   * that index refers to.
   *
   * @param index the index
   * @return the value
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  @Override
  public int getValue(int index) {
    return getField(index, getChronology()).get(getLocalMillis());
  }

  // -----------------------------------------------------------------------
  /**
   * Computes the local millis resulting from setting the given field to the given value.
   *
   * <p>Shared by the subclasses whose {@code withField} does not special-case "no supported fields
   * differ" the way {@link com.legacy.system.datetime.LocalDateTime} does (it always supports every
   * field, so it skips the {@link #isSupported} check that this helper performs).
   *
   * @param fieldType the field type to set, not null
   * @param value the value to set
   * @return the resulting local millis
   * @throws IllegalArgumentException if the field is null or unsupported
   */
  protected long computeFieldSet(DateTimeFieldType fieldType, int value) {
    if (fieldType == null) {
      throw new IllegalArgumentException("Field must not be null");
    }
    if (isSupported(fieldType) == false) {
      throw new IllegalArgumentException("Field '" + fieldType + "' is not supported");
    }
    return fieldType.getField(getChronology()).set(getLocalMillis(), value);
  }

  /**
   * Computes the local millis resulting from adding the given amount to the given field, or null if
   * the amount is zero (the "no change" case, left for the caller to turn into {@code this}).
   *
   * @param fieldType the field type to add to, not null
   * @param amount the amount to add
   * @return the resulting local millis, or null if the amount is zero
   * @throws IllegalArgumentException if the field is null or unsupported
   */
  protected Long computeFieldAdded(DurationFieldType fieldType, int amount) {
    if (fieldType == null) {
      throw new IllegalArgumentException("Field must not be null");
    }
    if (isSupported(fieldType) == false) {
      throw new IllegalArgumentException("Field '" + fieldType + "' is not supported");
    }
    if (amount == 0) {
      return null;
    }
    return fieldType.getField(getChronology()).add(getLocalMillis(), amount);
  }
}
