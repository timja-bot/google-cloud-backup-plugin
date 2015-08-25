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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Handles the details of interaction with the restore log file, used to track
 * restore status and to determine whether it is necessary to restart Jenkins.
 */
public class RestoreLog {

  private static final Logger logger =
      Logger.getLogger(RestoreLog.class.getName());

  private static final Object fileLock = new Object();
  private static final String SUCCESS_MESSAGE
      = " successfully restored at time: ";
  private static final String RESTORE_LOG_FILENAME = ".restore.log";

  private final Path logFilePath;

  public RestoreLog(Path jenkinsHome) {
    logFilePath = jenkinsHome.resolve(RESTORE_LOG_FILENAME);
  }

  public static Object getLock() {
    return fileLock;
  }

  /**
   * Attempts to read the restore log and extract the last backup ID.
   *
   * @return the latest backup ID, or null if no backup ID was found.
   */
  public String getLastBackupId() throws IOException {
    logger.fine("Reading restore log.");
    if (!Files.exists(logFilePath)) {
      logger.fine("No restore log file found.");
      return null;
    }
    if (Files.isDirectory(logFilePath)) {
      throw new IllegalStateException("Expected restore log file at: "
          + logFilePath + ", found directory instead.");
    }
    try (BufferedReader reader = Files.newBufferedReader(logFilePath,
            StandardCharsets.UTF_8)) {
      String result = parseLastBackupId(reader.readLine());
      if (result == null) {
        logger.fine("Could not parse last backup ID from restore log.");
        return null;
      }
      logger.fine("Parsed last backup ID from restore log: " + result);
      return result;
    }
  }

  /**
   * Writes the given backup ID to the restore log as the last successfully
   * restored backup.
   *
   * @param backupId the backup ID to be written.
   */
  public void writeLastBackupId(String backupId) throws IOException {
    if (backupId == null) {
      backupId = "null";
    }
    logger.fine("Creating and writing restore log.");
    if (Files.exists(logFilePath) && Files.isDirectory(logFilePath)) {
      throw new IllegalStateException("Expected restore log file at: "
          + logFilePath + ", found directory instead.");
    }
    try (BufferedWriter writer = Files.newBufferedWriter(logFilePath,
        StandardCharsets.UTF_8)) {
      logger.fine("Writing last backup ID: " + backupId);
      writer.write(getSuccessLogLine(backupId));
    }
  }

  /**
   * Attempts to extract the last backup ID from a given restore log line,
   * according to the format in which this class writes successful backup logs.
   *
   * @param logLine log line to be parsed.
   * @return the latest backup ID, or null if no backup ID was found.
   */
  public static String parseLastBackupId(String logLine) {
    if (logLine == null || logLine.isEmpty()) {
      return null;
    }
    int index = logLine.indexOf(SUCCESS_MESSAGE);
    if (index < 0) {
      return null;
    }
    return logLine.substring(0, index);
  }

  /**
   * Helper method to generate the success log message from a given backup ID.
   */
  private static String getSuccessLogLine(String lastBackupId) {
    return lastBackupId + SUCCESS_MESSAGE
        + new SimpleDateFormat().format(new Date());
  }
}
