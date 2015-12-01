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
package com.google.jenkins.plugins.persistentmaster;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests for {@link VersionComparator}.
 */
public class VersionComparatorTest {
  private VersionComparator versionComparator = VersionComparator.get();

  @Test
  public void testNullOrder() {
    assertTrue(versionComparator.compare(null, "2") < 0);
    assertTrue(versionComparator.compare("2", null) > 0);
    assertTrue(versionComparator.compare(null, null) == 0);
  }
  
  @Test
  public void testBasicNumericOrder() {
    assertTrue(versionComparator.compare("1", "2") < 0);
    assertTrue(versionComparator.compare("2", "1") > 0);
    assertTrue(versionComparator.compare("1", "1") == 0);
    assertTrue(versionComparator.compare("1", "12") < 0);
    assertTrue(versionComparator.compare("2", "12") < 0);
  }

  @Test
  public void testSubversionNumericOrder() {
    assertTrue(versionComparator.compare("1.1", "1.2") < 0);
    assertTrue(versionComparator.compare("1.2", "1.1") > 0);
    assertTrue(versionComparator.compare("1.1", "1.1") == 0);

    assertTrue(versionComparator.compare("1.1", "2.1") < 0);
    assertTrue(versionComparator.compare("1.2", "2.1") < 0);
    assertTrue(versionComparator.compare("1.12", "1.1") > 0);
  }

  @Test
  public void testSubversionLengthOrder() {
    assertTrue(versionComparator.compare("1.1", "1.1.1") < 0);
    assertTrue(versionComparator.compare("1.2", "1.1.1") > 0);
  }

  @Test
  public void testNumericLessThanNonNumeric() {
    assertTrue(versionComparator.compare("1.1", "1.a") < 0);
    assertTrue(versionComparator.compare("1.2", "1.1a") < 0);
    assertTrue(versionComparator.compare("1.2", "1.-") < 0);
    assertTrue(versionComparator.compare("1.2", "1. ") < 0);
  }

  @Test
  public void testNonNumericOrder() {
    assertTrue(versionComparator.compare("a", "b") < 0);
    assertTrue(versionComparator.compare("B", "a") < 0);
    assertTrue(versionComparator.compare("a", "ab") < 0);
    assertTrue(versionComparator.compare("1.1a", "1.1b") < 0);
  }

  @Test
  public void testMixedNumericNonNumeric() {
    assertTrue(versionComparator.compare("2.1b", "3.1a") < 0);
    assertTrue(versionComparator.compare("3.1.3", "3.1a.2beta") < 0);
    assertTrue(versionComparator.compare("1.1a.2", "1.1b.1") < 0);
  }
}
