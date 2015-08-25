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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import com.google.common.base.Preconditions;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * Implementation of {@link Volume.Extractor} for ZIP volumes.
 */
class ZipExtractor implements Volume.Extractor {

  private static final Logger logger =
      Logger.getLogger(ZipExtractor.class.getName());

  private final ZipFile zipFile;
  private final Path zipPath;
  private boolean closed = false;

  ZipExtractor(Path zip) throws IOException {
    zipPath = Preconditions.checkNotNull(zip);
    logger.finer("Extracting zip volume for path: " + zipPath);
    zipFile = new ZipFile(zipPath.toFile());
  }

  @Override
  public Iterator<Volume.Entry> iterator() {
    final Enumeration<ZipArchiveEntry> entriesInPhysicalOrder =
        zipFile.getEntriesInPhysicalOrder();
    return new Iterator<Volume.Entry>() {
      @Override
      public boolean hasNext() {
        Preconditions.checkState(!closed, "Volume closed");
        return entriesInPhysicalOrder.hasMoreElements();
      }

      @Override
      public Volume.Entry next() {
        Preconditions.checkState(!closed, "Volume closed");
        // nextElement() will throw NoSuchElementException if no next
        // element exists
        return new ZipVolumeEntry(zipFile,
            entriesInPhysicalOrder.nextElement());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException(
            "remove is not supported by this iterator");
      }
    };
  }

  @Override
  public void close() throws IOException {
    Preconditions.checkState(!closed, "Volume already closed");
    logger.finer("Closing zip extractor for path: " + zipPath);
    zipFile.close();
    closed = true;
  }

  /**
   * Represents an entry in a ZIP volume.
   */
  private static class ZipVolumeEntry implements Volume.Entry {

    private final ZipFile zipFile;
    private final ZipArchiveEntry zipArchiveEntry;

    private ZipVolumeEntry(ZipFile zipFile, ZipArchiveEntry zipArchiveEntry) {
      this.zipFile = zipFile;
      this.zipArchiveEntry = zipArchiveEntry;
    }

    @Override
    public String getName() {
      String entryName = zipArchiveEntry.getName();
      // directories in ZIP files are defined to end in /
      if (isDirectory() && entryName.endsWith("/")) {
        entryName = entryName.substring(0, entryName.length() - 1);
      }
      return entryName;
    }

    @Override
    public boolean isDirectory() {
      return zipArchiveEntry.isDirectory();
    }

    @Override
    public boolean isSymlink() {
      return zipArchiveEntry.isUnixSymlink();
    }

    @Override
    public void extractTo(Path target, boolean overwrite) throws IOException {
      if (!overwrite && Files.exists(target)) {
        logger.finer("Path already exists, skipping extraction: " + target);
        return;
      }
      if (isDirectory()) {
        extractDirectory(target);
      } else {
        Path parentDir = target.getParent();
        if (parentDir != null) {
          Files.createDirectories(parentDir);
        }
        if (isSymlink()) {
          extractSymlink(target);
        } else {
          extractRegularFile(target);
        }
      }
    }

    private void extractDirectory(Path target) throws IOException {
      logger.finer("Extracting directory: " + target);
      Files.createDirectories(target);
    }

    private void extractSymlink(Path target) throws IOException {
      logger.finer("Extracting symlink: " + target);
      String unixSymlink = zipFile.getUnixSymlink(zipArchiveEntry);
      Path symlinkPath = target.getFileSystem().getPath(unixSymlink);
      Files.deleteIfExists(target);
      Files.createSymbolicLink(target, symlinkPath);
    }

    private void extractRegularFile(Path target) throws IOException {
      logger.finer("Extracting file: " + target);
      try (InputStream inputStream = zipFile.getInputStream(zipArchiveEntry)) {
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
      }
    }

  }

}
