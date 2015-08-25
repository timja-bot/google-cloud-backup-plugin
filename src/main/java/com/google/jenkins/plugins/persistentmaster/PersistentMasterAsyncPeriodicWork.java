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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.google.jenkins.plugins.persistentmaster.backup.BackupProcedure;
import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.PeriodicWork;
import hudson.model.TaskListener;

import jenkins.model.Jenkins;

/**
 * Asynchronous scheduled worker that is used to perform the backup procedure.
 *
 * This class uses the {@link AsyncPeriodicWork} extension of Jenkins in order
 * to receive a scheduled call by jenkins every minute. It then checks if a new
 * backup should be created via the trigger extension point. If so, it performs
 * a backup within the current thread.
 */
@Extension
public class PersistentMasterAsyncPeriodicWork extends AsyncPeriodicWork {

  private static final Logger LOGGER = Logger
      .getLogger(PersistentMasterAsyncPeriodicWork.class.getName());

  private static final String WORKER_NAME = "PersistentMaster-Worker";

  public static String getLogFileName() {
    return WORKER_NAME + ".log";
  }

  /**
   * @return the instance of {@link PersistentMasterAsyncPeriodicWork} created
   * by jenkins.
   */
  static PersistentMasterAsyncPeriodicWork getInstance() {
    return Jenkins.getActiveInstance()
        .getExtensionList(AsyncPeriodicWork.class)
        .get(PersistentMasterAsyncPeriodicWork.class);
  }

  public PersistentMasterAsyncPeriodicWork() {
    super(WORKER_NAME);
  }

  @Override
  protected void execute(TaskListener taskListener)
      throws IOException, InterruptedException {
    final PersistentMasterPlugin plugin = PersistentMasterPlugin.getInstance();
    if (plugin == null) {
      throw new IllegalStateException("Plugin instance is not available");
    }
    if (plugin.isLoaded() && plugin.getEnableBackup()) {
      if (shouldCreateFullBackup(plugin)) {
        createBackup(plugin, true);
      } else if (shouldCreateIncrementalBackup(plugin)) {
        createBackup(plugin, false);
      }
    }
  }

  /**
   * Determines if a full backup is necessary. Package-visible to allow
   * direct invocation by {@link PersistentMasterRestartListener}.
   */
  boolean shouldCreateFullBackup(PersistentMasterPlugin plugin) {
    if (plugin.getLastFullBackupTime() == null) {
      return true;  // no full backup available, thus create one
    }
    return plugin.getFullBackupTrigger().shouldCreateBackup(
        plugin.getLastFullBackupTime());
  }

  private boolean shouldCreateIncrementalBackup(PersistentMasterPlugin plugin) {
    return plugin.getIncrementalBackupTrigger()
        .shouldCreateBackup(plugin.getLastBackupTime());
  }

  /**
   * Does the actual work of creating the backup. Package-visible to allow
   * direct invocation by {@link PersistentMasterRestartListener}.
   */
  void createBackup(PersistentMasterPlugin plugin, boolean fullBackup) {
    if (!plugin.beginBackupOrRestore()) {
      return;  // another thread already started a backup
    }
    try {
      try {
        BackupProcedure backupProcedure;
        if (fullBackup) {
          backupProcedure = plugin.getFullBackupProcedure();
        } else {
          backupProcedure = plugin.getIncrementalBackupProcedure();
        }
        DateTime backupTime = backupProcedure.performBackup();
        plugin.setLastBackupTime(backupTime);
        if (fullBackup) {
          plugin.setLastFullBackupTime(backupTime);
        }
        plugin.setLastBackupFailed(false);
      } catch (IOException e) {
        // this will trigger a new backup in the next execution
        plugin.setLastBackupFailed(true);
        LOGGER.log(Level.SEVERE, "IOException while creating backup", e);
      }
    } finally {
      plugin.endBackupOrRestore();
    }
  }

  @Override
  public long getRecurrencePeriod() {
    return PeriodicWork.MIN;  // check for backup triggers every minute
  }

}
