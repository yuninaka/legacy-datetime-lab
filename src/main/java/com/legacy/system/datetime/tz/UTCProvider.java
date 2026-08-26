/*
 *  Copyright 2001-2009 Stephen Colebourne
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
package com.legacy.system.datetime.tz;

import com.legacy.system.datetime.DateTimeZone;
import java.util.Set;

/**
 * Simple time zone provider that supports only UTC.
 *
 * <p>UTCProvider is thread-safe and immutable.
 *
 * @author Brian S O'Neill
 * @since 1.0
 */
public final class UTCProvider implements Provider {

  /** The available IDs. */
  private static final Set<String> AVAILABLE_IDS = Set.of("UTC");

  /** Constructor. */
  public UTCProvider() {
    super();
  }

  /** Returns {@link DateTimeZone#UTC UTC} for <code>"UTC"</code>, null otherwise. */
  @Override
  public DateTimeZone getZone(String id) {
    if ("UTC".equalsIgnoreCase(id)) {
      return DateTimeZone.UTC;
    }
    return null;
  }

  /** Returns a singleton collection containing only <code>"UTC"</code>. */
  @Override
  public Set<String> getAvailableIDs() {
    return AVAILABLE_IDS;
  }
}
