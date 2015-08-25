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

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Descriptor;

import jenkins.model.Jenkins;

/**
 * Defines a {@link Scope} that can be configured from form data and/or config
 * files.
 */
public abstract class ConfigurableScope
    implements Scope, Describable<ConfigurableScope>, ExtensionPoint {

  /**
   * Get the list of getAllDescriptors registered scopes.
   */
  public static DescriptorExtensionList<ConfigurableScope,
      ConfigurableScopeDescriptor> getAllDescriptors() {
    return Jenkins.getActiveInstance()
        .<ConfigurableScope, ConfigurableScopeDescriptor>
        getDescriptorList(ConfigurableScope.class);
  }

  /**
   * Get the descriptor for the scope.
   */
  public ConfigurableScopeDescriptor getDescriptor() {
    return (ConfigurableScopeDescriptor) Jenkins.getActiveInstance()
        .getDescriptor(getClass());
  }

  /**
   * @return a name for this scope. This is used both for display in the
   * Jenkins UI and in the backup file structure; thus, it should be both
   * human-readable and acceptable as a directory name.
   */
  public abstract String getScopeName();

  /**
   * The {@link Descriptor} for the scope.
   */
  public abstract static class ConfigurableScopeDescriptor
      extends Descriptor<ConfigurableScope> {

    protected ConfigurableScopeDescriptor() {}

    protected ConfigurableScopeDescriptor(
        Class<? extends ConfigurableScope> clazz) {
      super(clazz);
    }

  }
}
