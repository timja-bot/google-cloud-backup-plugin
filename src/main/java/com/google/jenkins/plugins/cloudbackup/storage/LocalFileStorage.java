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

import com.google.api.client.util.Lists;
import com.google.common.base.Objects;
import com.google.jenkins.plugins.cloudbackup.VersionUtility;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Simple storage implementation using a directory in the local filesystem as
 * storage provider.
 */
public class LocalFileStorage implements Storage {

  private static final String LAST_BACKUP_FILE = "last-backup";
  private static final String EXISTING_FILE_METADATA = "existing-files-metadata";
  private static final String COMMENT_PREFIX = "#";
  private static final String COMMENT_LINE =
      COMMENT_PREFIX + " This file contains the filename of the last backup.";
  private static final String EXISTING_FILES_COMMENT_LINE =
      COMMENT_PREFIX + " This file contains the existing files meta data.";

  private final Path storageDir;

  public LocalFileStorage(Path storageDir) {
    this.storageDir = storageDir;
  }

  @Override
  public void storeFile(Path localFile, String filename) throws IOException {
    Files.copy(localFile, storageDir.resolve(filename));
  }

  @Override
  public void loadFile(String filename, Path target) throws IOException {
    Files.copy(storageDir.resolve(filename), target);
  }

  @Override
  public void deleteFile(String filename) throws IOException {
    Files.delete(storageDir.resolve(filename));
  }

  @Override
  public List<String> listFiles() throws IOException {
    List<String> files = new LinkedList<>();
    try (
        DirectoryStream<Path> directoryStream
            = Files.newDirectoryStream(storageDir)) {
      for (Path next : directoryStream) {
        // exclude internal file
        Path fileName = next.getFileName();
        if (fileName != null && !fileName.toString().equals(LAST_BACKUP_FILE)) {
          files.add(fileName.toString());
        }
      }
    } // auto-close directory stream
    return files;
  }

  @Override
  public List<String> listMetadataForExistingFiles() throws IOException {
    List<String> fileMetadata = listDataFromStorage(EXISTING_FILE_METADATA);
    return (fileMetadata == null) ? new LinkedList<String>():fileMetadata;
  }

  @Override
  public List<String> findLatestBackup() throws IOException {
    return listDataFromStorage(LAST_BACKUP_FILE);
  }

  @Override
  public String getVersionInfo() {
    return VersionUtility.getFileSystemVersion(storageDir);
  }

  private List<String> listDataFromStorage(String name) throws IOException {
    Path path = storageDir.resolve(name);
    if (Files.exists(path)) {
      List<String> lines =
          Files.readAllLines(path, StandardCharsets.UTF_8);
      List<String> filenames = new LinkedList<>();
      for (String line : lines) {
        if (line != null && !line.trim().isEmpty()
            && !line.startsWith(COMMENT_PREFIX)) {
          filenames.add(line.trim());
        }
      }
      return filenames;
    }
    return null;
  }

  @Override
  public void updateLastBackup(List<String> filenames) throws IOException {
    updateObject(filenames, LAST_BACKUP_FILE, COMMENT_LINE);
  }

  @Override
  public void updateExistingFilesMetaData(Set<String> filenames) throws IOException {
    updateObject(Lists.newArrayList(filenames), EXISTING_FILE_METADATA , EXISTING_FILES_COMMENT_LINE);

  }

  @Override
  public void updateVersionInfo(String version) throws IOException {
    VersionUtility.updateFileSystemVersion(storageDir, version);
  }

  public void updateObject(List<String> filenames, String name, String comment) throws IOException {
    Path path = storageDir.resolve(name);
    Deque<String> content = new LinkedList<>(filenames);
    content.addFirst(comment);
    // write to file, overwriting any existing content
    Files.write(path, content, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LocalFileStorage that = (LocalFileStorage) o;

    if (storageDir != null ?
        !storageDir.equals(that.storageDir) : that.storageDir != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(storageDir);
  }

  @Override
  public String toString() {
    return "LocalFileStorage{" +
        "storageDir=" + storageDir +
        '}';
  }
}
