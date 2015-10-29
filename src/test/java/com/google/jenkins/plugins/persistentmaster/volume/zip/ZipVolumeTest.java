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
package com.google.jenkins.plugins.persistentmaster.volume.zip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.jenkins.plugins.persistentmaster.scope.Scopes;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * Tests for {@link ZipVolume}.
 */
public class ZipVolumeTest {

  private static final Logger logger
      = Logger.getLogger(ZipVolumeTest.class.getName());

  private Path tempDirectory;
  private Path nonEmptyDir;
  private Path emptyDir;
  private Path fileInRoot;
  private Path fileInDir;
  private Path validSymlink;
  private Path invalidSymlink;
  private Path existingFile;
  private Path newFile;
  private ZipVolume zipVolume;

  @Before
  public void setUp() throws Exception {
    tempDirectory = Files
        .createTempDirectory(ZipVolumeTest.class.getSimpleName());
    nonEmptyDir = tempDirectory.resolve("nonEmptyDir");
    Files.createDirectory(nonEmptyDir);
    emptyDir = tempDirectory.resolve("emptyDir");
    Files.createDirectory(emptyDir);
    fileInRoot = tempDirectory.resolve("fileInRoot");
    Files.write(fileInRoot, Collections.singleton("fileInRoot content"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    fileInDir = tempDirectory.resolve("nonEmptyDir/fileInDir");
    Files.write(fileInDir, Collections.singleton("fileInDir content"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    validSymlink = tempDirectory.resolve("validSymlink");
    Files.createSymbolicLink(validSymlink,
        tempDirectory.relativize(nonEmptyDir));
    invalidSymlink = tempDirectory.resolve("invalidSymlink");
    // invalid symlink in the fashion jenkins creates them
    Files.createSymbolicLink(invalidSymlink, Paths.get("-1"));
    existingFile = tempDirectory.resolve("existingFile");
    Files.write(existingFile, Collections.singleton("existingFile new content"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    newFile = tempDirectory.resolve("newFile");
    Files.write(newFile, Collections.singleton("newFile new content"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    zipVolume = new ZipVolume();
  }

  @After
  public void tearDown() throws Exception {
    deleteDirectory(tempDirectory);
    tempDirectory = null;
    zipVolume = null;
  }

  @Test
  public void testCreateAndExtractZipArchive() throws Exception {
    // create
    Path volumePath = tempDirectory.resolve("test.zip");
    List<String> existingFiles = Arrays.asList("nonEmptyDir","emptyDir", "fileInRoot", "nonEmptyDir/fileInDir","validSymlink","invalidSymlink");
    try (Volume.Creator creator = zipVolume.createNew(volumePath)) {
      creator.addFile(nonEmptyDir, "nonEmptyDir", null);
      creator.addFile(emptyDir, "emptyDir", null);
      creator.addFile(fileInRoot, "fileInRoot", null);
      creator.addFile(fileInDir, "nonEmptyDir/fileInDir", null);
      creator.addFile(validSymlink, "validSymlink",
          Files.readAttributes(validSymlink, BasicFileAttributes.class,
              LinkOption.NOFOLLOW_LINKS));
      creator.addFile(invalidSymlink, "invalidSymlink",
          Files.readAttributes(invalidSymlink, BasicFileAttributes.class,
              LinkOption.NOFOLLOW_LINKS));
    }  // auto-close creator
    assertTrue(Files.exists(volumePath));
    

    // extract
    Path extractPath = tempDirectory.resolve("extracted");
    Files.createDirectory(extractPath);
    try (Volume.Extractor extractor = zipVolume.extract(volumePath)) {
      Scopes.extractAllFilesTo(extractPath, extractor, true, existingFiles);
    } // auto-close extractor

    // verify
    assertTrue(Files.exists(extractPath.resolve("nonEmptyDir")));
    assertTrue(Files.exists(extractPath.resolve("emptyDir")));
    assertTrue(Files.exists(extractPath.resolve("fileInRoot")));
    assertTrue(Files.exists(extractPath.resolve("nonEmptyDir/fileInDir")));
    // ensure the link itself exists
    assertTrue(Files.exists(extractPath.resolve("validSymlink"),
        LinkOption.NOFOLLOW_LINKS));
    // ensure the link target exists
    assertTrue(Files.exists(extractPath.resolve("validSymlink")));
    // ensure the link itself exists
    assertTrue(Files.exists(extractPath.resolve("invalidSymlink"),
        LinkOption.NOFOLLOW_LINKS));
    // ensure the link target does not exist
    assertTrue(Files.notExists(extractPath.resolve("invalidSymlink")));
  }

  
  @Test
  public void a() throws Exception {
    String a = "plugins/maven-plugin/WEB-INF/lib/commons-cli-1.2.jar";
    List<String> b =  Arrays.asList("v","plugins/maven-plugin/WEB-INF/lib/commons-cli-1.2.jar");
    
    assertTrue(b.contains(a));
  }
  
  @Test
  public void testCreateAndExtractZipArchiveWithConflict() throws Exception {
    // create
    Path volumePath = tempDirectory.resolve("test.zip");
    List<String> existingFiles = Arrays.asList("existingFile","newFile");
    try (Volume.Creator creator = zipVolume.createNew(volumePath)) {
      creator.addFile(existingFile, "existingFile", null);
      creator.addFile(newFile, "newFile", null);
    }  // auto-close creator
    assertTrue(Files.exists(volumePath));

    // extract
    Path extractPath = tempDirectory.resolve("extracted");
    Files.createDirectory(extractPath);
    // create pre-existing file
    Path extractedExistingFile = extractPath.resolve("existingFile");
    Path extractedNewFile = extractPath.resolve("newFile");
    Files.write(extractedExistingFile,
        Collections.singleton("existingFile old content"),
        StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    try (Volume.Extractor extractor = zipVolume.extract(volumePath)) {
      Scopes.extractAllFilesTo(extractPath, extractor, false, existingFiles);
//      for (Volume.Entry entry : extractor) {
//        entry.extractTo(extractPath.resolve(entry.getName()), false, existingFiles);
//      }
    } // auto-close extractor

    // verify
    assertTrue(Files.exists(extractedExistingFile));
    assertTrue(Files.exists(extractedNewFile));
    List<String> existingFileText = Files.readAllLines(extractedExistingFile,
        StandardCharsets.UTF_8);
    List<String> newFileText = Files.readAllLines(extractedNewFile,
        StandardCharsets.UTF_8);
    assertEquals(1, existingFileText.size());
    assertEquals(1, newFileText.size());
    assertEquals("existingFile old content", existingFileText.get(0));
    assertEquals("newFile new content", newFileText.get(0));
  }

  private static void deleteDirectory(Path dir) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult visitFile(
          Path file, BasicFileAttributes attrs) throws IOException {
        logger.finer("deleting file " + file.toString());
        Files.delete(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(
          Path dir, IOException e) throws IOException {
        if (e == null) {
          logger.finer("deleting dir " + dir.toString());
          Files.delete(dir);
          return FileVisitResult.CONTINUE;
        } else {
          throw e;
        }
      }

    });
  }

}
