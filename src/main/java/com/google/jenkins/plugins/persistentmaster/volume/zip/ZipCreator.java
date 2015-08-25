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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;
import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import com.google.common.base.Preconditions;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * Implementation of {@link Volume.Creator} for ZIP volumes.
 */
class ZipCreator implements Volume.Creator {

  private static final Logger logger =
      Logger.getLogger(ZipCreator.class.getName());

  private static final String UTF_8 = "UTF-8";

  private final ZipArchiveOutputStream zipStream;
  private final Path zipPath;
  private boolean closed = false;
  private int fileCount = 0;

  ZipCreator(Path zip) throws IOException {
    zipPath = Preconditions.checkNotNull(zip);
    Preconditions.checkArgument(!Files.exists(zipPath), "zip file exists");
    logger.finer("Creating zip volume for path: " + zipPath);
    zipStream = new ZipArchiveOutputStream(
        Files.newOutputStream(zipPath, StandardOpenOption.CREATE_NEW));
    // unfortunately there is no typesafe way of doing this
    zipStream.setEncoding(UTF_8);
    zipStream.setUseZip64(Zip64Mode.AsNeeded);
  }

  @Override
  public void addFile(Path file, String pathInVolume,
      @Nullable BasicFileAttributes attrs) throws IOException {
    Preconditions.checkState(!closed, "Volume closed");
    if (attrs == null) {  // make sure attrs are available
      attrs = Files.readAttributes(file, BasicFileAttributes.class);
    }
    if (attrs.isSymbolicLink()) {
      copySymlink(file, pathInVolume);
    } else if (attrs.isDirectory()) {
      copyDirectory(pathInVolume);
    } else {
      copyRegularFile(file, pathInVolume);
    }
    fileCount++;
  }

  private void copySymlink(Path file, String filenameInZip) throws IOException {
    logger.finer("Adding symlink: " + file + " with filename: "
        + filenameInZip);
    Path symlinkTarget = Files.readSymbolicLink(file);
    // Unfortunately, there is no API method to create a symlink in a ZIP file,
    // however, a symlink entry can easily be created by hand.
    // The requirements for a symlink entry are:
    //  - the unix mode must have the LINK_FLAG set
    //  - the content must contain the target of the symlink as UTF8 string
    ZipArchiveEntry entry = new ZipArchiveEntry(filenameInZip);
    entry.setUnixMode(entry.getUnixMode() | UnixStat.LINK_FLAG);
    zipStream.putArchiveEntry(entry);
    zipStream.write(symlinkTarget.toString().getBytes(StandardCharsets.UTF_8));
    zipStream.closeArchiveEntry();
  }

  private void copyDirectory(String filenameInZip) throws IOException {
    logger.finer("Adding directory: " + filenameInZip);
    // entries ending in / indicate a directory
    ZipArchiveEntry entry = new ZipArchiveEntry(filenameInZip + "/");
    // in addition, set the unix directory flag
    entry.setUnixMode(entry.getUnixMode() | UnixStat.DIR_FLAG);
    zipStream.putArchiveEntry(entry);
    zipStream.closeArchiveEntry();
  }

  private void copyRegularFile(Path file, String filenameInZip)
      throws IOException {
    logger.finer("Adding file: " + file + " with filename: " + filenameInZip);
    ZipArchiveEntry entry = new ZipArchiveEntry(filenameInZip);
    zipStream.putArchiveEntry(entry);
    Files.copy(file, zipStream);
    zipStream.closeArchiveEntry();
  }

  @Override
  public int getFileCount() {
    return fileCount;
  }

  @Override
  public void close() throws IOException {
    Preconditions.checkState(!closed, "Volume already closed");
    logger.finer("Closing zip creator for path: " + zipPath);
    zipStream.close();
    closed = true;
  }

}
