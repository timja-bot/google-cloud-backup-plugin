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
package com.google.jenkins.plugins.persistentmaster.volume;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Forwarding class for {@link Volume.Entry}. Allows to wrap an instance of
 * {@link Volume.Entry} to add additional behaviour.
 */
public abstract class ForwardingVolumeEntry implements Volume.Entry {

  private final Volume.Entry entry;

  /**
   * Creates a new {@link Volume.Entry}, forwarding all method calls to the
   * given {@link Volume.Entry} instance.
   *
   * @param entry the instance to which method calls should be forwarded.
   */
  protected ForwardingVolumeEntry(Volume.Entry entry) {
    this.entry = entry;
  }

  @Override
  public String getName() {
    return entry.getName();
  }

  @Override
  public boolean isDirectory() {
    return entry.isDirectory();
  }

  @Override
  public boolean isSymlink() {
    return entry.isSymlink();
  }
  
  @Override
  public void extractTo(Path target, boolean overwrite) throws IOException {
    entry.extractTo(target, overwrite);
  }

}
