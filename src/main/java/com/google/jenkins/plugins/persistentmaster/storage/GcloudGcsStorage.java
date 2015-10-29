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

import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Storage implementation using a Google Cloud Storage (GCS) bucket as storage
 * provider.
 *
 * This implementation employs the command line tool gsutil (which is included
 * in gcloud) in order to perform the actual GCS access.
 */
public class GcloudGcsStorage implements Storage {
  private static final Logger logger = Logger.getLogger(GcloudGcsStorage.class.getName());

  private static final String LAST_BACKUP_FILE = "last-backup";
  private static final String ALL_EXISTING_FILES = "existing-files";
  private static final String COMMENT_PREFIX = "#";
  private static final String COMMENT_LINE =
      COMMENT_PREFIX + " This file contains the filename of the last backup.";
//  private static final String EXISTING_FILES_COMMENT_LINE =
//      COMMENT_PREFIX + " This file contains the existing files meta data.";
  private static final String GSUTIL_CMD = "gsutil";
  private static final String TMP_DIR_PREFIX = "persistent-master-backup-plugin";

  private final String gsUrlPrefix;

  public GcloudGcsStorage(String bucketName) {
    this.gsUrlPrefix = "gs://" + bucketName + "/";
  }

  @Override
  public void storeFile(Path localFile, String filename) throws IOException {
    logger.finer("Storing local file: " + localFile + " with filename: " + filename);
    gsutil("cp", localFile.toString(), gsUrlPrefix + filename);
  }

  @Override
  public void loadFile(String filename, Path target) throws IOException {
    logger.finer("Loading filename: " + filename + " to target: " + target);
    gsutil("cp", gsUrlPrefix + filename, target.toString());
  }

  @Override
  public void deleteFile(String filename) throws IOException {
    logger.finer("Deleting filename: " + filename);
    gsutil("rm", gsUrlPrefix + filename);
  }

  @Override
  public List<String> listFiles() throws IOException {
    List<String> gsutilOut = gsutil("ls", gsUrlPrefix);
    final int urlPrefixLength = gsUrlPrefix.length();
    List<String> files = new ArrayList<>(gsutilOut.size());
    for (String file : gsutilOut) {
      // remove gs://bucket/ from the beginning of every filename
      file = file.substring(urlPrefixLength);
      if (!Objects.equals(file, LAST_BACKUP_FILE) && !Objects.equals(file, ALL_EXISTING_FILES)) { // exclude internal files
        files.add(file);
      }
    }
    return files;
  }

  @Override
  public List<String> findLatestBackup() throws IOException {
    List<String> content = null;
    try {
      content = gsutil("cat", gsUrlPrefix + LAST_BACKUP_FILE);
    } catch (IOException e) {
      logger.log(Level.FINE, "Exception while loading last-backup file", e);
      return null;
    }
    if (content.isEmpty()) {
      logger.fine("Last-backup file is empty, no backups available.");
      return null;
    }
    List<String> files = new LinkedList<>();
    for (String line : content) {
      if (!line.trim().isEmpty() && !line.startsWith(COMMENT_PREFIX)) {
        files.add(line.trim());
      }
    }
    return files;
  }

  @Override
  public List<String> listMetadataForExistingFiles() throws IOException {
    List<String> content = null;
    List<String> files = new LinkedList<>();
    try {
      content = gsutil("cat", gsUrlPrefix + ALL_EXISTING_FILES);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Exception while loading existing file. Files previously deleted may load", e);
      return files;
    }
    if (content.isEmpty()) {
      logger.fine("Existing is empty. Looks like deleted files may load");
      return files;
    }
    for (String line : content) {
      if (!line.trim().isEmpty() && !line.startsWith(COMMENT_PREFIX)) {
        files.add(line.trim());
      }
    }
    return files;
  }

  

  @Override
  public void updateLastBackup(List<String> filenames) throws IOException {
    logger.fine("Updating last-backup file.");
    List<String> content = new ArrayList<>(filenames.size() + 1);
    content.add(COMMENT_LINE);
    content.addAll(filenames);
    final Path tempDirectory = Files.createTempDirectory(TMP_DIR_PREFIX);
    final Path tempFilePath = tempDirectory.resolve(LAST_BACKUP_FILE);
    logger.fine("Using temp file: " + tempFilePath);
    try {
      Files.write(tempFilePath, content, StandardCharsets.UTF_8);
      gsutil("cp", tempFilePath.toString(), gsUrlPrefix + LAST_BACKUP_FILE);
    } finally {
      logger.fine("Cleaning up temp file & directory.");
      Files.deleteIfExists(tempFilePath);
      Files.deleteIfExists(tempDirectory);
    }
  }

  /* (non-Javadoc)
   * @see com.google.jenkins.plugins.persistentmaster.storage.Storage#updateExistingFilesMetaData(java.util.List)
   */
  @Override
  public void updateExistingFilesMetaData(List<String> filenames) throws IOException {
    logger.fine("Updating existing files meta data.");
    logger.info("Updating existing files meta data.");
    List<String> content = new ArrayList<>(filenames.size() + 1);
    content.add(COMMENT_LINE);
    content.addAll(filenames);
    final Path tempDirectory = Files.createTempDirectory(TMP_DIR_PREFIX);
    final Path tempFilePath = tempDirectory.resolve(ALL_EXISTING_FILES);
    logger.fine("Using temp file: " + tempFilePath);
    try {
      Files.write(tempFilePath, content, StandardCharsets.UTF_8);
      gsutil("cp", tempFilePath.toString(), gsUrlPrefix + ALL_EXISTING_FILES);
    } finally {
      logger.fine("Cleaning up temp file & directory.");
      Files.deleteIfExists(tempFilePath);
      Files.deleteIfExists(tempDirectory);
    }
  }
    
  private List<String> gsutil(String... params) throws IOException {
    ProcessBuilder builder = new ProcessBuilder(GSUTIL_CMD);
    for (String param : params) {
      builder.command().add(param);
    }
    builder.redirectErrorStream(true);
    List<String> output = new LinkedList<>();
    Process process = builder.start();
    try (
        BufferedReader out = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      while ((line = out.readLine()) != null) {
        output.add(line);
      }
    }
    int returnCode;
    try {
      returnCode = process.waitFor();
    } catch (InterruptedException e) {
      throw new IOException("Could not wait for sub-process", e);
    }
    if (returnCode != 0) {
      throw new IOException("gsutil failed: " + joinStrings(output));
    }
    return output;
  }

  private static String joinStrings(List<String> output) {
    StringBuilder sb = new StringBuilder();
    for (String line : output) {
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    GcloudGcsStorage that = (GcloudGcsStorage) o;

    if (!gsUrlPrefix.equals(that.gsUrlPrefix)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return com.google.common.base.Objects.hashCode(gsUrlPrefix);
  }

  @Override
  public String toString() {
    return "GcloudGcsStorage{"
        + "gsUrlPrefix='" + gsUrlPrefix + '\'' + '}';
  }

}
