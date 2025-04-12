/*
 * Copyright © 2023 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TimeDuration} class.
 */
public class TimeDurationTest {

  @Test
  public void testValidTimeDurations() {
    // Test nanoseconds
    TimeDuration duration = new TimeDuration("1000000ns");
    Assert.assertEquals(1L, duration.getMilliseconds());
    Assert.assertEquals("ns", duration.getUnit());
    Assert.assertEquals(1000000.0, duration.getNumericValue(), 0.0);

    // Test milliseconds
    duration = new TimeDuration("1500ms");
    Assert.assertEquals(1500L, duration.getMilliseconds());
    Assert.assertEquals("ms", duration.getUnit());
    Assert.assertEquals(1500.0, duration.getNumericValue(), 0.0);

    // Test seconds
    duration = new TimeDuration("2s");
    Assert.assertEquals(2000L, duration.getMilliseconds());
    Assert.assertEquals("s", duration.getUnit());
    Assert.assertEquals(2.0, duration.getNumericValue(), 0.0);

    // Test hours
    duration = new TimeDuration("1.5h");
    Assert.assertEquals(5400000L, duration.getMilliseconds());
    Assert.assertEquals("h", duration.getUnit());
    Assert.assertEquals(1.5, duration.getNumericValue(), 0.0);

    // Test days
    duration = new TimeDuration("1d");
    Assert.assertEquals(86400000L, duration.getMilliseconds());
    Assert.assertEquals("d", duration.getUnit());
    Assert.assertEquals(1.0, duration.getNumericValue(), 0.0);
  }

  @Test
  public void testUnitConversion() {
    TimeDuration duration = new TimeDuration("1h");
    Assert.assertEquals(3600000.0, duration.convertTo("ms"), 0.0);
    Assert.assertEquals(3600.0, duration.convertTo("s"), 0.0);
    Assert.assertEquals(1.0, duration.convertTo("h"), 0.0);
    Assert.assertEquals(3600000000000.0, duration.convertTo("ns"), 0.0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new TimeDuration("1.5x");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeValue() {
    new TimeDuration("-1.5h");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConversion() {
    TimeDuration duration = new TimeDuration("1h");
    duration.convertTo("invalid");
  }
} 

