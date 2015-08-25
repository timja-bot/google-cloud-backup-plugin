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

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.jenkins.plugins.persistentmaster.PersistentMasterMainModule;
import com.google.jenkins.plugins.persistentmaster.PersistentMasterPlugin;
import com.google.jenkins.plugins.persistentmaster.restore.RestoreProcedure;
import com.google.jenkins.plugins.persistentmaster.scope.Scope;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * Tests for {@link AutoRestoreInitializer}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({AutoRestoreInitializer.class,
    PersistentMasterPlugin.class,
    RestartAfterRestoreStrategy.class, RestoreLog.class,
    RestoreProcedure.class})
public class AutoRestoreInitializerTest {

  @Mock private Path homePath;
  @Mock private Path scratchPath;
  @Mock private PersistentMasterMainModule module;
  @Mock private PersistentMasterPlugin plugin;
  @Mock private RestartAfterRestoreStrategy strategy;
  @Mock private RestoreLog restoreLog;
  @Mock private RestoreProcedure procedure;
  @Mock private Scope scope;
  @Mock private Storage storage;
  @Mock private Volume volume;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockStatic(PersistentMasterPlugin.class);
    mockStatic(RestartAfterRestoreStrategy.class);
    mockStatic(RestoreLog.class);
    mockStatic(RestoreProcedure.class);
    // Configure default mock for plugin
    when(PersistentMasterPlugin.getInstance()).thenReturn(plugin);
    when(plugin.getPersistentMasterMainModule()).thenReturn(module);
    when(plugin.isBackupOrRestoreInProgress()).thenReturn(true);
    when(plugin.getEnableAutoRestore()).thenReturn(true);
    when(plugin.calculateJenkinsHome()).thenReturn(homePath);
    when(plugin.getScratchDirectory()).thenReturn(scratchPath);
    when(plugin.getRestoreOverwritesData()).thenReturn(false);
    // Configure default mock for module
    when(module.getVolume()).thenReturn(volume);
    when(module.getScope()).thenReturn(scope);
    when(module.getStorage()).thenReturn(storage);
    // Configure other default mocks
    when(RestoreLog.getLock()).thenReturn(new Object());
    whenNew(RestoreLog.class).withAnyArguments().thenReturn(restoreLog);
    whenNew(RestartAfterRestoreStrategy.class).withAnyArguments()
        .thenReturn(strategy);
    whenNew(RestoreProcedure.class).withAnyArguments().thenReturn(procedure);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testInit_exitWithNoPlugin() throws Exception {
    when(plugin.getInstance()).thenReturn(null);
    AutoRestoreInitializer.init();
    // We can't use checkCleanExitWithoutRestore because this is slightly
    // different exit behavior - plugin.endBackupOrRestore should not be called.
    verifyNew(RestoreLog.class, never()).withArguments(homePath);
    verifyNew(RestartAfterRestoreStrategy.class, never()).withArguments(
        restoreLog);
    verifyNew(RestoreProcedure.class, never()).withArguments(volume, scope,
        storage, strategy, homePath, scratchPath, true);
    verifyNew(RestoreProcedure.class, never()).withArguments(volume, scope,
        storage, strategy, homePath, scratchPath, false);
    verify(procedure, never()).performRestore();
  }

  @Test
  public void testInit_exitWithNullModule() throws Exception {
    when(plugin.getPersistentMasterMainModule()).thenReturn(null);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_exitWithAutoRestoreDisabled() throws Exception {
    when(plugin.getEnableAutoRestore()).thenReturn(false);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_exitWithNullVolume() throws Exception {
    when(module.getVolume()).thenReturn(null);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_exitWithNullScope() throws Exception {
    when(module.getScope()).thenReturn(null);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_exitWithNullStorage() throws Exception {
    when(module.getScope()).thenReturn(null);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_exitWithNullHomePath() throws Exception {
    when(plugin.calculateJenkinsHome()).thenReturn(null);
    AutoRestoreInitializer.init();
    checkCleanExitWithoutRestore();
  }

  @Test
  public void testInit_success() throws Exception {
    AutoRestoreInitializer.init();
    verify(plugin).endBackupOrRestore();
    verifyNew(RestoreLog.class).withArguments(homePath);
    verifyNew(RestartAfterRestoreStrategy.class).withArguments(restoreLog);
    verifyNew(RestoreProcedure.class).withArguments(volume, scope, storage,
        strategy, homePath, scratchPath, false);
    verify(procedure).performRestore();
  }

  @Test
  public void testInit_successWithOverwriteData() throws Exception {
    when(plugin.getRestoreOverwritesData()).thenReturn(true);
    AutoRestoreInitializer.init();
    verify(plugin).endBackupOrRestore();
    verifyNew(RestoreLog.class).withArguments(homePath);
    verifyNew(RestartAfterRestoreStrategy.class).withArguments(restoreLog);
    verifyNew(RestoreProcedure.class).withArguments(volume, scope, storage,
        strategy, homePath, scratchPath, true);
    verify(procedure).performRestore();
  }

  /**
   * Helper method that verifies a clean exit without an invocation of the
   * restore procedure.
   */
  private void checkCleanExitWithoutRestore() throws Exception {
    verify(plugin).endBackupOrRestore();
    verifyNew(RestoreLog.class, never()).withArguments(homePath);
    verifyNew(RestartAfterRestoreStrategy.class, never()).withArguments(
        restoreLog);
    verifyNew(RestoreProcedure.class, never()).withArguments(volume, scope,
        storage, strategy, homePath, scratchPath);
    verify(procedure, never()).performRestore();
  }
}
