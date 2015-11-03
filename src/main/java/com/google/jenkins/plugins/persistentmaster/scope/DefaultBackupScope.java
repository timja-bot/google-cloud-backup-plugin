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

import com.google.common.annotations.VisibleForTesting;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Defines a {@link Scope} containing all files in JENKINS_HOME, excluding
 * various files and directories that do not usually need to be backed up -
 * e.g. temp directories, workspaces.
 */
public class DefaultBackupScope extends ConfigurableScope {
  // The display name for the descriptor.
  @VisibleForTesting
  public static final String DISPLAY_NAME = Messages.DefaultBackupScope_DisplayName();

  @DataBoundConstructor
  public DefaultBackupScope() {}

  @Override
  public String getScopeName() {
    return "Default";
  }

  @Override
  public void addFiles(final Path jenkinsHome, Volume.Creator creator,
      Set<String> existingFileMetadata) throws IOException {
    Set<Path> excludedDirs = new HashSet<>();
    // exclude tmp dirs from build slaves
    excludedDirs.add(jenkinsHome.resolve("container-tmp"));
    excludedDirs.add(jenkinsHome.resolve("garbage"));
    // exclude files and directories used by the backup/restore system
    excludedDirs.add(jenkinsHome.resolve("backup-tmp"));
    excludedDirs.add(jenkinsHome.resolve(".restore.log"));
    // exclude system dirs not used by Jenkins
    excludedDirs.add(jenkinsHome.resolve("lost+found"));
    // exclude the maven repo (gets quite big!)
    excludedDirs.add(jenkinsHome.resolve(".m2"));
    // exclude workspaces
    excludedDirs.add(jenkinsHome.resolve("workspace"));
    // exclude war file contents
    excludedDirs.add(jenkinsHome.resolve("war"));
    // exclude workspaces inside of branches
    excludedDirs.add(jenkinsHome.resolve("jobs/*/branches/*/workspace"));

    Scopes.addAllFilesIn(jenkinsHome, creator, excludedDirs, existingFileMetadata);
  }

  @Override
  public void extractFiles(Path jenkinsHome, Volume.Extractor extractor, boolean overwrite,
      Map<String, Boolean> existingFileMetadataMap) throws IOException {
    Scopes.extractAllFilesTo(jenkinsHome, extractor, overwrite, existingFileMetadataMap);
  }

  /**
   * Descriptor for {@link DefaultBackupScope}.
   */
  @Extension
  public static class DefaultBackupScopeDescriptor extends ConfigurableScopeDescriptor {
    @Override
    public String getDisplayName() {
      return DISPLAY_NAME;
    }
  }
}
