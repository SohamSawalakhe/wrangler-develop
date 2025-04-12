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
 * A token that represents byte size values with units (B, KB, MB, GB, TB, PB).
 */
@PublicEvolving
public class ByteSize implements Token {
  private static final Pattern BYTE_SIZE_PATTERN = 
    Pattern.compile("^(\\d+(\\.\\d+)?)\\s*(B|KB|MB|GB|TB|PB)$", Pattern.CASE_INSENSITIVE);
  
  private final String originalValue;
  private final double numericValue;
  private final String unit;
  private final long bytes;

  /**
   * Constructor to create a ByteSize token from a string representation (e.g., "10KB", "1.5MB").
   *
   * @param value String representation of byte size with unit
   * @throws IllegalArgumentException if the value does not match the expected pattern
   */
  public ByteSize(String value) {
    this.originalValue = value;
    
    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value.trim());
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
        String.format("Invalid byte size format: %s. Expected pattern like '10KB', '1.5MB', etc.", value));
    }
    
    this.numericValue = Double.parseDouble(matcher.group(1));
    this.unit = matcher.group(3).toUpperCase();
    this.bytes = convertToBytes();
  }

  /**
   * Converts the size value to bytes based on the unit.
   *
   * @return the size in bytes
   */
  private long convertToBytes() {
    switch (unit) {
      case "B":
        return (long) numericValue;
      case "KB":
        return (long) (numericValue * 1024);
      case "MB":
        return (long) (numericValue * 1024 * 1024);
      case "GB":
        return (long) (numericValue * 1024 * 1024 * 1024);
      case "TB":
        return (long) (numericValue * 1024 * 1024 * 1024 * 1024);
      case "PB":
        return (long) (numericValue * 1024 * 1024 * 1024 * 1024 * 1024);
      default:
        throw new IllegalStateException("Unknown unit: " + unit);
    }
  }

  /**
   * Gets the size in bytes.
   *
   * @return size in bytes
   */
  public long getBytes() {
    return bytes;
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
   * Gets the unit (B, KB, MB, GB, TB, PB).
   *
   * @return the unit
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Converts this byte size to a different unit.
   *
   * @param targetUnit the target unit to convert to
   * @return the size in the target unit
   */
  public double convertTo(String targetUnit) {
    long bytes = getBytes();
    switch (targetUnit.toUpperCase()) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024.0);
      case "GB":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      case "PB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
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
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", originalValue);
    object.addProperty("bytes", bytes);
    object.addProperty("unit", unit);
    object.addProperty("numericValue", numericValue);
    return object;
  }
  
  @Override
  public String toString() {
    return originalValue;
  }
} 

