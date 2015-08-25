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
package com.google.jenkins.plugins.persistentmaster;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests for {@link PersistentMasterPlugin}.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistentMasterPlugin.class)
public class PersistentMasterPluginTest {

  @Test
  public void testStartBehavior() throws Exception {
    PersistentMasterPlugin plugin = spy(new PersistentMasterPlugin());
    // At first the plugin should not be ready.
    assertFalse(plugin.isLoaded());

    doNothing().when(plugin, "load");
    doNothing().when(plugin, "save");
    plugin.start();

    verifyPrivate(plugin).invoke("load");
    verify(plugin).save();
    assertTrue(plugin.isLoaded());

    // Plugin should start in a restore state.
    assertTrue(plugin.isBackupOrRestoreInProgress());
  }
}
