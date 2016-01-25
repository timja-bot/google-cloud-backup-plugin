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
package com.google.jenkins.plugins.cloudbackup.scope;

import com.google.jenkins.plugins.cloudbackup.volume.Volume.Creator;
import com.google.jenkins.plugins.cloudbackup.volume.Volume.Extractor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * Forwarding class for {@link Scope}. Allows wrapping an instance of
 * {@link Scope} to add additional behaviour.
 */
public abstract class ForwardingScope implements Scope {
  private final Scope scope;

  protected ForwardingScope(Scope scope) {
    this.scope = scope;
  }

  @Override
  public void addFiles(Path jenkinsHome, Creator creator, Set<String> existingFileMetadata)
      throws IOException {
    scope.addFiles(jenkinsHome, creator, existingFileMetadata);
  }

  @Override
  public void extractFiles(Path jenkinsHome, Extractor extractor, boolean overwrite,
      Map<String, Boolean> existingFileMetadataMap) throws IOException {
    scope.extractFiles(jenkinsHome, extractor, overwrite, existingFileMetadataMap);
  }
}
