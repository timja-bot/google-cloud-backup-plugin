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
package com.google.jenkins.plugins.persistentmaster.trigger;

import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.jenkins.plugins.persistentmaster.PersistentMasterSaveableListener;
import hudson.ExtensionList;
import hudson.model.listeners.SaveableListener;
import jenkins.model.Jenkins;

/**
 * Tests for {@link ConfigFileChangedBackupTrigger}.
 */
public class ConfigFileChangedBackupTriggerTest {

  @Mock private Jenkins jenkins;
  @Mock private ExtensionList<SaveableListener> extensionList;
  @Mock private PersistentMasterSaveableListener saveableListener;

  private ConfigFileChangedBackupTrigger trigger;
  private DateTime now;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    setUpJenkinsInstance();
    when(jenkins.getExtensionList(eq(SaveableListener.class)))
        .thenReturn(extensionList);
    when(extensionList.get(eq(PersistentMasterSaveableListener.class)))
        .thenReturn(saveableListener);

    now = new DateTime(DateTimeZone.UTC);
    trigger = new ConfigFileChangedBackupTrigger();
  }

  private void setUpJenkinsInstance()
      throws NoSuchFieldException, IllegalAccessException {
    // make Jenkins.getInstance() return the mock
    Field theInstance = Jenkins.class.getDeclaredField("theInstance");
    theInstance.setAccessible(true);
    theInstance.set(Jenkins.class, jenkins);
  }

  @After
  public void tearDown() throws Exception {
    trigger = null;
    now = null;
  }

  @Test
  public void testLatestChangeNow_latestBackup1MinAgo_shouldReturnTrue()
      throws Exception {
    when(saveableListener.getMostRecentConfigFileChangeTime()).thenReturn(now);
    assertTrue(trigger.shouldCreateBackup(now.minusMinutes(1)));
    verify(saveableListener).getMostRecentConfigFileChangeTime();
  }

  @Test
  public void testLatestChange1MinAgo_latestBackupNow_shouldReturnFalse()
      throws Exception {
    when(saveableListener.getMostRecentConfigFileChangeTime())
        .thenReturn(now.minusMinutes(1));
    assertFalse(trigger.shouldCreateBackup(now));
    verify(saveableListener).getMostRecentConfigFileChangeTime();
  }

  @Test
  public void testLatestChangeNotAvailable_latestBackupNow_shouldReturnFalse()
      throws Exception {
    when(saveableListener.getMostRecentConfigFileChangeTime()).thenReturn(null);
    assertFalse(trigger.shouldCreateBackup(now));
    verify(saveableListener).getMostRecentConfigFileChangeTime();
  }

}
