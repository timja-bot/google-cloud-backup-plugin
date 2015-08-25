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
package com.google.jenkins.plugins.persistentmaster;

import org.joda.time.DurationFieldType;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.ReadablePeriod;

import com.google.jenkins.plugins.persistentmaster.history.BackupHistory;
import com.google.jenkins.plugins.persistentmaster.history.KeepLatestBackupHistory;
import com.google.jenkins.plugins.persistentmaster.trigger.BackupTrigger;
import com.google.jenkins.plugins.persistentmaster.trigger.BackupTriggers;
import com.google.jenkins.plugins.persistentmaster.trigger.ConfigFileChangedBackupTrigger;
import com.google.jenkins.plugins.persistentmaster.trigger.FailureBackupTrigger;
import com.google.jenkins.plugins.persistentmaster.trigger.PeriodicBackupTrigger;

/**
 * Jenkins specific module configuration, extending the main module
 * configuration.
 */
public class PersistentMasterJenkinsModule extends PersistentMasterMainModule {

  private ReadablePeriod fullBackupInterval = Hours.hours(1);
  private ReadablePeriod incrementalBackupInterval = Minutes.minutes(3);

  private final BackupHistory backupHistory;
  private final BackupTrigger fullBackupTrigger;
  private final BackupTrigger incrementalBackupTrigger;

  public PersistentMasterJenkinsModule() {
    backupHistory = new KeepLatestBackupHistory();

    // trigger a full backup if any of those triggers fires
    fullBackupTrigger = BackupTriggers.or(
        new FailureBackupTrigger(),
        new PeriodicBackupTrigger(fullBackupInterval));

    incrementalBackupTrigger = BackupTriggers.or(
        new ConfigFileChangedBackupTrigger(),
        new PeriodicBackupTrigger(incrementalBackupInterval));
  }

  public BackupHistory getBackupHistory() {
    return backupHistory;
  }

  public BackupTrigger getFullBackupTrigger() {
    return fullBackupTrigger;
  }

  public BackupTrigger getIncrementalBackupTrigger() {
    return incrementalBackupTrigger;
  }

  /**
   * Sets the full backup interval, in hours.
   *
   * <p>A full backup will be performed if the time since the last full backup
   * exceeds that of the full backup interval. If a backup is in progress, a
   * new backup will not take place.
   */
  public void setFullBackupIntervalHours(int hours) {
    fullBackupInterval = Hours.hours(hours);
  }

  /**
   * Returns the interval that full backups should be performed.
   *
   * @return  number of hours between full backups.
   */
  public int getFullBackupIntervalHours() {
    return fullBackupInterval.get(DurationFieldType.hours());
  }

  /**
   * Sets the incremental backup interval, in minutes.
   *
   * <p>An incremental backup will be performed if the time since the last
   * incremental backup exceeds that of the full backup interval. If a backup
   * is in progress, a new backup will not take place.
   */
  public void setIncrementalBackupIntervalMinutes(int minutes) {
    incrementalBackupInterval = Minutes.minutes(minutes);
  }

  /**
   * Returns the interval that incremental backups should be performed.
   *
   * @return  number of minutes between incremental backups.
   */
  public int getIncrementalBackupIntervalMinutes() {
    return incrementalBackupInterval.get(DurationFieldType.minutes());
  }
}
