/*
 * Copyright © 2024 Cask Data, Inc.
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

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.DirectiveParseException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ByteSizeTimeDurationParser}.
 */
public class ByteSizeTimeDurationParserTest {

  @Test
  public void testParseByteSize() throws DirectiveParseException {
    // Test basic units
    Assert.assertEquals(1, ByteSizeTimeDurationParser.parseByteSize("1B"));
    Assert.assertEquals(1024, ByteSizeTimeDurationParser.parseByteSize("1KB"));
    Assert.assertEquals(1024 * 1024, ByteSizeTimeDurationParser.parseByteSize("1MB"));
    Assert.assertEquals(1024 * 1024 * 1024, ByteSizeTimeDurationParser.parseByteSize("1GB"));
    Assert.assertEquals(1024L * 1024 * 1024 * 1024, ByteSizeTimeDurationParser.parseByteSize("1TB"));
    Assert.assertEquals(1024L * 1024 * 1024 * 1024 * 1024, ByteSizeTimeDurationParser.parseByteSize("1PB"));
    
    // Test decimal values
    Assert.assertEquals(1536, ByteSizeTimeDurationParser.parseByteSize("1.5KB"));
    Assert.assertEquals(1536 * 1024, ByteSizeTimeDurationParser.parseByteSize("1.5MB"));
    
    // Test case insensitivity
    Assert.assertEquals(1024, ByteSizeTimeDurationParser.parseByteSize("1kb"));
    Assert.assertEquals(1024 * 1024, ByteSizeTimeDurationParser.parseByteSize("1mb"));
    
    // Test whitespace
    Assert.assertEquals(1024, ByteSizeTimeDurationParser.parseByteSize("1 KB"));
    Assert.assertEquals(1024, ByteSizeTimeDurationParser.parseByteSize(" 1KB "));
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeNull() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize(null);
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeEmpty() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize("");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeZero() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize("0KB");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeNegative() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize("-1KB");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeInvalidFormat() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize("1.5XX");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseByteSizeOverflow() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseByteSize("9999999PB");
  }

  @Test
  public void testParseTimeDuration() throws DirectiveParseException {
    // Test basic units
    Assert.assertEquals(1, ByteSizeTimeDurationParser.parseTimeDuration("1000000n"));
    Assert.assertEquals(1, ByteSizeTimeDurationParser.parseTimeDuration("1000μ"));
    Assert.assertEquals(1, ByteSizeTimeDurationParser.parseTimeDuration("1000u"));
    Assert.assertEquals(1, ByteSizeTimeDurationParser.parseTimeDuration("1ms"));
    Assert.assertEquals(1000, ByteSizeTimeDurationParser.parseTimeDuration("1s"));
    Assert.assertEquals(60 * 1000, ByteSizeTimeDurationParser.parseTimeDuration("1m"));
    Assert.assertEquals(60 * 60 * 1000, ByteSizeTimeDurationParser.parseTimeDuration("1h"));
    Assert.assertEquals(24 * 60 * 60 * 1000L, ByteSizeTimeDurationParser.parseTimeDuration("1d"));
    
    // Test decimal values
    Assert.assertEquals(1500, ByteSizeTimeDurationParser.parseTimeDuration("1.5s"));
    Assert.assertEquals(90 * 1000, ByteSizeTimeDurationParser.parseTimeDuration("1.5m"));
    
    // Test case insensitivity
    Assert.assertEquals(1000, ByteSizeTimeDurationParser.parseTimeDuration("1S"));
    Assert.assertEquals(60 * 60 * 1000, ByteSizeTimeDurationParser.parseTimeDuration("1H"));
    
    // Test whitespace
    Assert.assertEquals(1000, ByteSizeTimeDurationParser.parseTimeDuration("1 s"));
    Assert.assertEquals(1000, ByteSizeTimeDurationParser.parseTimeDuration(" 1s "));
    
    // Test rounding
    Assert.assertEquals(2, ByteSizeTimeDurationParser.parseTimeDuration("1500000n"));
    Assert.assertEquals(2, ByteSizeTimeDurationParser.parseTimeDuration("1500μ"));
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationNull() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration(null);
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationEmpty() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration("");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationZero() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration("0s");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationNegative() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration("-1s");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationInvalidFormat() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration("1.5x");
  }

  @Test(expected = DirectiveParseException.class)
  public void testParseTimeDurationOverflow() throws DirectiveParseException {
    ByteSizeTimeDurationParser.parseTimeDuration("999999999d");
  }

  @Test
  public void testFormatByteSize() {
    // Test basic units
    Assert.assertEquals("1B", ByteSizeTimeDurationParser.formatByteSize(1));
    Assert.assertEquals("1.0KB", ByteSizeTimeDurationParser.formatByteSize(1024));
    Assert.assertEquals("1.0MB", ByteSizeTimeDurationParser.formatByteSize(1024 * 1024));
    Assert.assertEquals("1.0GB", ByteSizeTimeDurationParser.formatByteSize(1024 * 1024 * 1024));
    Assert.assertEquals("1.0TB", ByteSizeTimeDurationParser.formatByteSize(1024L * 1024 * 1024 * 1024));
    Assert.assertEquals("1.0PB", ByteSizeTimeDurationParser.formatByteSize(1024L * 1024 * 1024 * 1024 * 1024));
    
    // Test decimal values
    Assert.assertEquals("1.5KB", ByteSizeTimeDurationParser.formatByteSize(1536));
    Assert.assertEquals("1.5MB", ByteSizeTimeDurationParser.formatByteSize(1536 * 1024));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatByteSizeNegative() {
    ByteSizeTimeDurationParser.formatByteSize(-1);
  }

  @Test
  public void testFormatTimeDuration() {
    // Test basic units
    Assert.assertEquals("500ms", ByteSizeTimeDurationParser.formatTimeDuration(500));
    Assert.assertEquals("1.0s", ByteSizeTimeDurationParser.formatTimeDuration(1000));
    Assert.assertEquals("1.0m", ByteSizeTimeDurationParser.formatTimeDuration(60 * 1000));
    Assert.assertEquals("1.0h", ByteSizeTimeDurationParser.formatTimeDuration(60 * 60 * 1000));
    Assert.assertEquals("1.0d", ByteSizeTimeDurationParser.formatTimeDuration(24 * 60 * 60 * 1000));
    
    // Test decimal values
    Assert.assertEquals("1.5s", ByteSizeTimeDurationParser.formatTimeDuration(1500));
    Assert.assertEquals("1.5m", ByteSizeTimeDurationParser.formatTimeDuration(90 * 1000));
    
    // Test edge cases
    Assert.assertEquals("1ms", ByteSizeTimeDurationParser.formatTimeDuration(0));  // Sub-millisecond precision
    Assert.assertEquals("999ms", ByteSizeTimeDurationParser.formatTimeDuration(999));
    Assert.assertEquals("59.9s", ByteSizeTimeDurationParser.formatTimeDuration(59900));
    Assert.assertEquals("59.9m", ByteSizeTimeDurationParser.formatTimeDuration(3594000));
    Assert.assertEquals("23.9h", ByteSizeTimeDurationParser.formatTimeDuration(86040000));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormatTimeDurationNegative() {
    ByteSizeTimeDurationParser.formatTimeDuration(-1);
  }
} 