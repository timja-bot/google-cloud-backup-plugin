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
package com.google.jenkins.plugins.persistentmaster.volume;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

import javax.annotation.Nullable;

/**
 * A Volume combines many individual files into one container file, and provides
 * methods for creating new volumes and extracting files from existing volumes.
 *
 * Typical volume implementations are ZIP or TAR.
 */
public interface Volume {

  /**
   * Return the file extension that is typically used by container files of this
   * volume. Examples include {@literal zip} or {@literal tar.gz}.
   *
   * @return file extension used by this volume.
   */
  public String getFileExtension();

  /**
   * Create a new volume container, and return a {@link Volume.Creator} to be
   * able to add files to it. The given Path specifies the container file to be
   * created, it must not already exist.
   *
   * @param volume the container file to be created, which must not exist
   * already.
   * @return {@link Volume.Creator} for adding files to the volume.
   * @throws IOException if some file operation fails.
   */
  public Volume.Creator createNew(Path volume) throws IOException;

  /**
   * Open an existing volume, and return an {@link Volume.Extractor} to be able
   * to extract individual volume entries to specific locations.
   *
   * @param volume the existing container file, which should be opened.
   * @return {@link Volume.Extractor} for accessing all entries of the volume
   * and extracting them.
   * @throws IOException if some file operation fails.
   */
  public Volume.Extractor extract(Path volume) throws IOException;

  /**
   * A Creator is used to add files to a new Volume.
   *
   * This class implements Closeable, so it can be used in try-with-resources.
   */
  public static interface Creator extends Closeable {

    /**
     * Add the file the given {@link Path} points at to the Volume, using the
     * given path in the volume as the location where the file should be stored
     * inside the volume. The file can be a directory, a unix symlink, or a
     * regular file.
     *
     * @param file the file to be added to the Volume.
     * @param pathInVolume the path to store the file in the Volume.
     * @param attrs the file attributes of the given file. Can be null.
     * @throws IOException if some file operation fails.
     */
    public void addFile(Path file, String pathInVolume,
        @Nullable BasicFileAttributes attrs) throws IOException;

    /**
     * Returns the number of files that have been added to the volume.
     *
     * @return the number of files that have been added to the volume.
     */
    public int getFileCount();

    /**
     * Finalize Volume creation and write all changes to the file system. After
     * this method finishes, the volume file can be accessed for further
     * processing.
     *
     * This method ends the lifecycle of the Creator instance, thus no files can
     * be added to the Volume after this method has been called.
     *
     * @throws IOException if some file operation fails.
     */
    @Override
    public void close() throws IOException;
  }

  /**
   * An Extractor is used to access entries of an existing Volume, and to
   * extract them.
   *
   * This class implements Closeable, so it can be used in try-with-resources.
   */
  public static interface Extractor extends Iterable<Entry>, Closeable {

    /**
     * Return a new iterator for iterating over all entries in the associated
     * Volume. This method can be called multiple times until {@link
     * Extractor#close()} has been called, and will always return a new iterator
     * iterating over all files in the Volume, regardless of previous iterations
     * and extractions on the same Volume.
     *
     * @return a new iterator for iterating over all entries in the associated
     * Volume.
     */
    @Override
    public Iterator<Entry> iterator();

    /**
     * Finishes the extraction process and closes the underlying volume file
     * streams.
     *
     * This method ends the lifecycle of the Extractor instance. Further method
     * invocations on this Extractor instance will result in an {@link
     * java.lang.IllegalStateException}.
     *
     * @throws IOException if some file operation fails.
     */
    @Override
    public void close() throws IOException;

  }

  /**
   * A Volume entry which can be extracted to a given target Path.
   */
  public static interface Entry {

    /**
     * Returns the name of the entry inside the Volume. The name will contain
     * the whole path to the entry inside the Volume.
     *
     * @return the name of the entry inside the Volume
     */
    public String getName();

    /**
     * Returns true if this entry is a directory, false otherwise.
     *
     * @return true if this entry is a directory, false otherwise.
     */
    public boolean isDirectory();

    /**
     * Returns true if this entry is a symbolic link, false otherwise.
     *
     * @return true if this entry is a symbolic link, false otherwise.
     */
    public boolean isSymlink();

    /**
     * Extract the contents of this entry to the given target path.
     *
     * @param target the target path, where the contents should be extracted.
     * @param overwrite whether the operation should overwrite existing files
     * when a conflict is detected.
     * @throws IOException if some file operation fails.
     */
    public void extractTo(Path target, boolean overwrite) throws IOException;

  }

}
