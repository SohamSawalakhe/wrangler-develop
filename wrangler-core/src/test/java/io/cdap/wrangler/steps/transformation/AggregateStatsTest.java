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

package io.cdap.wrangler.steps.transformation;

import com.google.gson.JsonObject;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.TokenDefinition;
import io.cdap.wrangler.api.parser.UsageDefinition;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.cdap.etl.api.StageMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregateStatsTest {

  private ExecutorContext createMockContext() {
    ExecutorContext context = Mockito.mock(ExecutorContext.class);
    Mockito.when(context.getEnvironment()).thenReturn(ExecutorContext.Environment.TRANSFORM);
    
    StageMetrics metrics = Mockito.mock(StageMetrics.class);
    Mockito.when(context.getMetrics()).thenReturn(metrics);
    
    TransientStore store = Mockito.mock(TransientStore.class);
    Map<String, Object> variables = new HashMap<>();
    variables.put("aggregate-stats:total-bytes", 0L);
    variables.put("aggregate-stats:total-millis", 0L);
    variables.put("aggregate-stats:row-count", 0L);
    
    Mockito.when(context.getTransientStore()).thenReturn(store);
    Mockito.when(store.getVariables()).thenReturn(variables);
    Mockito.when(store.get("aggregate-stats:total-bytes")).thenReturn(0L);
    Mockito.when(store.get("aggregate-stats:total-millis")).thenReturn(0L);
    Mockito.when(store.get("aggregate-stats:row-count")).thenReturn(0L);
    
    return context;
  }

  @Test
  public void testUsageDefinition() {
    AggregateStats directive = new AggregateStats();
    UsageDefinition usage = directive.define();
    
    Assert.assertEquals("aggregate-stats", usage.getDirectiveName());
    List<TokenDefinition> tokens = usage.getTokens();
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(0).type());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(1).type());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(2).type());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(3).type());
    Assert.assertEquals(TokenType.TEXT, tokens.get(4).type());
    Assert.assertEquals(TokenType.TEXT, tokens.get(5).type());
  }

  @Test
  public void testBasicAggregation() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    args.put("sizeOutputUnit", new Text("MB"));
    args.put("timeOutputUnit", new Text("s"));
    
    directive.initialize(new MockArguments(args));
    
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("size", "1.5MB");
    row1.add("duration", "2.5s");
    rows.add(row1);
    
    Row row2 = new Row();
    row2.add("size", "500KB");
    row2.add("duration", "1500ms");
    rows.add(row2);
    
    ExecutorContext context = createMockContext();
    
    List<Row> results = directive.execute(rows, context);
    
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals("2.00MB", result.getValue("total_size"));
    Assert.assertEquals("4.00s", result.getValue("total_time"));
  }

  @Test(expected = DirectiveParseException.class)
  public void testInvalidByteUnit() throws DirectiveParseException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    args.put("sizeOutputUnit", new Text("invalid"));
    
    directive.initialize(new MockArguments(args));
  }

  @Test(expected = DirectiveParseException.class)
  public void testInvalidTimeUnit() throws DirectiveParseException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    args.put("timeOutputUnit", new Text("invalid"));
    
    directive.initialize(new MockArguments(args));
  }

  @Test
  public void testEmptyRows() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    
    directive.initialize(new MockArguments(args));
    
    List<Row> rows = new ArrayList<>();
    ExecutorContext context = createMockContext();
    
    try {
      directive.execute(rows, context);
      Assert.fail("Should have thrown DirectiveExecutionException");
    } catch (DirectiveExecutionException e) {
      Assert.assertEquals("No valid rows were processed. Total rows: 0, Error rows: 0", e.getMessage());
    }
  }

  @Test
  public void testInvalidValues() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    
    directive.initialize(new MockArguments(args));
    
    List<Row> rows = new ArrayList<>();
    
    // Row with invalid byte size
    Row row1 = new Row();
    row1.add("size", "invalid");
    row1.add("duration", "2.5s");
    rows.add(row1);
    
    // Row with invalid time duration
    Row row2 = new Row();
    row2.add("size", "500KB");
    row2.add("duration", "invalid");
    rows.add(row2);
    
    // Row with missing columns
    Row row3 = new Row();
    row3.add("other", "value");
    rows.add(row3);
    
    // Row with null values
    Row row4 = new Row();
    row4.add("size", null);
    row4.add("duration", null);
    rows.add(row4);
    
    // Valid row
    Row row5 = new Row();
    row5.add("size", "1MB");
    row5.add("duration", "1s");
    rows.add(row5);
    
    ExecutorContext context = createMockContext();
    
    List<Row> results = directive.execute(rows, context);
    
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals("1.00MB", result.getValue("total_size"));
    Assert.assertEquals("1.00s", result.getValue("total_time"));
  }

  @Test
  public void testDefaultUnits() throws DirectiveParseException, DirectiveExecutionException {
    AggregateStats directive = new AggregateStats();
    Map<String, Object> args = new HashMap<>();
    args.put("byteSizeColumn", new ColumnName("size"));
    args.put("timeDurationColumn", new ColumnName("duration"));
    args.put("totalSizeColumnName", new ColumnName("total_size"));
    args.put("totalTimeColumnName", new ColumnName("total_time"));
    
    directive.initialize(new MockArguments(args));
    
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("size", "1024KB");
    row.add("duration", "1000ms");
    rows.add(row);
    
    ExecutorContext context = createMockContext();
    
    List<Row> results = directive.execute(rows, context);
    
    Assert.assertEquals(1, results.size());
    Row result = results.get(0);
    Assert.assertEquals("1.00MB", result.getValue("total_size")); // Default to MB
    Assert.assertEquals("1.00s", result.getValue("total_time")); // Default to s
  }

  private static class MockArguments implements io.cdap.wrangler.api.Arguments {
    private final Map<String, Object> args;
    
    public MockArguments(Map<String, Object> args) {
      this.args = args;
    }
    
    @Override
    public <T> T value(String name) {
      @SuppressWarnings("unchecked")
      T value = (T) args.get(name);
      return value;
    }
    
    @Override
    public boolean contains(String name) {
      return args.containsKey(name);
    }

    @Override
    public int size() {
      return args.size();
    }

    @Override
    public TokenType type(String name) {
      Object value = args.get(name);
      if (value instanceof ColumnName) {
        return TokenType.COLUMN_NAME;
      } else if (value instanceof Text) {
        return TokenType.TEXT;
      }
      return null;
    }

    @Override
    public int line() {
      return 0;
    }

    @Override
    public int column() {
      return 0;
    }

    @Override
    public String source() {
      return "";
    }

    @Override
    public com.google.gson.JsonElement toJson() {
      JsonObject json = new JsonObject();
      for (Map.Entry<String, Object> entry : args.entrySet()) {
        if (entry.getValue() != null) {
          json.addProperty(entry.getKey(), entry.getValue().toString());
        }
      }
      return json;
    }
  }
} 