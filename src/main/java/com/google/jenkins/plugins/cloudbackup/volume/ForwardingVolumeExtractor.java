/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jenkins.plugins.cloudbackup.volume;

import java.io.IOException;
import java.util.Iterator;

/**
 * Forwarding class for {@link Volume.Extractor}. Allows to wrap an instance of
 * {@link Volume.Extractor} to add additional behaviour.
 */
public abstract class ForwardingVolumeExtractor implements Volume.Extractor {

  private final Volume.Extractor extractor;

  /**
   * Creates a new {@link Volume.Extractor}, forwarding all method calls to the
   * given {@link Volume.Extractor} instance.
   *
   * @param extractor the instance to which method calls should be forwarded.
   */
  protected ForwardingVolumeExtractor(Volume.Extractor extractor) {
    this.extractor = extractor;
  }

  @Override
  public Iterator<Volume.Entry> iterator() {
    return extractor.iterator();
  }

  @Override
  public void close() throws IOException {
    extractor.close();
  }

}
