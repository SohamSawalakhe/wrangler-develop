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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link TimeDurationToken} class.
 */
public class TimeDurationTokenTest {

  @Test
  public void testConstructor() {
    TimeDurationToken token = new TimeDurationToken(1000);
    Assert.assertEquals(1000L, token.getMilliseconds());
  }

  @Test
  public void testValue() {
    TimeDurationToken token = new TimeDurationToken(1000);
    Assert.assertEquals(1000L, token.value());
  }

  @Test
  public void testType() {
    TimeDurationToken token = new TimeDurationToken(1000);
    Assert.assertEquals(TokenType.TIME_DURATION, token.type());
  }

  @Test
  public void testToJson() {
    TimeDurationToken token = new TimeDurationToken(1000);
    JsonElement json = token.toJson();
    Assert.assertTrue(json.isJsonPrimitive());
    Assert.assertTrue(json.getAsJsonPrimitive().isNumber());
    Assert.assertEquals(1000L, json.getAsLong());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeMilliseconds() {
    new TimeDurationToken(-1);
  }
}



