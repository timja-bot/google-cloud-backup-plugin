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
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Forwarding class for {@link Volume.Creator}. Allows to wrap an instance of
 * {@link Volume.Creator} to add additional behaviour.
 */
public abstract class ForwardingVolumeCreator implements Volume.Creator {

  private final Volume.Creator creator;

  /**
   * Creates a new {@link Volume.Creator}, forwarding all method calls to the
   * given {@link Volume.Creator} instance.
   *
   * @param creator the instance to which method calls should be forwarded.
   */
  protected ForwardingVolumeCreator(Volume.Creator creator) {
    this.creator = creator;
  }

  @Override
  public void addFile(Path file, String pathInVolume, BasicFileAttributes attrs)
      throws IOException {
    creator.addFile(file, pathInVolume, attrs);
  }

  @Override
  public int getFileCount() {
    return creator.getFileCount();
  }

  @Override
  public void close() throws IOException {
    creator.close();
  }

}
