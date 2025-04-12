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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A token that represents time duration values with units (ns, ms, s, h, d).
 */
@PublicEvolving
public class TimeDuration implements Token {
  private static final Pattern TIME_DURATION_PATTERN = 
    Pattern.compile("^(\\d+(\\.\\d+)?)\\s*(ns|ms|s|h|d)$", Pattern.CASE_INSENSITIVE);
  
  private final String originalValue;
  private final double numericValue;
  private final String unit;
  private final long milliseconds;

  /**
   * Constructor to create a TimeDuration token from a string representation (e.g., "10s", "1.5h").
   *
   * @param value String representation of time duration with unit
   * @throws IllegalArgumentException if the value does not match the expected pattern
   */
  public TimeDuration(String value) {
    this.originalValue = value;
    
    Matcher matcher = TIME_DURATION_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        String.format("Invalid time duration format: %s. Expected pattern like '10s', '1.5h', etc.", value));
    }
    
    this.numericValue = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(3).toLowerCase();
    this.milliseconds = convertToMilliseconds();
  }

  /**
   * Converts the duration value to milliseconds based on the unit.
   *
   * @return the duration in milliseconds
   */
  private long convertToMilliseconds() {
    switch (unit) {
      case "ns":
        return (long) (numericValue / 1_000_000);
      case "ms":
        return (long) numericValue;
      case "s":
        return (long) (numericValue * 1000);
      case "h":
        return (long) (numericValue * 60 * 60 * 1000);
      case "d":
        return (long) (numericValue * 24 * 60 * 60 * 1000);
      default:
        throw new IllegalStateException("Unknown unit: " + unit);
    }
  }

  /**
   * Gets the duration in milliseconds.
   *
   * @return duration in milliseconds
   */
  public long getMilliseconds() {
    return milliseconds;
  }

  /**
   * Gets the numeric value before unit conversion.
   *
   * @return the numeric value
   */
  public double getNumericValue() {
    return numericValue;
  }

  /**
   * Gets the unit (ns, ms, s, h, d).
   *
   * @return the unit
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Converts this time duration to a different unit.
   *
   * @param targetUnit the target unit to convert to
   * @return the duration in the target unit
   */
  public double convertTo(String targetUnit) {
    long millis = getMilliseconds();
    switch (targetUnit.toLowerCase()) {
      case "ns":
        return millis * 1_000_000.0;
      case "ms":
        return millis;
      case "s":
        return millis / 1000.0;
      case "h":
        return millis / (60.0 * 60 * 1000);
      case "d":
        return millis / (24.0 * 60 * 60 * 1000);
      default:
        throw new IllegalArgumentException("Unknown unit: " + targetUnit);
    }
  }

  @Override
  public String value() {
    return originalValue;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", originalValue);
    object.addProperty("milliseconds", milliseconds);
    object.addProperty("unit", unit);
    object.addProperty("numericValue", numericValue);
    return object;
  }
  
  @Override
  public String toString() {
    return originalValue;
  }
} 

