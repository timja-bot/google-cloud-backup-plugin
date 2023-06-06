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

import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;

import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import com.google.jenkins.plugins.cloudbackup.CloudBackupPlugin;
import hudson.DescriptorExtensionList;

/**
 * Jenkins based tests for {@link GcloudGcsStorageProvider}.
 *
 * These tests are done in a separate class because {@link JenkinsRule} does
 * not work with PowerMock.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
public class GcloudGcsStorageProviderJenkinsTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  private static final String BUCKET = "bucket";

  @Test
  public void testProviderAvailable() {
    DescriptorExtensionList<AbstractStorageProvider,
        AbstractStorageProvider.StorageProviderDescriptor> allDescriptors =
        AbstractStorageProvider.getAllDescriptors();
    assertEquals(GcloudGcsStorageProvider.DISPLAY_NAME,
        allDescriptors.find(GcloudGcsStorageProvider.class).getDisplayName());
  }

  @Test
  public void testConfigureProvider() throws Exception {
    GcloudGcsStorage expectedStorage = new GcloudGcsStorage(BUCKET);

    HtmlPage configPage = jenkins.createWebClient().goTo("configure");
    HtmlForm form = configPage.getFormByName("config");
    HtmlOption option = TestUtils.findOptionWithText(
        form, GcloudGcsStorageProvider.DISPLAY_NAME);
    option.setSelected(true);
    form.getInputByName("_.bucket").setValue(BUCKET);
    jenkins.submit(form);

    GcloudGcsStorageProvider provider =
        (GcloudGcsStorageProvider) CloudBackupPlugin.getInstance()
            .getStorageProvider();

    assertEquals(BUCKET, provider.getBucket());
    assertEquals(expectedStorage, provider.getStorage());
  }
}
