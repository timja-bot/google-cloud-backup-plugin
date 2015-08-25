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
package com.google.jenkins.plugins.persistentmaster;

import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.RestartListener;

import jenkins.model.Jenkins;

/**
 * Invokes a final backup when
 */
@Extension
public class PersistentMasterRestartListener extends RestartListener {

  private static final Logger LOGGER = Logger
      .getLogger(PersistentMasterRestartListener.class.getName());

  private PersistentMasterPlugin plugin = null;
  private PersistentMasterAsyncPeriodicWork work = null;

  /**
   * @return the instance of this extension point created by jenkins.
   */
  public static PersistentMasterRestartListener getInstance() {
    return Jenkins.getActiveInstance()
        .getExtensionList(RestartListener.class)
        .get(PersistentMasterRestartListener.class);
  }

  /**
   * Creates a new instance, will be called by Jenkins upon startup.
   */
  public PersistentMasterRestartListener() {
  }

  @Override
  public void onRestart() {
    plugin = PersistentMasterPlugin.getInstance();
    if (plugin == null) {
      LOGGER.warning("Persistent Master Plugin instance missing when "
          + "attempting backup at restart time.");
      return;
    }
    work = PersistentMasterAsyncPeriodicWork.getInstance();
    if (work == null) {
      LOGGER.warning("Persistent Master Backup Worker instance missing when "
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
