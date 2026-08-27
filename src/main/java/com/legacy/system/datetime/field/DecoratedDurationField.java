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
package com.legacy.system.datetime.field;

import com.legacy.system.datetime.DurationField;
import com.legacy.system.datetime.DurationFieldType;

/**
 * <code>DecoratedDurationField</code> extends {@link BaseDurationField}, implementing only the
 * minimum required set of methods. These implemented methods delegate to a wrapped field.
 *
 * <p>This design allows new DurationField types to be defined that piggyback on top of another,
 * inheriting all the safe method implementations from BaseDurationField. Should any method require
 * pure delegation to the wrapped field, simply override and use the provided getWrappedField
 * method.
 *
 * <p>DecoratedDurationField is thread-safe and immutable, and its subclasses must be as well.
 *
 * @author Brian S O'Neill
 * @see DelegatedDurationField
 * @since 1.0
 */
public class DecoratedDurationField extends BaseDurationField {

  private static final long serialVersionUID = 8019982251647420015L;

  /** The DurationField being wrapped */
  private final DurationField iField;

  /**
   * Constructor.
   *
   * @param field the base field
   * @param type the type to actually use
   */
  public DecoratedDurationField(DurationField field, DurationFieldType type) {
    super(type);
    if (field == null) {
      throw new IllegalArgumentException("The field must not be null");
    }
    if (!field.isSupported()) {
      throw new IllegalArgumentException("The field must be supported");
    }
    iField = field;
  }

  // -----------------------------------------------------------------------
  /**
   * Gets the wrapped duration field.
   *
   * @return the wrapped DurationField
   */
  public final DurationField getWrappedField() {
    return iField;
  }

  @Override
  public boolean isPrecise() {
    return iField.isPrecise();
  }

  @Override
  public long getValueAsLong(long duration, long instant) {
    // CPD-OFF: structurally similar but operates on different interface types
    // (DateTimeField vs DurationField) or is an int/long overload pair. Overload
    // resolution for add(long,int) vs add(long,long) can hit different overflow-safety
    // paths in concrete DurationField implementations (see PreciseDurationField: the
    // int overload uses plain multiplication, the long overload uses
    // FieldUtils.safeMultiply), so collapsing an int-arg method into a cast-and-
    // delegate call to the long-arg one is not guaranteed behavior-preserving.
    return iField.getValueAsLong(duration, instant);
  }

  @Override
  public long getMillis(int value, long instant) {
    return iField.getMillis(value, instant);
  }

  @Override
  public long getMillis(long value, long instant) {
    return iField.getMillis(value, instant);
  }

  @Override
  public long add(long instant, int value) {
    return iField.add(instant, value);
  }

  @Override
  public long add(long instant, long value) {
    return iField.add(instant, value);
  }

  @Override
  public long getDifferenceAsLong(long minuendInstant, long subtrahendInstant) {
    // CPD-ON
    return iField.getDifferenceAsLong(minuendInstant, subtrahendInstant);
  }

  @Override
  public long getUnitMillis() {
    return iField.getUnitMillis();
  }
}
