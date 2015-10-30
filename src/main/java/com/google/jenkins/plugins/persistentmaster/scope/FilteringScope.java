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
import com.google.jenkins.plugins.persistentmaster.volume.Volume.Creator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * A scope implementation that allows filtering out special files that would
 * otherwise be added by the given scope.
 */
public class FilteringScope extends ForwardingScope {
  private final Set<String> exclusions = new HashSet<>();

  public FilteringScope(Scope scope) {
    super(scope);
  }

  public void addExclusion(String exclusion) {
    exclusions.add(exclusion);
  }

  @Override
  public void addFiles(Path jenkinsHome, Creator creator, List<String> existingFileMetadata)
      throws IOException {
    super.addFiles(jenkinsHome, new ForwardingVolumeCreator(creator) {
      @Override
      public void addFile(Path file, String pathInVolume, BasicFileAttributes attrs)
          throws IOException {
        if (!exclusions.contains(pathInVolume)) {
          super.addFile(file, pathInVolume, attrs);
        }
      }
    },
        existingFileMetadata);
  }
}
