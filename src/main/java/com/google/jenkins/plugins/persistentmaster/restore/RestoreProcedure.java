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
package com.google.jenkins.plugins.persistentmaster.restore;

import com.google.jenkins.plugins.persistentmaster.initiation.InitiationStrategy;
import com.google.jenkins.plugins.persistentmaster.scope.Scope;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * The restore procedure restores the jenkins environment from a previous
 * backup, or if no previous backup is available, creates a new environment
 * for jenkins.
 */
public class RestoreProcedure {

  private static final Logger logger =
      Logger.getLogger(RestoreProcedure.class.getName());

  private static final String TMP_DIR_PREFIX
      = "persistent-master-restore-tmp";

  private final Volume volume;
  private final Scope scope;
  private final Storage storage;
  private final InitiationStrategy initiationStrategy;
  private final Path jenkinsHome;
  private final Path scratchDir;
  private final boolean overwrite;

  public RestoreProcedure(
      Volume volume, Scope scope, Storage storage,
      InitiationStrategy initiationStrategy, Path jenkinsHome,
      @Nullable Path scratchDir, boolean overwrite) {
    this.volume = volume;
    this.scope = scope;
    this.storage = storage;
    this.initiationStrategy = initiationStrategy;
    this.jenkinsHome = jenkinsHome;
    this.scratchDir = scratchDir;
    this.overwrite = overwrite;
  }

  public void performRestore() throws IOException {
    if (storage == null) {
      logger.warning(
          "No backup location configured, initializing new environment");
      initiationStrategy.initializeNewEnvironment(jenkinsHome);
      logger.fine("Finished environment setup for jenkins");
      return;
    }

    List<String> existingFileMetadata = storage.listMetadataForExistingFiles();
    logger.info("Listing the existing file names " + existingFileMetadata.size());
    List<String> latestBackupFiles = storage.findLatestBackup();
    
    if (latestBackupFiles == null || latestBackupFiles.isEmpty()) {
      logger.warning("No backup files found, initializing new environment");
      initiationStrategy.initializeNewEnvironment(jenkinsHome);
    } else {
      String finalBackupFile = latestBackupFiles.get(
          latestBackupFiles.size() - 1);
      logger.info("Restoring from backup files up to: " + finalBackupFile);
      if (scratchDir != null) {
        // This is a no-op if the scratch directory already exists.
        Files.createDirectories(scratchDir);
      }
      final Path tempDirectory = scratchDir == null
          ? Files.createTempDirectory(TMP_DIR_PREFIX)
          : Files.createTempDirectory(scratchDir, TMP_DIR_PREFIX);
      logger.fine("Using temp directory: " + tempDirectory);
      try {
        parallelFetchAndExtract(latestBackupFiles, existingFileMetadata, tempDirectory);
      } finally {
        // cleanup after ourselves
        try {
          logger.fine("Deleting temp directory: " + tempDirectory);
          Files.deleteIfExists(tempDirectory);
        } catch (IOException e) {
          // be silent about cleanup errors, only log them
          logger.log(Level.FINE, "IOException while performing cleanup", e);
        }
      }
      logger.fine("Backup restored, initializing restored environment");
      initiationStrategy.initializeRestoredEnvironment(jenkinsHome,
          finalBackupFile);
    }
    logger.fine("Finished environment setup for jenkins");
  }

  private void parallelFetchAndExtract(List<String> latestBackupFiles,List<String> existingFileMetadata,
      Path tempDirectory) throws IOException {
   
   
    // A ForkJoinPool should usually be shared, rather than creating a new one
    // every time. However, since the RestoreProcedure is only ever invoked
    // once per VM, creating a shared pool is really not necessary.
    ForkJoinPool forkJoinPool = new ForkJoinPool();
    try {
      forkJoinPool.invoke(new FetchExtractChain(latestBackupFiles, existingFileMetadata, storage,
          volume, scope, tempDirectory, jenkinsHome, overwrite));
    } catch (RuntimeException e) {
      // fork join pool wraps original exception in RuntimeException(s)
      Throwable cause = e.getCause();
      while (cause != null) {
        if (cause instanceof IOException) {
          throw (IOException) cause;
        }
        cause = cause.getCause();
      }
      throw e;  // IOException not found, rethrow original exception
    } finally {
      forkJoinPool.shutdown();
    }
  }

