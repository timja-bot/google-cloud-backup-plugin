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

import static hudson.init.InitMilestone.EXTENSIONS_AUGMENTED;

import com.google.jenkins.plugins.cloudbackup.CloudBackupMainModule;
import com.google.jenkins.plugins.cloudbackup.CloudBackupPlugin;
import com.google.jenkins.plugins.cloudbackup.restore.RestoreProcedure;

import hudson.init.Initializer;

/**
 * Automatically restores data from the backup location configured with the
 * backup plugin.
 */
public class AutoRestoreInitializer {
  private static final Logger logger = Logger.getLogger(
      AutoRestoreInitializer.class.getName());

  @Initializer(after = EXTENSIONS_AUGMENTED)
  public static void init() throws IOException {
    logger.info("Checking if backups need to be restored.");

    CloudBackupPlugin plugin = CloudBackupPlugin.getInstance();
    if (plugin == null) {
      logRestoreFailure("No cloud backup plugin found.");
      return;
    }

    try {
      CloudBackupMainModule module
          = plugin.getCloudBackupMainModule();
      if (module == null) {
        logRestoreFailure("No cloud backup plugin configuration found.");
        return;
      }

      if (!plugin.getEnableAutoRestore()) {
        logger.info("Automatic restores are disabled.");
        return;
      }

      if (module.getVolume() == null || module.getScope() == null
          || module.getStorage() == null) {
        logRestoreFailure("Cloud backup plugin configuration did not "
            + "specify a usable backup storage provider to restore from.");
        return;
      }

      final Path jenkinsHomePath = plugin.calculateJenkinsHome();
      final Path scratchDirectory = plugin.getScratchDirectory();

      if (jenkinsHomePath == null) {
        logRestoreFailure("Cloud backup plugin could not locate Jenkins"
            + " home directory.");
        return;
      }

      synchronized (RestoreLog.getLock()) {
        RestoreProcedure restoreProcedure = new RestoreProcedure(
            module.getVolume(), module.getScope(), module.getStorage(),
            new RestartAfterRestoreStrategy(new RestoreLog(
                jenkinsHomePath)), jenkinsHomePath, scratchDirectory,
            plugin.getRestoreOverwritesData());
        try {
          restoreProcedure.performRestore();
        } catch (IOException | RuntimeException e) {
          throw new IllegalStateException(
              "Could not restore and initialize jenkins", e);
        }
      }
    } finally {
      plugin.endBackupOrRestore();
    }
  }

  private static void logRestoreFailure(String message) {
    logger.warning("Cannot restore from backup: " + message);
  }
}
