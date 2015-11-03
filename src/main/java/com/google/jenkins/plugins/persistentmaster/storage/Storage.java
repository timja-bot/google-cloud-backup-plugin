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
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Specifies a storage provider for storing and retrieving backups volumes.
 */
public interface Storage {

  /**
   * Store the given local file in the storage using the given filename.
   *
   * @param localFile a path to the local file that should be stored.
   * @param filename the filename to use for the file in the storage.
   * @throws IOException if storing the local file fails.
   */
  public void storeFile(Path localFile, String filename) throws IOException;

  /**
   * Load the file identified by the given filename from the storage provider
   * to the given local path.
   *
   * @param filename the filename of the file in the storage.
   * @param target the local target path to store the file.
   * @throws IOException if loading the file fails.
   */
  public void loadFile(String filename, Path target) throws IOException;

  /**
   * Delete the file from the storage.
   *
   * @param filename the filename of the file in the storage.
   * @throws IOException if deleting the file fails.
   */
  public void deleteFile(String filename) throws IOException;

  /**
   * Returns a list of the filenames of all files in the storage. Files that are
   * used internally by the storage implementation will be excluded.
   *
   * @return list of the filenames of all files in the storage.
   * @throws IOException if listing the file names fails.
   */
  public List<String> listFiles() throws IOException;

  /**
   * Finds the filenames of the latest backup stored in the storage.
   *
   * @return the filename of the latest backup stored in the storage.
   * @throws IOException if retrieving the filename of the latest backup fails.
   */
  public List<String> findLatestBackup() throws IOException;

  /**
   * Book keeping of all the files that currently exist in the backup.
   *
   * @return the filenames of all files that exist in the most recent backup.
   * @throws IOException if retrieving this list fails.
   */
  public List<String> listMetadataForExistingFiles() throws IOException;
  
  /**
   * Updates the filename of the latest backup stored in the storage.
   *
   * @param filenames the filenames of the latest backup stored in the storage.
   * @throws IOException if updating the filename of the latest backup fails.
   */
  public void updateLastBackup(List<String> filenames) throws IOException;
  
  /**
   * Updates the filenames of all files that currently exist in the volume
   *
   * @param filenames the filenames of all the files currently in the volume.
   * @throws IOException if updating the filename of the latest backup fails.
   */
  public void updateExistingFilesMetaData(Set<String> filenames) throws IOException;

}
