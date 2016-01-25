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
package com.google.jenkins.plugins.cloudbackup;

import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.jenkins.plugins.cloudbackup.backup.BackupProcedure;
import com.google.jenkins.plugins.cloudbackup.trigger.BackupTrigger;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

/**
 * Tests for {@link CloudBackupAsyncPeriodicWork}.
 */
public class CloudBackupAsyncPeriodicWorkTest {

  @Mock private Jenkins jenkins;
  @Mock private CloudBackupPlugin plugin;
  @Mock private TaskListener taskListener;
  @Mock private BackupProcedure backupProcedure;
  @Mock private BackupTrigger backupTriggerTrue;
  @Mock private BackupTrigger backupTriggerFalse;

  private CloudBackupAsyncPeriodicWork periodicWork;
  private DateTime now;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    setUpJenkinsInstance();
    when(jenkins.getPlugin(eq(CloudBackupPlugin.class)))
        .thenReturn(plugin);

    periodicWork = new CloudBackupAsyncPeriodicWork();
    now = new DateTime(DateTimeZone.UTC);

    when(backupTriggerTrue.shouldCreateBackup(eq(now))).thenReturn(true);
    when(backupTriggerFalse.shouldCreateBackup(eq(now))).thenReturn(false);

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
    periodicWork = null;
    now = null;
  }

  @Test(expected = IllegalStateException.class)
  public void test_noPluginInstance_shouldThrowException() throws Exception {
    when(jenkins.getPlugin(eq(CloudBackupPlugin.class))).thenReturn(null);
    periodicWork.execute(taskListener);
  }

  @Test
  public void testBackupConditionsMet_shouldCreateBackup() throws Exception {
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(true);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastFullBackupTime()).thenReturn(null);
    when(plugin.beginBackupOrRestore()).thenReturn(true);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerTrue);
    when(backupProcedure.performBackup()).thenReturn(backupTime);

    periodicWork.execute(taskListener);

    verify(plugin).getLastFullBackupTime();
    verify(plugin).beginBackupOrRestore();
    verify(backupProcedure).performBackup();
    verify(plugin).setLastBackupTime(eq(backupTime));
    verify(plugin).setLastFullBackupTime(eq(backupTime));
    verify(plugin).setLastBackupFailed(eq(false));
    verify(plugin).endBackupOrRestore();
    verify(plugin).setManualBackupRequested(false);
  }

  @Test
  public void testNotLoaded_shouldNotCreateBackup() throws Exception {
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(false);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastFullBackupTime()).thenReturn(null);
    when(plugin.beginBackupOrRestore()).thenReturn(true);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerTrue);
    when(backupProcedure.performBackup()).thenReturn(backupTime);

    periodicWork.execute(taskListener);

    verify(plugin, never()).beginBackupOrRestore();
    verify(backupProcedure, never()).performBackup();
    verify(plugin, never()).setLastBackupTime(eq(backupTime));
    verify(plugin, never()).setLastBackupFailed(eq(false));
    verify(plugin, never()).endBackupOrRestore();
    verify(plugin, never()).setManualBackupRequested(false);
  }

  @Test
  public void testDisableBackup_shouldNotCreateBackup() throws Exception {
    final DateTime backupTime = now;
    when(plugin.getEnableBackup()).thenReturn(false);
    when(plugin.getLastFullBackupTime()).thenReturn(null);
    when(plugin.beginBackupOrRestore()).thenReturn(true);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerTrue);
    when(backupProcedure.performBackup()).thenReturn(backupTime);

    periodicWork.execute(taskListener);

    verify(plugin, never()).beginBackupOrRestore();
    verify(backupProcedure, never()).performBackup();
    verify(plugin, never()).setLastBackupTime(eq(backupTime));
    verify(plugin, never()).setLastBackupFailed(eq(false));
    verify(plugin, never()).endBackupOrRestore();
  }

  @Test
  public void testIncrementalBackupConditionsMet_shouldCreateIncrementalBackup()
      throws Exception {
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(true);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastFullBackupTime()).thenReturn(now);
    when(plugin.getLastBackupTime()).thenReturn(now);
    when(plugin.beginBackupOrRestore()).thenReturn(true);
    when(plugin.getIncrementalBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getIncrementalBackupTrigger()).thenReturn(backupTriggerTrue);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerFalse);
    when(backupProcedure.performBackup()).thenReturn(backupTime);

    periodicWork.execute(taskListener);

    verify(plugin, atLeastOnce()).getLastFullBackupTime();
    verify(plugin).getLastBackupTime();
    verify(plugin).beginBackupOrRestore();
    verify(backupProcedure).performBackup();
    verify(plugin).setLastBackupTime(eq(backupTime));
    verify(plugin, never()).setLastFullBackupTime(any(DateTime.class));
    verify(plugin).setLastBackupFailed(eq(false));
    verify(plugin).endBackupOrRestore();
  }

  @Test
  public void testFullBackupManuallyRequested_shouldCreateFullBackup()
      throws Exception {
    // Set the plugin in such a state that an incremental backup would normally
    // be performed - but also set the 'full backup' flag.
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(true);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastFullBackupTime()).thenReturn(now);
    when(plugin.getLastBackupTime()).thenReturn(now);
    when(plugin.beginBackupOrRestore()).thenReturn(true);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getIncrementalBackupTrigger()).thenReturn(backupTriggerTrue);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerFalse);
    when(plugin.isManualBackupRequested()).thenReturn(true);
    when(backupProcedure.performBackup()).thenReturn(backupTime);

    periodicWork.execute(taskListener);

    verify(plugin).getLastFullBackupTime();
    verify(plugin).beginBackupOrRestore();
    verify(backupProcedure).performBackup();
    verify(plugin).setLastBackupTime(eq(backupTime));
    verify(plugin).setLastFullBackupTime(eq(backupTime));
    verify(plugin).setLastBackupFailed(eq(false));
    verify(plugin).endBackupOrRestore();
    verify(plugin).setManualBackupRequested(false);
  }

  @Test
  public void testBeginBackupOrRestoreFails_shouldNotCreateBackup()
      throws Exception {
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(true);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastBackupTime()).thenReturn(null);
    when(plugin.beginBackupOrRestore()).thenReturn(false);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerTrue);
    when(backupProcedure.performBackup()).thenReturn(backupTime);
    when(backupTriggerTrue.shouldCreateBackup((DateTime) isNull()))
        .thenReturn(true);

    periodicWork.execute(taskListener);

    verify(plugin).beginBackupOrRestore();
    verify(backupProcedure, never()).performBackup();
    verify(plugin, never()).setLastBackupTime(eq(backupTime));
    verify(plugin, never()).setLastBackupFailed(eq(false));
    verify(plugin, never()).endBackupOrRestore();
    verify(plugin, never()).setManualBackupRequested(false);
  }

  @Test
  public void testBackupOrRestoreInProgress_shouldNotCreateAnotherBackup()
      throws Exception {
    final DateTime backupTime = now;
    when(plugin.isLoaded()).thenReturn(true);
    when(plugin.getEnableBackup()).thenReturn(true);
    when(plugin.getLastBackupTime()).thenReturn(null);
    when(plugin.beginBackupOrRestore()).thenReturn(false);
    when(plugin.getFullBackupProcedure()).thenReturn(backupProcedure);
    when(plugin.getFullBackupTrigger()).thenReturn(backupTriggerTrue);
    when(backupProcedure.performBackup()).thenReturn(backupTime);
    when(backupTriggerTrue.shouldCreateBackup((DateTime) isNull()))
        .thenReturn(true);

    periodicWork.execute(taskListener);

    verify(plugin).beginBackupOrRestore();
    verify(backupProcedure, never()).performBackup();
    verify(plugin, never()).setLastBackupTime(eq(backupTime));
    verify(plugin, never()).setLastBackupFailed(eq(false));
    verify(plugin, never()).endBackupOrRestore();
    verify(plugin, never()).setManualBackupRequested(false);
  }

}
