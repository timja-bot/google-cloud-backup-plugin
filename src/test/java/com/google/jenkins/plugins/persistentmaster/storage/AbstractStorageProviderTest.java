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
package com.google.jenkins.plugins.persistentmaster.storage;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.DescriptorExtensionList;
import net.sf.json.JSONObject;

/**
 * Tests for {@link AbstractStorageProvider}.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
public class AbstractStorageProviderTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void testGetAllDescriptorsHasRegisteredProviders() {
    DescriptorExtensionList<AbstractStorageProvider,
        AbstractStorageProvider.StorageProviderDescriptor> allDescriptors =
        AbstractStorageProvider.getAllDescriptors();
    assertEquals("Fake Storage",
        allDescriptors.find(FakeStorageProvider.class).getDisplayName());
  }

  @Test
  public void testGetProviderThroughDescriptor() throws Exception {
    FakeStorageProvider provider =
        (FakeStorageProvider) AbstractStorageProvider.getAllDescriptors()
        .find(FakeStorageProvider.class).newInstance(null, new JSONObject());
    assertEquals("Fake provider", provider.toString());
  }

  /**
   * Fake provider for testing.
   */
  public static class FakeStorageProvider extends AbstractStorageProvider {

    @DataBoundConstructor
    public FakeStorageProvider() {}

    @Override
    public Storage getStorage() {
      return null;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public boolean equals(Object o) {
      return true;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public String toString() {
      return "Fake provider";
    }

    /**
     * Descriptor for {@link FakeStorageProvider}.
     */
    @TestExtension
    public static class FakeStorageProviderDescriptor
        extends StorageProviderDescriptor {

      @Override
      public String getDisplayName() {
        return "Fake Storage";
      }
    }
  }
}
