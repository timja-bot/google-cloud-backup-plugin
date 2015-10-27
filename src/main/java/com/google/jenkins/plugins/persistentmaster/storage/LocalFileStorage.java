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
package com.google.jenkins.plugins.persistentmaster.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Objects;

/**
 * Simple storage implementation using a directory in the local filesystem as
 * storage provider.
 */
public class LocalFileStorage implements Storage {

  private static final String LAST_BACKUP_FILE = "last-backup";
  private static final String COMMENT_PREFIX = "#";
  private static final String COMMENT_LINE =
      COMMENT_PREFIX + " This file contains the filename of the last backup.";

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
  public List<String> findLatestBackup() throws IOException {
    Path lastBackupPath = storageDir.resolve(LAST_BACKUP_FILE);
    if (Files.exists(lastBackupPath)) {
      List<String> lines =
          Files.readAllLines(lastBackupPath, StandardCharsets.UTF_8);
      List<String> filenames = new LinkedList<>();
      for (String line : lines) {
        if (line != null && !line.trim().isEmpty()
            && !line.startsWith(COMMENT_PREFIX)) {
          filenames.add(line.trim());
        }
      }
      return filenames;
    }
    return null;  // last backup reference not found
  }
  
 
  @Override
  public List<String> listMetadataForExistingFiles() throws IOException {
    // TODO(ckerur): Auto-generated method stub
    return null;
  }

  @Override
  public void updateLastBackup(List<String> filenames) throws IOException {
    Path lastBackupPath = storageDir.resolve(LAST_BACKUP_FILE);
    Deque<String> content = new LinkedList<>(filenames);
    content.addFirst(COMMENT_LINE);
    // write filename of last backup to file, overwriting any existing content
    Files.write(lastBackupPath, content, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }

  
  @Override
  public void updateExistingFilesMetaData(List<String> filenames) throws IOException {
    // TODO(ckerur): Auto-generated method stub
   
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
