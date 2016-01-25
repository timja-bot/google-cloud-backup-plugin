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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

/**
 * Provides a {@link Storage} instance that can be used to perform backups.
 *
 * Different providers can be registered which would allow users to select the
 * provider and configure it in the global configuration page.
 *
 * @author akshayd@google.com (Akshay Dayal)
 */
public abstract class AbstractStorageProvider implements
    Describable<AbstractStorageProvider>, ExtensionPoint {

  /**
   * Get the list of getAllDescriptors registered providers.
   */
  public static DescriptorExtensionList<AbstractStorageProvider,
      StorageProviderDescriptor> getAllDescriptors() {
    return Jenkins.getActiveInstance()
        .<AbstractStorageProvider, StorageProviderDescriptor>
        getDescriptorList(AbstractStorageProvider.class);
  }

  /**
   * Get the descriptor for the provider.
   */
  public StorageProviderDescriptor getDescriptor() {
    return (StorageProviderDescriptor) Jenkins.getActiveInstance()
        .getDescriptor(getClass());
  }

  /**
   * Get the {@link Storage} instance based on user configuration.
   */
  public abstract Storage getStorage();

  /**
   * Whether or not the user entered all configuration correctly.
   */
  public abstract boolean isValid();

  /**
   * All storage providers must implement an equals method that does more
   * than just compare object references.
   */
  @Override
  public abstract boolean equals(Object other);

  @Override
  public abstract int hashCode();

  /**
   * The {@link Descriptor} for the provider.
   */
  public abstract static class StorageProviderDescriptor
      extends Descriptor<AbstractStorageProvider> {

    protected StorageProviderDescriptor() {}

    protected StorageProviderDescriptor(
        Class<? extends AbstractStorageProvider> clazz) {
      super(clazz);
    }

  }
}
