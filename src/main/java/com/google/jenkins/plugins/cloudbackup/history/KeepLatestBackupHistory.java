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
package com.google.jenkins.plugins.cloudbackup.history;

import java.io.IOException;
import java.util.List;

import com.google.jenkins.plugins.cloudbackup.storage.Storage;

/**
 * Implementation of {@link BackupHistory} that removes all but the latest
 * backup.
 */
public class KeepLatestBackupHistory implements BackupHistory {

  @Override
  public void processHistoricBackups(Storage storage, String latestBackupName)
      throws IOException {
    List<String> filesInStorage = storage.listFiles();
    for (String filename : filesInStorage) {
      if (!filename.equals(latestBackupName)) {
        storage.deleteFile(filename);
      }
    }
  }

}
