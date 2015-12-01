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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.jenkins.plugins.persistentmaster.volume.zip.ZipVolumeTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;

/**
 * Tests for {@link VersionUtility}.
 */
public class VersionUtilityTest {
  
  @Mock private Path jenkinsHome;
  Path file;
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    Path tempDirectory = Files.createTempDirectory(ZipVolumeTest.class.getSimpleName());
    file = tempDirectory.resolve("fileInRoot");
    when(jenkinsHome.resolve(any(String.class))).thenReturn(file);
  }
  
  @Test
  public void testContent() throws IOException {
    Files.write(file, Collections.singleton("3"), StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW);
   String a =  VersionUtility.getFileSystemVersion(jenkinsHome);
   assertEquals("3", a);
  }
  
  @Test
  public void testNoFile()  {
    when(jenkinsHome.resolve(any(String.class))).thenReturn(null);
   String a =  VersionUtility.getFileSystemVersion(jenkinsHome);
   assertNull(a);
  }
  
}

