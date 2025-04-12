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

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.cdap.wrangler.api.annotations.PublicEvolving;

import java.io.Serializable;

/**
 * Implementation of {@link Token} for time duration values.
 * This class represents tokens that contain time duration values with units like ns, ms, s, h, d.
 */
@PublicEvolving
public class TimeDurationToken implements Token, Serializable {
  private static final long serialVersionUID = 1L;
  private final long milliseconds;

  public TimeDurationToken(long milliseconds) {
    if (milliseconds < 0) {
      throw new IllegalArgumentException("Time duration cannot be negative");
    }
    this.milliseconds = milliseconds;
  }

  @Override
  public Object value() {
    return milliseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    return new JsonPrimitive(milliseconds);
  }

  /**
   * Returns the time duration value in milliseconds.
   *
   * @return the time duration value in milliseconds
   */
  public long getMilliseconds() {
    return milliseconds;
  }
} 

