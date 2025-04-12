/*
 * Copyright © 2017-2019 Cask Data, Inc.
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

package io.cdap.wrangler.steps.transformation;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Optional;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.annotations.Categories;
import io.cdap.wrangler.api.lineage.Lineage;
import io.cdap.wrangler.api.lineage.Many;
import io.cdap.wrangler.api.lineage.Mutation;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.parser.ByteSizeTimeDurationParser;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.lineage.Relation;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A directive that performs aggregation on byte size and time duration columns.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Categories(categories = { "aggregator", "statistics"})
@Description("Aggregates byte size and time duration columns and outputs statistics.")
public class AggregateStats implements Directive, Lineage {
  public static final String FUNCTION_NAME = "aggregate-stats";
  
  private static final String STORE_TOTAL_BYTES_KEY = "aggregate-stats:total-bytes";
  private static final String STORE_TOTAL_MILLIS_KEY = "aggregate-stats:total-millis";
  private static final String STORE_ROW_COUNT_KEY = "aggregate-stats:row-count";
  
  private static final Pattern BYTE_SIZE_UNIT_PATTERN = Pattern.compile("^[KMGTP]?B$", Pattern.CASE_INSENSITIVE);
  private static final Pattern TIME_DURATION_UNIT_PATTERN = Pattern.compile("^(n|[μu]|ms|[smhd])$", Pattern.CASE_INSENSITIVE);
  
  private String byteSizeColumn;
  private String timeDurationColumn;
  private String totalSizeColumnName;
  private String totalTimeColumnName;
  private String sizeOutputUnit;
  private String timeOutputUnit;
  
  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(FUNCTION_NAME);
    builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
    builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
    builder.define("totalSizeColumnName", TokenType.COLUMN_NAME);
    builder.define("totalTimeColumnName", TokenType.COLUMN_NAME);
    builder.define("sizeOutputUnit", TokenType.TEXT, Optional.TRUE);
    builder.define("timeOutputUnit", TokenType.TEXT, Optional.TRUE);
    return builder.build();
  }
  
  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.byteSizeColumn = ((ColumnName) args.value("byteSizeColumn")).value();
    this.timeDurationColumn = ((ColumnName) args.value("timeDurationColumn")).value();
    this.totalSizeColumnName = ((ColumnName) args.value("totalSizeColumnName")).value();
    this.totalTimeColumnName = ((ColumnName) args.value("totalTimeColumnName")).value();
    
    if (args.contains("sizeOutputUnit")) {
      this.sizeOutputUnit = ((Text) args.value("sizeOutputUnit")).value().toUpperCase();
      if (!BYTE_SIZE_UNIT_PATTERN.matcher(sizeOutputUnit).matches()) {
        throw new DirectiveParseException("Invalid byte size output unit: " + sizeOutputUnit + 
            ". Must be one of: B, KB, MB, GB, TB, PB");
      }
    } else {
      this.sizeOutputUnit = "MB"; // Default to megabytes
    }
    
    if (args.contains("timeOutputUnit")) {
      this.timeOutputUnit = ((Text) args.value("timeOutputUnit")).value().toLowerCase();
      if (!TIME_DURATION_UNIT_PATTERN.matcher(timeOutputUnit).matches()) {
        throw new DirectiveParseException("Invalid time duration output unit: " + timeOutputUnit + 
            ". Must be one of: n, μ/u, ms, s, m, h, d");
      }
    } else {
      this.timeOutputUnit = "s"; // Default to seconds
    }
  }
  
  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException, ErrorRowException {
    TransientStore store = context.getTransientStore();
    
    // If no store exists yet, initialize it with zeros
    if (!store.getVariables().contains(STORE_TOTAL_BYTES_KEY)) {
      store.set(TransientVariableScope.GLOBAL, STORE_TOTAL_BYTES_KEY, 0L);
    }
    if (!store.getVariables().contains(STORE_TOTAL_MILLIS_KEY)) {
      store.set(TransientVariableScope.GLOBAL, STORE_TOTAL_MILLIS_KEY, 0L);
    }
    if (!store.getVariables().contains(STORE_ROW_COUNT_KEY)) {
      store.set(TransientVariableScope.GLOBAL, STORE_ROW_COUNT_KEY, 0L);
    }
    
    // Get current values from store
    long totalBytes = (long) store.get(STORE_TOTAL_BYTES_KEY);
    long totalMillis = (long) store.get(STORE_TOTAL_MILLIS_KEY);
    long rowCount = (long) store.get(STORE_ROW_COUNT_KEY);
    long processedRows = 0;
    long errorRows = 0;

    // Process all rows to accumulate statistics
    for (Row row : rows) {
      boolean hasSize = row.find(byteSizeColumn) != -1;
      boolean hasTime = row.find(timeDurationColumn) != -1;
      
      if (!hasSize || !hasTime) {
        context.getMetrics().count("error.rows", 1);
        errorRows++;
        continue;
      }

      Object byteValue = row.getValue(byteSizeColumn);
      Object timeValue = row.getValue(timeDurationColumn);
      
      if (byteValue == null || timeValue == null) {
        context.getMetrics().count("error.rows", 1);
        errorRows++;
        continue;
      }
      
      try {
        // Parse byte size values
        String byteSizeStr = byteValue.toString();
        long bytes = ByteSizeTimeDurationParser.parseByteSize(byteSizeStr);
        totalBytes += bytes;
        
        // Parse time duration values
        String timeDurationStr = timeValue.toString();
        long millis = ByteSizeTimeDurationParser.parseTimeDuration(timeDurationStr);
        totalMillis += millis;
        
        processedRows++;
        context.getMetrics().count("processed.rows", 1);
      } catch (DirectiveParseException e) {
        context.getMetrics().count("error.rows", 1);
        errorRows++;
      }
      
      rowCount++;
    }
    
    // Update the store with new totals
    store.set(TransientVariableScope.GLOBAL, STORE_TOTAL_BYTES_KEY, totalBytes);
    store.set(TransientVariableScope.GLOBAL, STORE_TOTAL_MILLIS_KEY, totalMillis);
    store.set(TransientVariableScope.GLOBAL, STORE_ROW_COUNT_KEY, rowCount);
    
    // If all rows have been processed, generate the result row
    if (context.getEnvironment() == ExecutorContext.Environment.TRANSFORM) {
      if (processedRows == 0) {
        throw new DirectiveExecutionException(String.format(
            "No valid rows were processed. Total rows: %d, Error rows: %d", rowCount, errorRows));
      }
      
      // Format the values using our parser's format methods
      String formattedSize = formatByteSizeWithUnit(totalBytes, sizeOutputUnit);
      String formattedTime = formatTimeDurationWithUnit(totalMillis, timeOutputUnit);
      
      // Create a new row with aggregated results
      Row result = new Row();
      result.add(totalSizeColumnName, formattedSize);
      result.add(totalTimeColumnName, formattedTime);
      
      // Return the result row as the only row
      return List.of(result);
    }
    
    // Return empty list for intermediate steps (no rows emitted yet)
    return List.of();
  }
  
  @Override
  public Mutation lineage() {
    return Mutation.builder()
      .readable("Aggregates byte size from column '%s' and time duration from column '%s' into '%s' and '%s'", 
                byteSizeColumn, timeDurationColumn, totalSizeColumnName, totalTimeColumnName)
      .relation(Many.of(byteSizeColumn, timeDurationColumn), 
               Many.of(totalSizeColumnName, totalTimeColumnName))
      .build();
  }
  
  /**
   * Formats a byte size in bytes to a string with the specified unit.
   */
  private String formatByteSizeWithUnit(long bytes, String unit) {
    if (bytes < 0) {
      throw new IllegalArgumentException("Byte size cannot be negative");
    }
    
    double value;
    switch (unit.toUpperCase()) {
      case "B":
        value = bytes;
        break;
      case "KB":
        value = bytes / 1024.0;
        break;
      case "MB":
        value = bytes / (1024.0 * 1024.0);
        break;
      case "GB":
        value = bytes / (1024.0 * 1024.0 * 1024.0);
        break;
      case "TB":
        value = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
        break;
      case "PB":
        value = bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0);
        break;
      default:
        throw new IllegalArgumentException("Unsupported byte size unit: " + unit);
    }
    
    return String.format("%.2f%s", value, unit);
  }
  
  /**
   * Formats a duration in milliseconds to a string with the specified unit.
   */
  private String formatTimeDurationWithUnit(long millis, String unit) {
    if (millis < 0) {
      throw new IllegalArgumentException("Time duration cannot be negative");
    }
    
    double value;
    switch (unit.toLowerCase()) {
      case "n":
        value = millis * 1_000_000.0; // Convert ms to ns
        break;
      case "μ":
      case "u":
        value = millis * 1_000.0; // Convert ms to μs
        break;
      case "ms":
        value = millis; // Already in milliseconds
        break;
      case "s":
        value = millis / 1_000.0; // Convert ms to s
        break;
      case "m":
        value = millis / 60_000.0; // Convert ms to minutes
        break;
      case "h":
        value = millis / 3_600_000.0; // Convert ms to hours
        break;
      case "d":
        value = millis / 86_400_000.0; // Convert ms to days
        break;
      default:
        throw new IllegalArgumentException("Unsupported time duration unit: " + unit);
    }
    
    return String.format("%.2f%s", value, unit);
  }

  @Override
  public void destroy() {
    // No resources to clean up
  }
} 