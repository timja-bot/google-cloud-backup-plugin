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

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.util.FormValidation;

/**
 * Tests for {@link LocalFileStorageProvider}.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LocalFileStorageProvider.class)
public class LocalFileStorageProviderTest {

  private static final String PATH = "/tmp/bdir";

  @Mock private File mockFile;

  @Test
  public void testDefaultConstructor() {
    LocalFileStorageProvider provider = new LocalFileStorageProvider();
    LocalFileStorage expectedStorage =
        new LocalFileStorage(
            new File(LocalFileStorageProvider.DEFAULT_DIRECTORY).toPath());

    assertEquals(LocalFileStorageProvider.DEFAULT_DIRECTORY,
        provider.getDirectory());
    assertEquals(expectedStorage, provider.getStorage());
  }

  @Test
  public void testConstructorWithDirectory() {
    LocalFileStorageProvider provider = new LocalFileStorageProvider(PATH);
    LocalFileStorage expectedStorage =
        new LocalFileStorage(new File(PATH).toPath());

    assertEquals(PATH, provider.getDirectory());
    assertEquals(expectedStorage, provider.getStorage());
  }

  @Test
  public void testConstructorWithNullDirectory() {
    LocalFileStorageProvider provider = new LocalFileStorageProvider(null);
    LocalFileStorage expectedStorage =
        new LocalFileStorage(
            new File(LocalFileStorageProvider.DEFAULT_DIRECTORY).toPath());

    assertEquals(LocalFileStorageProvider.DEFAULT_DIRECTORY,
        provider.getDirectory());
    assertEquals(expectedStorage, provider.getStorage());
  }

  @Test
  public void testValidateDirectoryEmptyInputs() {
    assertNotNull(LocalFileStorageProvider.validateDirectory(null));
    assertNotNull(LocalFileStorageProvider.validateDirectory(""));
    assertNotNull(LocalFileStorageProvider.validateDirectory("    "));
  }

  @Test
  public void testValidateDirectoryDoesntExist() throws Exception {
    whenNew(File.class).withArguments(PATH).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(false);

    assertNotNull(LocalFileStorageProvider.validateDirectory(PATH));

    verify(mockFile).exists();
  }

  @Test
  public void testValidateDirectoryNotDirectory() throws Exception {
    whenNew(File.class).withArguments(PATH).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.isDirectory()).thenReturn(false);

    assertNotNull(LocalFileStorageProvider.validateDirectory(PATH));

    verify(mockFile).exists();
    verify(mockFile).isDirectory();
  }

  @Test
  public void testValidateDirectoryCannotWrite() throws Exception {
    whenNew(File.class).withArguments(PATH).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.isDirectory()).thenReturn(true);
    when(mockFile.canWrite()).thenReturn(false);

    assertNotNull(LocalFileStorageProvider.validateDirectory(PATH));

    verify(mockFile).exists();
    verify(mockFile).isDirectory();
    verify(mockFile).canWrite();
  }

  @Test
  public void testValidateDirectorySuccess() throws Exception {
    whenNew(File.class).withArguments(PATH).thenReturn(mockFile);
    when(mockFile.exists()).thenReturn(true);
    when(mockFile.isDirectory()).thenReturn(true);
    when(mockFile.canWrite()).thenReturn(true);

    assertNull(LocalFileStorageProvider.validateDirectory(PATH));

    verify(mockFile).exists();
    verify(mockFile).isDirectory();
    verify(mockFile).canWrite();
  }

  @Test
  public void testIsValidSuccess() {
    LocalFileStorageProvider provider = new LocalFileStorageProvider(PATH);
    mockStatic(LocalFileStorageProvider.class);
    when(LocalFileStorageProvider.validateDirectory(PATH)).thenReturn(null);

    assertTrue(provider.isValid());
  }

  @Test
  public void testIsValidFailure() {
    LocalFileStorageProvider provider = new LocalFileStorageProvider(PATH);
    mockStatic(LocalFileStorageProvider.class);
    when(LocalFileStorageProvider.validateDirectory(PATH)).thenReturn("error");

    assertFalse(provider.isValid());
  }

  @Test
  public void testDoCheckDirectorySuccess() {
    LocalFileStorageProvider.LocalFileStorageProviderDescriptor descriptor =
        new LocalFileStorageProvider.LocalFileStorageProviderDescriptor();
    mockStatic(LocalFileStorageProvider.class);
    when(LocalFileStorageProvider.validateDirectory(PATH)).thenReturn(null);

    assertEquals(FormValidation.Kind.OK,
        descriptor.doCheckDirectory(PATH).kind);
  }

  @Test
  public void testDoCheckDirectoryFailure() {
    LocalFileStorageProvider.LocalFileStorageProviderDescriptor descriptor =
        new LocalFileStorageProvider.LocalFileStorageProviderDescriptor();
    mockStatic(LocalFileStorageProvider.class);
    when(LocalFileStorageProvider.validateDirectory(PATH)).thenReturn("error");

    assertEquals(FormValidation.Kind.ERROR,
        descriptor.doCheckDirectory(PATH).kind);
  }
}
