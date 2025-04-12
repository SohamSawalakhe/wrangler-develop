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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for byte sizes and time durations in CDAP Wrangler.
 */
public class ByteSizeTimeDurationParser {

  private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("^(\\d+(\\.\\d+)?)\\s*([KMGTP]?B)$", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIME_DURATION_PATTERN = Pattern.compile("^(\\d+(\\.\\d+)?)\\s*([nμums]|ms|[hd])$", Pattern.CASE_INSENSITIVE);

  /**
   * Parses a byte size string into bytes.
   * 
   * @param value The byte size string to parse (e.g. "1.5MB")
   * @return The number of bytes
   * @throws DirectiveParseException if the string cannot be parsed
   */
  public static long parseByteSize(String value) throws DirectiveParseException {
    if (value == null || value.trim().isEmpty()) {
      throw new DirectiveParseException("Byte size value cannot be null or empty");
    }
    
    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      throw new DirectiveParseException("Invalid byte size format: " + value);
    }

    double number = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(3).toUpperCase();

    // Check for zero or negative values
    if (number <= 0) {
      throw new DirectiveParseException("Byte size must be positive: " + value);
    }

    long result;
    switch (unit) {
      case "B":
        result = (long) number;
        break;
      case "KB":
        result = (long) (number * 1024);
        break;
      case "MB":
        result = (long) (number * 1024 * 1024);
        break;
      case "GB":
        result = (long) (number * 1024 * 1024 * 1024);
        break;
      case "TB":
        result = (long) (number * 1024 * 1024 * 1024 * 1024);
        break;
      case "PB":
        result = (long) (number * 1024 * 1024 * 1024 * 1024 * 1024);
        break;
      default:
        throw new DirectiveParseException("Unsupported byte size unit: " + unit);
    }

    // Check for overflow
    if (result < 0) {
      throw new DirectiveParseException("Byte size value too large: " + value);
    }

    return result;
  }

  /**
   * Parses a time duration string into milliseconds.
   * 
   * @param value The time duration string to parse (e.g. "1.5h")
   * @return The duration in milliseconds
   * @throws DirectiveParseException if the string cannot be parsed
   */
  public static long parseTimeDuration(String value) throws DirectiveParseException {
    if (value == null || value.trim().isEmpty()) {
      throw new DirectiveParseException("Time duration value cannot be null or empty");
    }
    
    Matcher matcher = TIME_DURATION_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      throw new DirectiveParseException("Invalid time duration format: " + value);
    }

    double number = Double.parseDouble(matcher.group(1));
    String unit = matcher.group(3).toLowerCase();

    // Check for zero or negative values
    if (number <= 0) {
      throw new DirectiveParseException("Time duration must be positive: " + value);
    }

    long result;
    switch (unit) {
      case "n":
        // Convert nanoseconds to milliseconds (1 ns = 1/1,000,000 ms)
        result = Math.round(number / 1_000_000.0);
        break;
      case "μ":
      case "u":
        // Convert microseconds to milliseconds (1 μs = 1/1,000 ms)
        result = Math.round(number / 1_000.0);
        break;
      case "ms":
        // Already in milliseconds
        result = Math.round(number);
        break;
      case "s":
        // Convert seconds to milliseconds (1 s = 1,000 ms)
        result = Math.round(number * 1_000.0);
        break;
      case "m":
        // Convert minutes to milliseconds (1 m = 60,000 ms)
        result = Math.round(number * 60_000.0);
        break;
      case "h":
        // Convert hours to milliseconds (1 h = 3,600,000 ms)
        result = Math.round(number * 3_600_000.0);
        break;
      case "d":
        // Convert days to milliseconds (1 d = 86,400,000 ms)
        result = Math.round(number * 86_400_000.0);
        break;
      default:
        throw new DirectiveParseException("Unsupported time duration unit: " + unit);
    }

    // Check for overflow
    if (result < 0) {
      throw new DirectiveParseException("Time duration value too large: " + value);
    }

    return result;
  }

  /**
   * Formats a byte size in bytes to a human readable string.
   * 
   * @param bytes The number of bytes
   * @return A human readable string (e.g. "1.5MB")
   */
  public static String formatByteSize(long bytes) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Byte size cannot be negative");
    }
    
    if (bytes < 1024) {
      return bytes + "B";
    }
    
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTP".charAt(exp - 1) + "B";
    return String.format("%.1f%s", bytes / Math.pow(1024, exp), pre);
  }

  /**
   * Formats a duration in milliseconds to a human readable string.
   * 
   * @param millis The duration in milliseconds
   * @return A human readable string (e.g. "1.5h")
   */
  public static String formatTimeDuration(long millis) {
    if (millis < 0) {
      throw new IllegalArgumentException("Time duration cannot be negative");
    }
    
    // For sub-millisecond precision, we don't support formatting
    // since our internal representation is in milliseconds
    if (millis < 1) {
      return "1ms";
    }
    
    // Use milliseconds for durations less than a second
    if (millis < 1_000) {
      return millis + "ms";
    }
    
    // Use seconds for durations less than a minute
    if (millis < 60_000) {
      return String.format("%.1fs", millis / 1_000.0);
    }
    
    // Use minutes for durations less than an hour
    if (millis < 3_600_000) {
      return String.format("%.1fm", millis / 60_000.0);
    }
    
    // Use hours for durations less than a day
    if (millis < 86_400_000) {
      return String.format("%.1fh", millis / 3_600_000.0);
    }
    
    // Use days for longer durations
    return String.format("%.1fd", millis / 86_400_000.0);
  }
} 