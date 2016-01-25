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
package com.google.jenkins.plugins.cloudbackup.scope;

import com.google.jenkins.plugins.cloudbackup.volume.Volume;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Utility class for {@link Scope}. Provides methods for conveniently adding a
 * whole file tree to a volume, and for extracting all files in a given volume
 * to a target path.
 */
public final class Scopes {
  private static final Logger logger = Logger.getLogger(Scopes.class.getName());

  private Scopes() {}

  /**
   * Add all files in the given base directory and all subdirectories to the
   * Volume via the given {@link Volume.Creator}.
   *
   * @param basePath the base directory to be added to the Volume.
   * @param creator the {@link Volume.Creator} used for adding files to the
   * Volume.
   * @param excludedDirs files and/or directories which should be excluded.
   * These must be full (absolute) paths.
   * @param existingFileMetadata the source of truth file for what data should be restored
   * @throws IOException if some file operation fails.
   */
  public static void addAllFilesIn(final Path basePath, final Volume.Creator creator,
      final Set<Path> excludedDirs, final Set<String> existingFileMetadata) throws IOException {
    Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        if (attrs.isSymbolicLink()) {
          // don't follow symlinks, rather store them as such
          logger.finer("Skipping symlink directory: " + dir);
          return FileVisitResult.SKIP_SUBTREE;
        } else {
          if (excludedDirs.contains(dir)) {
            logger.finer("Skipping excluded directory: " + dir);
            return FileVisitResult.SKIP_SUBTREE;
          }
          // check for an empty directory, because in that case we must
          // explicitly add the directory to the ZIP file, otherwise it will
          // get lost.
          try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir)) {
            if (!directoryStream.iterator().hasNext()) {
              logger.finer("Adding empty directory: " + dir);
              existingFileMetadata.add(basePath.relativize(dir).toString());
              creator.addFile(dir, basePath.relativize(dir).toString(), attrs);
            }
          } // auto-close directoryStream
          return FileVisitResult.CONTINUE;
        }
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (excludedDirs.contains(file)) {
          logger.finer("Skipping excluded file: " + file);
        } else {
          logger.finer("Adding file: " + file);
          existingFileMetadata.add(basePath.relativize(file).toString());
          creator.addFile(file, basePath.relativize(file).toString(), attrs);
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        if (excludedDirs.contains(file)) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return super.visitFileFailed(file, exc);
      }
    });
  }

  /**
   * Extract all files in the Volume represented by the given {@link
   * Volume.Extractor} to the specified target directory.
   *
   * @param targetDir the target directory for file extraction.
   * @param extractor the {@link Volume.Extractor} used for extracting file from
   * the Volume.
   * @param overwrite whether the operation should overwrite existing files
   * when a conflict is detected.
   * @param existingFileMetadata the source of truth file for what data should be restored
   * @throws IOException if some file operation fails.
   */
  public static void extractAllFilesTo(Path targetDir, Volume.Extractor extractor,
      boolean overwrite, Map<String, Boolean> existingFileMetadataMap) throws IOException {
    // If this is empty, there could be a bug during backup - but we should not block the rest of
    // the restoration. We have already logged this earlier, so just move on
    boolean isExistingFileMetadata = !existingFileMetadataMap.isEmpty();
    for (Volume.Entry entry : extractor) {
      if (isExistingFileMetadata && !existingFileMetadataMap.containsKey(entry.getName())) {
        logger.fine("File found in storage volume with no corresponding entry "
            + "in list of existing files: " + entry.getName());
        continue;
      }
      Path path = targetDir.resolve(entry.getName());
      if (overwrite || !Files.exists(path)) {
        //Either we are overwriting or path does not exist, so going forward we will always restore from backup
        existingFileMetadataMap.put(entry.getName(), true);
      }

      Boolean shouldRestoreFromBackup = existingFileMetadataMap.get(entry.getName());
      if (shouldRestoreFromBackup != null && shouldRestoreFromBackup) {
        entry.extractTo(path);
      }
    }
  }
}
