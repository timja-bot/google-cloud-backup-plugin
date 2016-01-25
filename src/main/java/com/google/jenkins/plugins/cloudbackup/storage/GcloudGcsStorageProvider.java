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

import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.util.FormValidation;

/**
 * Allows users to configure a {@link GcloudGcsFileStorage} and provides an
 * instance of it.
 *
 * Users can specify which directory backups should be stored in.
 *
 * @author akshayd@google.com (Akshay Dayal)
 */
public class GcloudGcsStorageProvider extends AbstractStorageProvider {

  // The display name for the descriptor.
  @VisibleForTesting
  public static final String DISPLAY_NAME =
      Messages.GcloudGcsStorageProvider_DisplayName();

  private final String bucket;

  @DataBoundConstructor
  public GcloudGcsStorageProvider(final String bucket) {
    this.bucket = bucket;
  }

  public String getBucket() {
    return bucket;
  }

  @Override
  public Storage getStorage() {
    return new GcloudGcsStorage(bucket);
  }

  @Override
  public boolean isValid() {
    return validateBucket(bucket) == null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GcloudGcsStorageProvider that = (GcloudGcsStorageProvider) o;

    if (bucket != null ?
        !bucket.equals(that.bucket) : that.bucket != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(bucket);
  }

  @Override
  public String toString() {
    return "GcloudGcsStorageProvider{" +
        "bucket='" + bucket + '\'' +
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
  public static String validateBucket(String value) {
    if (value == null) {
      return Messages.GcloudGcsStorageProvider_BucketRequiredError();
    }

    value = value.trim();
    if (Strings.isNullOrEmpty(value)) {
      return Messages.GcloudGcsStorageProvider_BucketRequiredError();
    }

    return null;
  }

  /**
   * Descriptor for {@link GcloudGcsStorageProvider}.
   */
  @Extension
  public static class GcloudGcsStorageProviderDescriptor
      extends StorageProviderDescriptor {

    @Override
    public String getDisplayName() {
      return DISPLAY_NAME;
    }

    public FormValidation doCheckBucket(@QueryParameter String value) {
      String errorMsg = validateBucket(value);
      if (errorMsg != null) {
        return FormValidation.error(errorMsg);
      }

      return FormValidation.ok();
    }
  }
}
