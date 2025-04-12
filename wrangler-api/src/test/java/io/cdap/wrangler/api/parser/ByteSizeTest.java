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
 * Tests for {@link ByteSize} class.
 */
public class ByteSizeTest {

  @Test
  public void testValidByteSizes() {
    // Test bytes
    ByteSize size = new ByteSize("1024B");
    Assert.assertEquals(1024L, size.getBytes());
    Assert.assertEquals("B", size.getUnit());
    Assert.assertEquals(1024.0, size.getNumericValue(), 0.0);

    // Test kilobytes
    size = new ByteSize("1.5KB");
    Assert.assertEquals(1536L, size.getBytes());
    Assert.assertEquals("KB", size.getUnit());
    Assert.assertEquals(1.5, size.getNumericValue(), 0.0);

    // Test megabytes
    size = new ByteSize("2MB");
    Assert.assertEquals(2L * 1024 * 1024, size.getBytes());
    Assert.assertEquals("MB", size.getUnit());
    Assert.assertEquals(2.0, size.getNumericValue(), 0.0);

    // Test gigabytes
    size = new ByteSize("1.5GB");
    Assert.assertEquals(1610612736L, size.getBytes());
    Assert.assertEquals("GB", size.getUnit());
    Assert.assertEquals(1.5, size.getNumericValue(), 0.0);

    // Test terabytes
    size = new ByteSize("1TB");
    Assert.assertEquals(1099511627776L, size.getBytes());
    Assert.assertEquals("TB", size.getUnit());
    Assert.assertEquals(1.0, size.getNumericValue(), 0.0);

    // Test petabytes
    size = new ByteSize("0.5PB");
    Assert.assertEquals(562949953421312L, size.getBytes());
    Assert.assertEquals("PB", size.getUnit());
    Assert.assertEquals(0.5, size.getNumericValue(), 0.0);
  }

  @Test
  public void testUnitConversion() {
    ByteSize size = new ByteSize("1024MB");
    Assert.assertEquals(1024.0, size.convertTo("MB"), 0.0);
    Assert.assertEquals(1.0, size.convertTo("GB"), 0.0);
    Assert.assertEquals(1024.0 * 1024.0, size.convertTo("KB"), 0.0);
    Assert.assertEquals(1024.0 * 1024.0 * 1024.0, size.convertTo("B"), 0.0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new ByteSize("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new ByteSize("1.5XB");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeValue() {
    new ByteSize("-1.5MB");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidConversion() {
    ByteSize size = new ByteSize("1MB");
    size.convertTo("invalid");
  }
} 

