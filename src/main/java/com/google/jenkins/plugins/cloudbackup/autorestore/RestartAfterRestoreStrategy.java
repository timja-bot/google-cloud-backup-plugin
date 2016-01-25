/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.jenkins.plugins.cloudbackup.autorestore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import com.google.common.base.Preconditions;
import com.google.jenkins.plugins.cloudbackup.CloudBackupPlugin;
import com.google.jenkins.plugins.cloudbackup.initiation.InitiationStrategy;
import hudson.lifecycle.RestartNotSupportedException;

import jenkins.model.Jenkins;

/**
 * Initiation strategy that restarts Jenkins after a
 * configuration has been restored from backups. Uses a log file to track
 * restores. The last backup ID stored in the log file is compared to
 * the just-loaded backup ID to determine if a restart is necessary.
 */
public class RestartAfterRestoreStrategy implements InitiationStrategy {

  private static final String SUCCESS_MESSAGE
      = " successfully restored at time: ";
  private static final String RESTORE_LOG_FILENAME = ".restore.log";

  private static final Logger logger =
      Logger.getLogger(RestartAfterRestoreStrategy.class.getName());

  private final RestoreLog restoreLog;

  public RestartAfterRestoreStrategy(RestoreLog restoreLog) {
    this.restoreLog = restoreLog;
  }

  @Override
  public void initializeNewEnvironment(Path jenkinsHome) throws IOException {
    logger.info("Initialized new environment.");
  }

  @Override
  public void initializeRestoredEnvironment(Path jenkinsHome,
      String lastBackupId) throws IOException {
    Preconditions.checkNotNull(lastBackupId);

    logger.fine("Checking if restart is needed after restoring backup: "
        + lastBackupId);

    String loggedBackupId = restoreLog.getLastBackupId();

    if (lastBackupId.equals(loggedBackupId)) {
      logger.info("Jenkins successfully restored from backup.");
      return;
    }

    if (loggedBackupId != null) {
      logger.fine("Last restored backup id: " + lastBackupId
          + " does not match last backup id in restore log: " + loggedBackupId);
      logger.fine("Writing new backup id and restarting Jenkins.");
    } else {
      logger.fine("Writing new backup id: " + lastBackupId
          + " and restarting Jenkins.");
    }

    restoreLog.writeLastBackupId(lastBackupId);
    logger.info("Restored data from backup: " + lastBackupId
        + "; restarting Jenkins.");
    CloudBackupPlugin plugin = CloudBackupPlugin.getInstance();
    try {
      if (plugin != null) {
        plugin.setSkipBackupOnNextRestart(true);
      }
      Jenkins.getActiveInstance().restart();
    } catch (RestartNotSupportedException e) {
      if (plugin != null) {
        plugin.setSkipBackupOnNextRestart(false);
      }
      logger.warning("Could not restart Jenkins after restoring backup: "
          + lastBackupId);
    }
  }
}
