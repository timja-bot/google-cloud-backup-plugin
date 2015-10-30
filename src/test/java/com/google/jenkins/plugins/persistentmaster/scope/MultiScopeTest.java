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
package com.google.jenkins.plugins.persistentmaster.scope;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import com.google.jenkins.plugins.persistentmaster.volume.Volume;
import com.google.jenkins.plugins.persistentmaster.volume.Volume.Entry;

import org.junit.*;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Tests for {@link MultiScope}.
 */
public class MultiScopeTest {
  @Mock
  private Volume.Creator creator;

  @Mock
  private Volume.Extractor extractor;

  @Mock
  private Volume.Entry entry1;

  @Mock
  private Volume.Entry entry2;

  @Mock
  private Scope scope1;

  @Mock
  private Scope scope2;

  @Mock
  private Path jenkinsHome;

  private MultiScope multiScope;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    multiScope = new MultiScope();
    multiScope.addSubScope(scope1, "scope1/");
    multiScope.addSubScope(scope2, "scope2/");
  }

  @After
  public void tearDown() throws Exception {
    multiScope = null;
  }

  @Test
  public void testAddFiles_prefixAdded() throws Exception {
    multiScope.addFiles(jenkinsHome, creator, new ArrayList<String>());

    // scope1
    ArgumentCaptor<Volume.Creator> volumeCreatorCaptor =
        ArgumentCaptor.forClass(Volume.Creator.class);
    verify(scope1).addFiles(same(jenkinsHome), volumeCreatorCaptor.capture(), any(List.class));
    assertNotSame(creator, volumeCreatorCaptor.getValue());
    volumeCreatorCaptor.getValue().addFile(jenkinsHome, "fileOfScope1", null);
    verify(creator).addFile(
        same(jenkinsHome), eq("scope1/fileOfScope1"), any(BasicFileAttributes.class));

    // scope2
    volumeCreatorCaptor = ArgumentCaptor.forClass(Volume.Creator.class);
    verify(scope2).addFiles(same(jenkinsHome), volumeCreatorCaptor.capture(), any(List.class));
    assertNotSame(creator, volumeCreatorCaptor.getValue());
    volumeCreatorCaptor.getValue().addFile(jenkinsHome, "fileOfScope2", null);
    verify(creator).addFile(
        same(jenkinsHome), eq("scope2/fileOfScope2"), any(BasicFileAttributes.class));
  }

  @Test
  public void testExtractFiles_prefixRemovedAndSeparateIteratorsForEachScope() throws Exception {
    // use answer, b/c a new instance needs to be returned for each invocation
    when(extractor.iterator())
        .thenAnswer(new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return Arrays.asList(entry1, entry2).iterator();
          }
        });
    when(entry1.getName()).thenReturn("scope1/fileOfScope1");
    when(entry2.getName()).thenReturn("scope2/fileOfScope2");

    multiScope.extractFiles(jenkinsHome, extractor, false, null);

    // scope1
    ArgumentCaptor<Volume.Extractor> volumeExtractorCaptor =
        ArgumentCaptor.forClass(Volume.Extractor.class);
    verify(scope1).extractFiles(
        same(jenkinsHome), volumeExtractorCaptor.capture(), eq(false), any(List.class));
    assertNotSame(extractor, volumeExtractorCaptor.getValue());
    Iterator<Entry> entries = volumeExtractorCaptor.getValue().iterator();
    assertTrue(entries.hasNext());
    assertEquals("fileOfScope1", entries.next().getName());
    assertFalse(entries.hasNext());

    // scope2
    volumeExtractorCaptor = ArgumentCaptor.forClass(Volume.Extractor.class);
    verify(scope2).extractFiles(
        same(jenkinsHome), volumeExtractorCaptor.capture(), eq(false), any(List.class));
    assertNotSame(extractor, volumeExtractorCaptor.getValue());
    entries = volumeExtractorCaptor.getValue().iterator();
    assertTrue(entries.hasNext());
    assertEquals("fileOfScope2", entries.next().getName());
    assertFalse(entries.hasNext());
  }
}
