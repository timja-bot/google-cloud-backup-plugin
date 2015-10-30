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
package com.google.jenkins.plugins.persistentmaster.backup;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.jenkins.plugins.persistentmaster.history.BackupHistory;
import com.google.jenkins.plugins.persistentmaster.scope.Scope;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link BackupProcedure}.
 */
public class BackupProcedureTest {
  @Mock
  private Volume volume;

  @Mock
  private Volume.Creator volumeCreator;

  @Mock
  private Scope scope;

  @Mock
  private Storage storage;

  @Mock
  private BackupHistory backupHistory;

  @Mock
  private Path jenkinsHome;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testPerformBackup_correctInteraction() throws Exception {
    when(volume.getFileExtension()).thenReturn("test");
    when(volume.createNew(any(Path.class))).thenReturn(volumeCreator);
    when(volumeCreator.getFileCount()).thenReturn(1); // must be > 0

    BackupProcedure backupProcedure =
        new BackupProcedure(volume, scope, storage, backupHistory, jenkinsHome, null, null);
    DateTime backupTime = backupProcedure.performBackup();

    verify(volume).getFileExtension();
    ArgumentCaptor<Path> backupVolumePathCaptor = ArgumentCaptor.forClass(Path.class);
    verify(volume).createNew(backupVolumePathCaptor.capture());
    verify(scope).addFiles(same(jenkinsHome), same(volumeCreator), any(List.class));
    ArgumentCaptor<String> backupVolumeNameCapture = ArgumentCaptor.forClass(String.class);
    verify(storage).storeFile(
        same(backupVolumePathCaptor.getValue()), backupVolumeNameCapture.capture());
    verify(storage).updateLastBackup(eq(Arrays.asList(backupVolumeNameCapture.getValue())));
    verify(backupHistory)
        .processHistoricBackups(same(storage), eq(backupVolumeNameCapture.getValue()));
    verify(storage).updateExistingFilesMetaData(any(List.class));
    verifyNoMoreInteractions(volume, scope, storage, backupHistory);
    assertTrue(backupVolumeNameCapture.getValue().endsWith(".test"));
    assertTrue(backupTime.isBeforeNow());
  }

  @Test
  public void testPerformBackup_nameSuffix_shouldAddSuffix() throws Exception {
    when(volume.getFileExtension()).thenReturn("test");
    when(volume.createNew(any(Path.class))).thenReturn(volumeCreator);
    when(volumeCreator.getFileCount()).thenReturn(1); // must be > 0

    BackupProcedure backupProcedure =
        new BackupProcedure(volume, scope, storage, backupHistory, jenkinsHome, null, "-suffix");
    backupProcedure.performBackup();

    ArgumentCaptor<String> backupVolumeNameCapture = ArgumentCaptor.forClass(String.class);
    verify(storage).storeFile(any(Path.class), backupVolumeNameCapture.capture());
    assertTrue(backupVolumeNameCapture.getValue().endsWith("-suffix.test"));
  }
}
