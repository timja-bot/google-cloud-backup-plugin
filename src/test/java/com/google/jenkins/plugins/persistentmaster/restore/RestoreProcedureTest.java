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
package com.google.jenkins.plugins.persistentmaster.restore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.jenkins.plugins.persistentmaster.initiation.InitiationStrategy;
import com.google.jenkins.plugins.persistentmaster.scope.Scope;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * Tests for {@link RestoreProcedure}.
 */
public class RestoreProcedureTest {

  private static final int MAX_SLEEP_TIME_MS = 50;

  @Mock private Volume volume;
  @Mock private Volume.Extractor volumeExtractor;
  @Mock private Scope scope;
  @Mock private Storage storage;
  @Mock private InitiationStrategy initiationStrategy;
  @Mock private Path jenkinsHome;

  private RestoreProcedure restoreProcedure;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    restoreProcedure = new RestoreProcedure(
        volume, scope, storage, initiationStrategy, jenkinsHome, null, false);
  }

  @After
  public void tearDown() throws Exception {
    restoreProcedure = null;
  }

  @Test
  public void testNoExistingBackup_shouldInitiateNewEnvironment()
      throws Exception {
    when(storage.findLatestBackup()).thenReturn(null);  // no existing backup

    restoreProcedure.performRestore();

    verify(storage).findLatestBackup();
    verify(initiationStrategy).initializeNewEnvironment(eq(jenkinsHome));
    verifyNoMoreInteractions(initiationStrategy, volume, scope, storage);
  }

  @Test
  public void testExistingBackup_shouldRestoreAndInitiateRestoredEnvironment()
      throws Exception {
    final String latestBackup = "latestBackup";
    when(storage.findLatestBackup()).thenReturn(Arrays.asList(latestBackup));
    when(volume.extract(any(Path.class))).thenReturn(volumeExtractor);

    restoreProcedure.performRestore();

    verify(storage).findLatestBackup();
    ArgumentCaptor<Path> volumePathCaptor = ArgumentCaptor.forClass(Path.class);
    verify(storage).loadFile(eq(latestBackup), volumePathCaptor.capture());
    verify(volume).extract(eq(volumePathCaptor.getValue()));
    verify(scope).extractFiles(eq(jenkinsHome), same(volumeExtractor),
        eq(false));
    verify(initiationStrategy).initializeRestoredEnvironment(eq(jenkinsHome),
        eq(latestBackup));
    verifyNoMoreInteractions(initiationStrategy, volume, scope, storage);
  }

  @Test
  public void testMultipleBackups_shouldRestoreInOrder() throws Exception {
    final int backupCnt = 100;
    final List<String> backups = new ArrayList<>(backupCnt);
    for (int i = 0; i < backupCnt; i++) {
      backups.add("backup" + i);
    }

    when(storage.findLatestBackup()).thenReturn(backups);
    when(volume.extract(any(Path.class))).thenReturn(volumeExtractor);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        // sleep some time to simulate fetching of a file from storage
        Thread.sleep(new Random().nextInt(MAX_SLEEP_TIME_MS));
        return null;
      }
    }).when(storage).loadFile(any(String.class), any(Path.class));

    restoreProcedure.performRestore();

    verify(storage).findLatestBackup();

    // every backup must be restored in order
    List<ArgumentCaptor<Path>> pathCaptorList = new ArrayList<>(backupCnt);
    for (String backup : backups) {
      InOrder backupOrder = inOrder(storage, volume, scope);
      ArgumentCaptor<Path> volumePath = ArgumentCaptor.forClass(Path.class);
      backupOrder.verify(storage).loadFile(eq(backup), volumePath.capture());
      backupOrder.verify(volume).extract(eq(volumePath.getValue()));
      backupOrder.verify(scope).extractFiles(
          eq(jenkinsHome), same(volumeExtractor), eq(false));
      pathCaptorList.add(volumePath);
    }

    // extraction of files must happen in order
    InOrder extractOrder = inOrder(volume);
    for (ArgumentCaptor<Path> volumePath : pathCaptorList) {
      extractOrder.verify(volume).extract(eq(volumePath.getValue()));
    }

    verify(initiationStrategy).initializeRestoredEnvironment(eq(jenkinsHome),
        eq(backups.get(backupCnt - 1)));
    verifyNoMoreInteractions(initiationStrategy, volume, scope, storage);
  }

  @Test(expected = IOException.class)
  public void testMultipleBackups_shouldTerminateOnFail() throws Exception {
    final int backupCnt = 100;
    final List<String> backups = new ArrayList<>(backupCnt);
    for (int i = 0; i < backupCnt; i++) {
      backups.add("backup" + i);
    }
    when(storage.findLatestBackup()).thenReturn(backups);
    when(volume.extract(any(Path.class))).thenReturn(volumeExtractor);
    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        // sleep some time to simulate fetching of a file from storage
        Thread.sleep(new Random().nextInt(MAX_SLEEP_TIME_MS));
        if ("backup4".equals(invocation.getArguments()[0])) {
          throw new IOException();  // fail fetching backup4
        }
        return null;
      }
    }).when(storage).loadFile(any(String.class), any(Path.class));

    restoreProcedure.performRestore();  // must throw IOException
  }

}