  /**
   * A ForkJoinTask that creates and forks a {@link FetchExtractTask} for every
   * backup, which should be fetched and extracted.
   *
   * Every {@link FetchExtractTask} will get a reference to the preceding
   * {@link FetchExtractTask}, in order to be able to perform backup extraction
   * in order, however, fetching will be performed in parallel.
   */
  private static class FetchExtractChain extends ForkJoinTask<Void> {

    private final List<String> latestBackupFiles;
    private final List<String> existingFileMetadata;
    private final Storage storage;
    private final Volume volume;
    private final Scope scope;
    private final Path tempDirectory;
    private final Path jenkinsHome;
    private final boolean overwrite;

    private FetchExtractChain(
        List<String> latestBackupFiles, List<String> existingFileMetadata, Storage storage, Volume volume,
        Scope scope, Path tempDirectory, Path jenkinsHome, boolean overwrite) {
      this.latestBackupFiles = latestBackupFiles;
      this.existingFileMetadata = existingFileMetadata;
      this.storage = storage;
      this.volume = volume;
      this.scope = scope;
      this.tempDirectory = tempDirectory;
      this.jenkinsHome = jenkinsHome;
      this.overwrite = overwrite;
    }

    @Override
    public Void getRawResult() {
      return null;
    }

    @Override
    protected void setRawResult(Void value) {
    }

    @Override
    protected boolean exec() {
      FetchExtractTask previousTask = null;
      logger.fine("Loading backup volume(s) from storage");
      for (Iterator<String> it = latestBackupFiles.iterator(); it.hasNext(); ) {
        String file = it.next();
        FetchExtractTask fetchExtractTask = new FetchExtractTask(previousTask,
            file,  existingFileMetadata, volume, scope, storage, jenkinsHome, tempDirectory,
            overwrite);
        if (it.hasNext()) {
          fetchExtractTask.fork();
        } else {
          fetchExtractTask.invoke(); // invoke the last task directly
        }
        previousTask = fetchExtractTask;
      }
      return true;
    }

  }

  /**
   * A ForkJoinTask implementing fetching and extracting of a particular
   * backup file.
   *
   * The task will immediately begin fetching the backup file, possibly in
   * parallel with other {@link FetchExtractTask}s. After that it will join
   * its preceding {@link FetchExtractTask} (if available) in order to wait
   * for its completion, because backup extraction must be performed in order.
   * As soon as the preceding task is finished extracting its backup file,
   * this task will extract its backup file and finish (which again will trigger
   * the extraction process of the next task in line).
   */
  private static class FetchExtractTask extends ForkJoinTask<Void> {

    private final FetchExtractTask previousTask;
    private final String backupFile;
    private final List<String> existingFileMetadata;
    private final Volume volume;
    private final Scope scope;
    private final Storage storage;
    private final Path jenkinsHome;
    private final Path tempDirectory;
    private final boolean overwrite;

    public FetchExtractTask(FetchExtractTask previousTask, String backupFile, List<String> existingFileMetadata,
        Volume volume, Scope scope, Storage storage, Path jenkinsHome,
        Path tempDirectory, boolean overwrite) {
      this.previousTask = previousTask;
      this.backupFile = backupFile;
      this.existingFileMetadata = existingFileMetadata;
      this.volume = volume;
      this.scope = scope;
      this.storage = storage;
      this.jenkinsHome = jenkinsHome;
      this.tempDirectory = tempDirectory;
      this.overwrite = overwrite;
    }

    @Override
    public Void getRawResult() {
      return null;
    }

    @Override
    protected void setRawResult(Void value) {
    }

    @Override
    protected boolean exec() {
      Path volumePath = tempDirectory.resolve(backupFile);

      try {
        logger.fine("Fetching backup volume for backup file: " + volumePath);
        storage.loadFile(backupFile, volumePath);

        // previous task must finish before this one can start extracting
        if (previousTask != null) {
          logger.fine("Waiting for previous task to complete");
          previousTask.join();
        }

        logger.fine("Extracting backup volume");
        try (Volume.Extractor extractor = volume.extract(volumePath)) {
          scope.extractFiles(jenkinsHome, extractor, overwrite, existingFileMetadata);
        }  // auto-close extractor
      } catch (IOException e) {
        completeExceptionally(e);  // causes consecutive tasks to stop
        return false;
      } finally {
        // cleanup after ourselves
        try {
          Files.deleteIfExists(volumePath);
        } catch (IOException e) {
          // be silent about cleanup errors, only log them
          logger.log(Level.FINE, "IOException while performing cleanup", e);
        }
      }
      return true;
    }

  }

}
