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
package com.google.jenkins.plugins.cloudbackup.storage;

import java.io.File;

import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.util.FormValidation;

/**
 * Allows users to configure backups to local disk.
 *
 * Provides an instance of {@link LocalFileStorage} based on user
 * configuration.
 *
 * @author akshayd@google.com (Akshay Dayal)
 */
public class LocalFileStorageProvider extends AbstractStorageProvider {

  // The display name for the descriptor.
  @VisibleForTesting
  public static final String DISPLAY_NAME =
      Messages.LocalFileStorageProvider_DisplayName();

  // The default location where backups are put.
  @VisibleForTesting
  public static final String DEFAULT_DIRECTORY =
      System.getProperty("java.io.tmpdir");

  // Specifies where to store backups. Initialize with a default value.
  private final String directory;

  /**
   * Construct a {@link LocalFileStorageProvider} with default storage
   * location.
   */
  public LocalFileStorageProvider() {
    this(null);
  }

  /**
   * Construct a {@link LocalFileStorageProvider} with specified storage
   * location.
   */
  @DataBoundConstructor
  public LocalFileStorageProvider(final String directory) {
    this.directory = directory == null ? DEFAULT_DIRECTORY : directory;
  }

  /**
   * Get the local directory where backups are stored.
   */
  public String getDirectory() {
    return directory;
  }

  @Override
  public Storage getStorage() {
    return new LocalFileStorage(new File(directory).toPath());
  }

  @Override
  public boolean isValid() {
    return validateDirectory(directory) == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LocalFileStorageProvider that = (LocalFileStorageProvider) o;

    if (directory != null ?
        !directory.equals(that.directory) : that.directory != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(directory);
  }

  @Override
  public String toString() {
    return "LocalFileStorageProvider{" +
        "directory='" + directory + '\'' +
        '}';
  }

  /**
   * Validates a directory is suitable to use for storage.
   *
   * @return A message describing whats wrong with the directory. If there
   * is no issue null is returned.
   */
  @VisibleForTesting
  @Nullable
  protected static String validateDirectory(String value) {
    if (value == null) {
      return Messages.LocalFileStorageProvider_DirectoryRequiredError();
    }

    value = value.trim();
    if (Strings.isNullOrEmpty(value)) {
      return Messages.LocalFileStorageProvider_DirectoryRequiredError();
    }

    File check = new File(value);
    if (!check.exists()) {
      return Messages
          .LocalFileStorageProvider_DirectoryDoesntExistError(value);
    } else if (!check.isDirectory()) {
      return Messages.LocalFileStorageProvider_NotDirectoryError(value);
    } else if (!check.canWrite()) {
      return Messages
          .LocalFileStorageProvider_DirectoryNotWritableError(value);
    }

    return null;
  }

  /**
   * Descriptor for {@link LocalFileStorageProvider}.
   */
  @Extension
  public static class LocalFileStorageProviderDescriptor
      extends StorageProviderDescriptor {

    @Override
    public String getDisplayName() {
      return DISPLAY_NAME;
    }

    public FormValidation doCheckDirectory(@QueryParameter String value) {
      String errorMsg = validateDirectory(value);
      if (errorMsg != null) {
        return FormValidation.error(errorMsg);
      }

      return FormValidation.ok();
    }
  }
}
