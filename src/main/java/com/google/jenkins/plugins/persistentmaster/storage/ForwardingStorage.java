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
package com.google.jenkins.plugins.persistentmaster.storage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Forwarding class for {@link Storage}. Allows to wrap an instance of
 * {@link Storage} to add additional behaviour.
 */
public abstract class ForwardingStorage implements Storage {

  private final Storage storage;

  protected ForwardingStorage(Storage storage) {
    this.storage = storage;
  }

  @Override
  public void storeFile(Path localFile, String filename) throws IOException {
    storage.storeFile(localFile, filename);
  }

  @Override
  public void loadFile(String filename, Path target) throws IOException {
    storage.loadFile(filename, target);
  }

  @Override
  public void deleteFile(String filename) throws IOException {
    storage.deleteFile(filename);
  }

  @Override
  public List<String> listFiles() throws IOException {
    return storage.listFiles();
  }

  @Override
  public List<String> findLatestBackup() throws IOException {
    return storage.findLatestBackup();
  }

  @Override
  public void updateLastBackup(List<String> filenames) throws IOException {
    storage.updateLastBackup(filenames);
  }

}
