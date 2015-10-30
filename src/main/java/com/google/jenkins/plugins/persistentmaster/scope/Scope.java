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
package com.google.jenkins.plugins.persistentmaster.scope;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * A Scope defines a set of files to add to a {@link Volume} and provides
 * methods for adding and extracting them to the correct target path.
 */
public interface Scope {
  /**
   * Add all files from this scope to the Volume via the given {@link
   * Volume.Creator}.
   *
   * @param jenkinsHome path to JENKINS_HOME.
   * @param creator the {@link Volume.Creator} of the volume that the files
   * should be added to.
   * @throws IOException if some file operation fails.
   */
  public void addFiles(Path jenkinsHome, Volume.Creator creator, List<String> existingFileNames)
      throws IOException;

  /**
   * Extract all files in this scope from the Volume via the given {@link
   * Volume.Extractor} to the respective target path.
   *
   * @param jenkinsHome path to JENKINS_HOME.
   * @param extractor the {@link Volume.Extractor} of the volume which should be
   * extracted.
   * @param overwrite whether the operation should overwrite existing files
   * when a conflict is detected.
   * @throws IOException if some file operation fails.
   */
  public void extractFiles(Path jenkinsHome, Volume.Extractor extractor,
      boolean overwrite, List<String> existingFileMetadata) throws IOException;
}
