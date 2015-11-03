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
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Defines a user-configured {@link Scope}.
 */
public class CustomScope extends ConfigurableScope {

  // The display name for the descriptor.
  @VisibleForTesting
  public static final String DISPLAY_NAME = Messages.CustomScope_DisplayName();
  @VisibleForTesting
  public static final String EXCLUDE_DISPLAY_NAME =
      Messages.CustomScope_ExcludeDisplayName();

  private final String filepath;
  private final String scopeName;
  private final List<Exclude> excludedFilepaths;

  @DataBoundConstructor
  public CustomScope(final String filepath, final String scopeName,
      final List<Exclude> excludedFilepaths) {
    this.filepath = filepath;
    this.scopeName = scopeName;
    this.excludedFilepaths = excludedFilepaths == null ?
        new ArrayList<Exclude>() :
        excludedFilepaths;
  }

  public String getFilepath() {
    return filepath;
  }

  @Override
  public String getScopeName() {
    return scopeName;
  }

  public List<Exclude> getExcludedFilepaths() {
    return excludedFilepaths;
  }

  @Override
  public void addFiles(final Path jenkinsHome, Volume.Creator creator, Set<String> existingFileMetadata)
      throws IOException {
    Set<Path> excludedDirs = new HashSet<>();
    Path basePath = jenkinsHome.resolve(filepath);
    if (excludedFilepaths != null) {
      for (Exclude exclude : excludedFilepaths) {
        excludedDirs.add(basePath.resolve(exclude.getExcludedFilepath()));
      }
    }
    Scopes.addAllFilesIn(basePath, creator, excludedDirs, existingFileMetadata);
  }

  @Override
  public void extractFiles(Path jenkinsHome, Volume.Extractor extractor, boolean overwrite, 
      Map<String, Boolean> existingFileMetadataMap) throws IOException {
    Scopes.extractAllFilesTo(jenkinsHome.resolve(filepath), extractor, overwrite, existingFileMetadataMap);
  }

  /**
   * Descriptor for {@link CustomScope}.
   */
  @Extension
  public static class CustomScopeDescriptor
      extends ConfigurableScopeDescriptor {

    @Override
    public String getDisplayName() {
      return DISPLAY_NAME;
    }
  }

  /**
   * Wrapper class for excluded filepaths.
   */
  public static class Exclude implements Describable<Exclude> {
    private String excludedFilepath;

    @DataBoundConstructor
    public Exclude(String excludedFilepath) {
      this.excludedFilepath = excludedFilepath;
    }

    public void setExcludedFilepath(String excludedFilepath) {
      this.excludedFilepath = excludedFilepath;
    }

    public String getExcludedFilepath() {
      return excludedFilepath;
    }

    public DescriptorImpl getDescriptor() {
      return (DescriptorImpl) Jenkins.getActiveInstance()
        .getDescriptor(getClass());
    }

    /**
     * Descriptor for {@link Exclude}.
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<Exclude> {

      @Override
      public String getDisplayName() {
        return EXCLUDE_DISPLAY_NAME;
      }
    }
  }
}
