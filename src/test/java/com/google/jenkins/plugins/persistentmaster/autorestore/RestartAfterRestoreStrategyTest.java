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
package com.google.jenkins.plugins.persistentmaster.autorestore;

import java.nio.file.Path;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import jenkins.model.Jenkins;

/**
 * Tests for {@link RestartAfterRestoreStrategy}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Jenkins.class})
public class RestartAfterRestoreStrategyTest {
  private static final String BACKUP_ID_1 = "backup-id-1";
  private static final String BACKUP_ID_2 = "backup-id-2";

  @Mock private Jenkins jenkins;
  @Mock private Path homePath;
  @Mock private RestoreLog restoreLog;
  private RestartAfterRestoreStrategy strategy;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockStatic(Jenkins.class);
    when(Jenkins.getActiveInstance()).thenReturn(jenkins);
    strategy = new RestartAfterRestoreStrategy(restoreLog);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testInitNewEnvironmentNoInteraction() throws Exception {
    strategy.initializeNewEnvironment(homePath);
    verifyNoMoreInteractions(restoreLog);
    verifyNoMoreInteractions(jenkins);
  }

  @Test
  public void testInitRestoredEnvironmentAlreadyRestored() throws Exception {
    when(restoreLog.getLastBackupId()).thenReturn(BACKUP_ID_1);
    strategy.initializeRestoredEnvironment(homePath, BACKUP_ID_1);
    verify(restoreLog).getLastBackupId();
    verifyNoMoreInteractions(restoreLog);
    verifyNoMoreInteractions(jenkins);
  }

  @Test
  public void testInitRestoredEnvironmentNoLoggedBackupId() throws Exception {
    when(restoreLog.getLastBackupId()).thenReturn(null);
    strategy.initializeRestoredEnvironment(homePath, BACKUP_ID_1);
    verify(restoreLog).getLastBackupId();
    verify(restoreLog).writeLastBackupId(BACKUP_ID_1);
    verify(jenkins).restart();
  }

  @Test
  public void testInitRestoredEnvironmentDifferentBackupId() throws Exception {
    when(restoreLog.getLastBackupId()).thenReturn(BACKUP_ID_2);
    strategy.initializeRestoredEnvironment(homePath, BACKUP_ID_1);
    verify(restoreLog).getLastBackupId();
    verify(restoreLog).writeLastBackupId(BACKUP_ID_1);
    verify(jenkins).restart();
  }
}
