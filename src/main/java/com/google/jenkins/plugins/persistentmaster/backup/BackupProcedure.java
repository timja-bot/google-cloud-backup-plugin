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
package com.google.jenkins.plugins.persistentmaster.backup;

import com.google.jenkins.plugins.persistentmaster.VersionUtility;
import com.google.jenkins.plugins.persistentmaster.history.BackupHistory;
import com.google.jenkins.plugins.persistentmaster.scope.Scope;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * The procedure that actually performs a backup.
 *
 * In order to perform a backup it employs the configured extension point
 * implementations for {@link Volume}, {@link Scope}, {@link Storage}, and
 * {@link BackupHistory}.
 */
public class BackupProcedure {

  private static final Logger logger =
      Logger.getLogger(BackupProcedure.class.getName());

  private static final String TMP_DIR_PREFIX
      = "google-cloud-backup-plugin";

  private final Volume volume;
  private final Scope scope;
  private final Storage storage;
  private final BackupHistory backupHistory;
  private final Path jenkinsHome;
  private final Path tempDir;
  private final String backupNameSuffix;

  /**
   * Creates a new backup procedure with the given extension point
   * implementations.
   *
   * @param volume the {@link Volume} to use for the backup.
   * @param scope the {@link Scope} to use for the backup.
   * @param storage  the {@link Storage} to use for the backup.
   * @param backupHistory the {@link BackupHistory} to use for the backup.
   * @param jenkinsHome path to the JENKINS_HOME directory.
   * @param tempDir optional path to the directory to use for scratch files.
   * @param backupNameSuffix an optional suffix to the backup filename
   */
  public BackupProcedure(
      Volume volume, Scope scope, Storage storage,
      BackupHistory backupHistory, Path jenkinsHome,
      @Nullable Path tempDir,
      @Nullable String backupNameSuffix) {
    this.volume = volume;
    this.scope = scope;
    this.storage = storage;
    this.backupHistory = backupHistory;
    this.jenkinsHome = jenkinsHome;
    this.tempDir = tempDir;
    this.backupNameSuffix = backupNameSuffix;
  }

  /**
   * Performs a backup employing the extension point implementations provided
   * in the constructor.
   *
   * @return the date and time the backup was taken.
   * @throws IOException if backup creation fails.
   */
  public DateTime performBackup() throws IOException {
    logger.fine("Start creating backup");
    final DateTime backupTime = new DateTime(DateTimeZone.UTC);
    // This is a no-op if the scratch directory already exists.
    if (tempDir != null) {
      Files.createDirectories(tempDir);
    }
    final Path tempDirectory = tempDir == null
        ? Files.createTempDirectory(TMP_DIR_PREFIX)
        : Files.createTempDirectory(tempDir, TMP_DIR_PREFIX);
    final String backupVolumeName = calculateBackupName(backupTime)
        + (backupNameSuffix != null ? backupNameSuffix : "")
        + "." + volume.getFileExtension();
    logger.fine("Using temporary directory: " + tempDirectory);
    final Path volumePath = tempDirectory.resolve(Paths.get(backupVolumeName));

    try {
      logger.fine("Creating backup volume");
      int fileCount;
      Set<String> existingFileNames = new HashSet<>();
      try (Volume.Creator creator = volume.createNew(volumePath)) {
        scope.addFiles(jenkinsHome, creator, existingFileNames);
        fileCount = creator.getFileCount();
      } // auto-close creator

      if (fileCount > 0) {
        logger.fine("Storing backup volume");
        storage.storeFile(volumePath, backupVolumeName);

        logger.fine("Updating last backup reference");
        storage.updateLastBackup(Arrays.asList(backupVolumeName));

        logger.fine("Applying backup history policy");
        backupHistory.processHistoricBackups(storage, backupVolumeName);
      } else {
        logger.fine("Volume is empty, will skip storing backup");
      }

      // This must happen after processHistoricBackups, which can delete files.
      logger.fine("Updating list of existing files : Size " + existingFileNames.size());
      storage.updateExistingFilesMetaData(existingFileNames);

      String version = VersionUtility.getFileSystemVersion(jenkinsHome);
      logger.fine("Updating version : " + version);
      storage.updateVersionInfo(version);
    } finally {
      // cleanup after ourselves
      try {
        logger.fine("Deleting local backup volume");
        Files.deleteIfExists(volumePath);
        logger.fine("Deleting temp directory");
        Files.deleteIfExists(tempDirectory);
      } catch (IOException e) {
        // be silent about cleanup errors, only log them
        logger.log(Level.FINE, "IOException while performing cleanup", e);
      }
    }
    logger.fine("Finished creating backup");
    return backupTime;
  }

  private static String calculateBackupName(DateTime backupTime) {
    return String.format("backup-%d%02d%02d%02d%02d%02d",
        backupTime.getYear(), backupTime.getMonthOfYear(),
        backupTime.getDayOfMonth(), backupTime.getHourOfDay(),
        backupTime.getMinuteOfHour(), backupTime.getSecondOfMinute());
  }

}
