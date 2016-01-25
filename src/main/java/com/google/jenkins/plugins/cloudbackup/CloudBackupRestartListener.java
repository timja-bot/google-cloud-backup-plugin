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
package com.google.jenkins.plugins.cloudbackup;

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.RestartListener;

import jenkins.model.Jenkins;

/**
 * Invokes a final backup when
 */
@Extension
public class CloudBackupRestartListener extends RestartListener {

  private static final Logger LOGGER = Logger
      .getLogger(CloudBackupRestartListener.class.getName());

  private CloudBackupPlugin plugin = null;
  private CloudBackupAsyncPeriodicWork work = null;

  /**
   * @return the instance of this extension point created by jenkins.
   */
  public static CloudBackupRestartListener getInstance() {
    return Jenkins.getActiveInstance()
        .getExtensionList(RestartListener.class)
        .get(CloudBackupRestartListener.class);
  }

  /**
   * Creates a new instance, will be called by Jenkins upon startup.
   */
  public CloudBackupRestartListener() {
  }

  @Override
  public void onRestart() {
    plugin = CloudBackupPlugin.getInstance();
    if (plugin == null) {
      LOGGER.warning("Cloud Backup Plugin instance missing when "
          + "attempting backup at restart time.");
      return;
    }
    work = CloudBackupAsyncPeriodicWork.getInstance();
    if (work == null) {
      LOGGER.warning("Cloud Backup Worker instance missing when "
          + "attempting backup at restart time.");
      return;
    }
    if (plugin.isLoaded() && plugin.getEnableBackup()
        && !plugin.isSkipBackupOnNextRestart()) {
      work.createBackup(plugin, work.shouldCreateFullBackup(plugin));
    }
  }

  @Override
  public boolean isReadyToRestart() {
    return true;
  }
}
