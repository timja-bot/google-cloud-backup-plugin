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

import com.google.jenkins.plugins.persistentmaster.volume.ForwardingVolumeCreator;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Set;


/**
 * An incremental scope only adds files that have been modified since the last
 * backup time to the volume.
 *
 * To do that it wraps another scope to define which files should be considered
 * for inclusion, and then checks the last modified time of every file before
 * including it in the volume.
 */
public class IncrementalScope extends ForwardingScope {
  private final FileTime lastBackupTime;

  /**
   * Creates an incremental scope for the given scope, only including files that
   * were modified after the given last backup time.
   *
   * @param scope the original scope to wrap.
   * @param lastBackupTime the time when the last backup was performed.
   */
  public IncrementalScope(Scope scope, FileTime lastBackupTime) {
    super(scope);
    this.lastBackupTime = lastBackupTime;
  }

  @Override
  public void addFiles(Path jenkinsHome, Volume.Creator creator,
      final Set<String> existingFileMetadata) throws IOException {
    super.addFiles(jenkinsHome, new ForwardingVolumeCreator(creator) {
      @Override
      public void addFile(Path file, String pathInVolume, BasicFileAttributes attrs)
          throws IOException {
        // add file only if it has been modified since the last backup
        if (attrs.lastModifiedTime().compareTo(lastBackupTime) > 0) {
          super.addFile(file, pathInVolume, attrs);
        }
      }
    },
        existingFileMetadata);
  }
}
