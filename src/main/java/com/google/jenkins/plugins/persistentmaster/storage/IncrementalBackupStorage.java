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
import java.util.List;

/**
 * Storage wrapper for handling incremental backups.
 *
 * Adds the filenames of the current backup to the list of existing backup
 * filenames, rather then overwriting them.
 */
public class IncrementalBackupStorage extends ForwardingStorage {

  public IncrementalBackupStorage(Storage storage) {
    super(storage);
  }

  @Override
  public void updateLastBackup(List<String> filenames) throws IOException {
    List<String> latestBackupFilenames = super.findLatestBackup();
    if (latestBackupFilenames != null) {
      latestBackupFilenames.addAll(filenames);
    } else {
      latestBackupFilenames = filenames;
    }
    super.updateLastBackup(latestBackupFilenames);
  }

}
