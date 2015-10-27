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
package com.google.jenkins.plugins.persistentmaster.scope;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.jenkins.plugins.persistentmaster.volume.ForwardingVolumeCreator;
import com.google.jenkins.plugins.persistentmaster.volume.ForwardingVolumeEntry;
import com.google.jenkins.plugins.persistentmaster.volume.ForwardingVolumeExtractor;
import com.google.jenkins.plugins.persistentmaster.volume.Volume;

/**
 * A {@link Scope} implementation that combines multiple other {@link Scope}s
 * into one {@link Scope} by putting them into respective subdirectories in the
 * Volume.
 */
public class MultiScope implements Scope {

  private final List<SubScope> subScopes = new LinkedList<>();

  /**
   * Add the given {@link Scope} with the given prefix to the definition of this
   * {@link MultiScope}.
   *
   * @param scope the {@link Scope} to add to this {@link MultiScope}.
   * @param volumePrefix the prefix which should be added to the given
   *                     {@link Scope}.
   */
  public void addSubScope(Scope scope, String volumePrefix) {
    subScopes.add(new SubScope(scope, volumePrefix));
  }

  @Override
  public void addFiles(Path jenkinsHome, Volume.Creator creator, List<String> existingFileNames)
      throws IOException {
    for (final SubScope subScope : subScopes) {
      subScope.getScope()
          .addFiles(jenkinsHome, new ForwardingVolumeCreator(creator) {
            @Override
            public void addFile(
                Path file, String pathInVolume, BasicFileAttributes attrs, List<String> existingFileNames)
                throws IOException {
              super.addFile(
                  file, subScope.getVolumePrefix() + pathInVolume, attrs, existingFileNames);
            }
          }, existingFileNames);
    }
  }

  @Override
  public void extractFiles(final Path jenkinsHome, Volume.Extractor extractor,
      boolean overwrite, List<String> existingFileNames) throws IOException {
    for (final SubScope subScope : subScopes) {
      subScope.getScope()
          .extractFiles(jenkinsHome, new ForwardingVolumeExtractor(extractor) {

            public Iterator<Volume.Entry> iterator() {
              return new SubScopeIterator(subScope, super.iterator());
            }

          }, overwrite, existingFileNames);
    }
  }

  /**
   * An iterator for all entries of a given sub scope, which is contained in a
   * {@link MultiScope}.
   *
   * The iterator is backed by the actual entry iterator of the Volume. It will,
   * however, only return those entries in the Volume that actually belong to
   * the given sub scope.
   */
  private static class SubScopeIterator implements Iterator<Volume.Entry> {

    private final SubScope subScope;
    private final Iterator<Volume.Entry> extractorIterator;
    private Volume.Entry next = null;

    /**
     * Create a new iterator, which is backed by the given iterator, but only
     * returns entries that belong to the given sub scope.
     *
     * @param subScope the sub scope this iterator should handle.
     * @param extractorIterator the actual iterator of the Volume.
     */
    private SubScopeIterator(
        SubScope subScope, Iterator<Volume.Entry> extractorIterator) {
      this.subScope = subScope;
      this.extractorIterator = extractorIterator;
    }

    @Override
    public boolean hasNext() {
      if (next != null) {
        return true;
      }
      while (extractorIterator.hasNext()) {
        next = extractorIterator.next();
        if (next.getName().startsWith(subScope.getVolumePrefix())) {
          return true;
        }
      }
      next = null;
      return false;
    }

    @Override
    public Volume.Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      assert (next != null);
      try {
        return new ForwardingVolumeEntry(next) {
          @Override
          public String getName() {
            int volumePrefixLength = subScope.getVolumePrefix().length();
            return super.getName().substring(volumePrefixLength);
          }
        };
      } finally {
        next = null; // make sure the entry is consumed
      }
    }

    @Override
    public void remove() {
      extractorIterator.remove();
    }
  }

  /**
   * A descriptor for a sub scope, which is contained in a {@link MultiScope}.
   *
   * Instances are created by {@link MultiScope#addSubScope}, and used for
   * storing the internal definition of the {@link MultiScope}.
   */
  private static class SubScope {

    private final Scope scope;
    private final String volumePrefix;

    /**
     * Create a new sub scope definition with the actual {@link Scope} and the
     * prefix to use.
     *
     * @param scope the actual {@link Scope}.
     * @param volumePrefix the prefix to add to all files in the given
     *                     {@link Scope}.
     */
    private SubScope(Scope scope, String volumePrefix) {
      this.scope = scope;
      this.volumePrefix = volumePrefix;
    }

    /**
     * Return the associated {@link Scope}.
     *
     * @return the associated {@link Scope}.
     */
    public Scope getScope() {
      return scope;
    }

    /**
     * Return the associated volume prefix.
     *
     * @return the associated volume prefix.
     */
    public String getVolumePrefix() {
      return volumePrefix;
    }

  }

}
