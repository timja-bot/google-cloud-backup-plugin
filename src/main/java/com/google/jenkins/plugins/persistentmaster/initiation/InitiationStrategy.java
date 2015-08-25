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
package com.google.jenkins.plugins.persistentmaster.initiation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy for initiating the environment for jenkins.
 */
public interface InitiationStrategy {

  /**
   * This method will be called by the restore procedure if no existing backup
   * could be found, in order to setup a new environment for jenkins.
   *
   * @param jenkinsHome the path for JENKINS_HOME.
   * @throws IOException if initializing the environment fails.
   */
  public void initializeNewEnvironment(Path jenkinsHome) throws IOException;

  /**
   * This method will be called by the restore procedure after the latest
   * backup has been successfully restored.
   *
   * @param jenkinsHome the path for JENKINS_HOME.
   * @param lastBackupId identifier of the loaded backup, e.g. filename.
   * @throws IOException if initializing the environment fails.
   */
  public void initializeRestoredEnvironment(Path jenkinsHome,
      String lastBackupId) throws IOException;

}
