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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import hudson.Extension;
import hudson.XmlFile;
import hudson.model.Saveable;
import hudson.model.listeners.SaveableListener;

import jenkins.model.Jenkins;

/**
 * {@link SaveableListener} implementation of the persistent master plugin used
 * to detect changes to XML config files.
 */
@Extension
public class PersistentMasterSaveableListener extends SaveableListener {

  /**
   * Returns the instance of this extension point created by jenkins.
   *
   * @return the instance of this extension point created by jenkins.
   */
  public static PersistentMasterSaveableListener getInstance() {
    return Jenkins.getActiveInstance()
        .getExtensionList(SaveableListener.class)
        .get(PersistentMasterSaveableListener.class);
  }

  private DateTime mostRecentConfigFileChangeTime;

  /**
   * Creates a new instance, will be called by Jenkins upon startup.
   */
  public PersistentMasterSaveableListener() {
  }

  @Override
  public void onChange(Saveable o, XmlFile file) {
    mostRecentConfigFileChangeTime = new DateTime(DateTimeZone.UTC);
  }

  /**
   * Returns the time of the most recent XML config file change.
   *
   * @return the time of the most recent XML config file change.
   */
  public DateTime getMostRecentConfigFileChangeTime() {
    return mostRecentConfigFileChangeTime;
  }

}
