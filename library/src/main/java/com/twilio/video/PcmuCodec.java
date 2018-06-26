/*
 * Copyright (C) 2018 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twilio.video;

/**
 * ITU-T standard for audio companding.
 *
 * @see <a href="https://en.wikipedia.org/wiki/G.711">PCMU</a>
 */
public class PcmuCodec extends AudioCodec {
  public static final String NAME = "PCMU";

  public PcmuCodec() {
    super(NAME);
  }
}
