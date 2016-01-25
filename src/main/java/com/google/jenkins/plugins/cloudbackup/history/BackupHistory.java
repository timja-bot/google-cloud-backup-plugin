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

import com.google.jenkins.plugins.cloudbackup.storage.Storage;

/**
 * Defines a history policy for dealing with historic backups.
 *
 * This class will be called by the backup procedure after a new backup has
 * successfully been created and uploaded to the given storage. An
 * implementation of this class can then decide to remove old backups, or to
 * keep (some of) them.
 */
public interface BackupHistory {

  /**
   * Implements the policy for dealing with historic backups.
   *
   * @param storage the {@link Storage} where all backups reside.
   * @param latestBackupName the filename of the latest backup.
   */
  public void processHistoricBackups(Storage storage, String latestBackupName)
      throws IOException;

}
