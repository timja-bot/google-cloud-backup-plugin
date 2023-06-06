/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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
package com.google.jenkins.plugins.cloudbackup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;

import org.jvnet.hudson.test.JenkinsRule;

import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import com.google.jenkins.plugins.cloudbackup.storage.LocalFileStorageProvider;

/**
 * Jenkins based tests for {@link CloudBackupPlugin}.
 */
public class CloudBackupPluginJenkinsTest {

  @Rule
  public JenkinsRule jenkins = new JenkinsRule();

  @Test
  public void testPluginSectionOnMainConfigPage() throws Exception {
    HtmlPage configPage = jenkins.createWebClient().goTo("configure");
    WebAssert.assertTextPresent(configPage, "Cloud Backup Plugin");
    WebAssert.assertInputPresent(configPage, "enableBackup");
    WebAssert.assertInputPresent(configPage, "fullBackupIntervalHours");
    WebAssert.assertInputPresent(configPage,
        "incrementalBackupIntervalMinutes");
  }

  @Test
  public void testConfigurePlugin() throws Exception {
    HtmlPage configPage = jenkins.createWebClient().goTo("configure");
    HtmlForm form = configPage.getFormByName("config");
    form.getInputByName("enableBackup").setChecked(true);
    form.getInputByName("fullBackupIntervalHours").setValue("50");
    form.getInputByName("incrementalBackupIntervalMinutes")
        .setValue("55");
    jenkins.submit(form);

    CloudBackupPlugin plugin = CloudBackupPlugin.getInstance();
    LocalFileStorageProvider provider =
        (LocalFileStorageProvider) CloudBackupPlugin.getInstance()
            .getStorageProvider();

    assertTrue(plugin.getEnableBackup());
    assertEquals(50, plugin.getFullBackupIntervalHours());
    assertEquals(55, plugin.getIncrementalBackupIntervalMinutes());
    assertEquals(LocalFileStorageProvider.DEFAULT_DIRECTORY,
        provider.getDirectory());
  }

  @Test
  public void testChangingLocationResetsBackupTimings() throws Exception {
    CloudBackupPlugin plugin = CloudBackupPlugin.getInstance();
    assertNull(plugin.getLastBackupTime());
    assertNull(plugin.getLastFullBackupTime());
    assertFalse(plugin.isLastBackupFailed());

    // Give some values to timing parameters.
    plugin.setLastBackupTime(new DateTime(1));
    plugin.setLastFullBackupTime(new DateTime(2));
    plugin.setLastBackupFailed(true);
    assertEquals(1, plugin.getLastBackupTime().getMillis());
    assertEquals(2, plugin.getLastFullBackupTime().getMillis());
    assertTrue(plugin.isLastBackupFailed());

    // Simulate save from user which changes the storage location.
    HtmlPage configPage = jenkins.createWebClient().goTo("configure");
    HtmlForm form = configPage.getFormByName("config");
    form.getInputByName("_.directory").setValue("/some/location");
    jenkins.submit(form);

    LocalFileStorageProvider provider =
        (LocalFileStorageProvider) CloudBackupPlugin.getInstance()
            .getStorageProvider();

    assertEquals("/some/location", provider.getDirectory());
    // The backup timing parameters should be reset.
    assertNull(plugin.getLastBackupTime());
    assertNull(plugin.getLastFullBackupTime());
    assertFalse(plugin.isLastBackupFailed());
  }
}
