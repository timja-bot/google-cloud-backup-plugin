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
package com.google.jenkins.plugins.cloudbackup.trigger;

import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;

/**
 * Periodically triggers backups, specified by a certain interval provided in
 * the constructor.
 */
public class PeriodicBackupTrigger implements BackupTrigger {

  private final ReadablePeriod interval;

  public PeriodicBackupTrigger(ReadablePeriod interval) {
    this.interval = interval;
  }

  @Override
  public boolean shouldCreateBackup(DateTime lastBackupTime) {
    DateTime nextBackupTime = lastBackupTime.plus(interval);
    return nextBackupTime.isBeforeNow() || nextBackupTime.isEqualNow();
  }

}
