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
 * Tests for {@link ByteSizeToken} class.
 */
public class ByteSizeTokenTest {

  @Test
  public void testConstructor() {
    ByteSizeToken token = new ByteSizeToken(1024);
    Assert.assertEquals(1024L, token.getBytes());
  }

  @Test
  public void testValue() {
    ByteSizeToken token = new ByteSizeToken(1024);
    Assert.assertEquals(1024L, token.value());
  }

  @Test
  public void testType() {
    ByteSizeToken token = new ByteSizeToken(1024);
    Assert.assertEquals(TokenType.BYTE_SIZE, token.type());
  }

  @Test
  public void testToJson() {
    ByteSizeToken token = new ByteSizeToken(1024);
    JsonElement json = token.toJson();
    Assert.assertTrue(json.isJsonPrimitive());
    Assert.assertTrue(json.getAsJsonPrimitive().isNumber());
    Assert.assertEquals(1024L, json.getAsLong());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNegativeBytes() {
    new ByteSizeToken(-1);
  }
}
