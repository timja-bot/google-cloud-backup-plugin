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

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.util.FormValidation;

/**
 * Tests for {@link GcloudGcsStorageProvider}.
 *
 * @author akshayd@google.com (Akshay Dayal).
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(GcloudGcsStorageProvider.class)
public class GcloudGcsStorageProviderTest {

  private static final String BUCKET = "bucket";

  @Test
  public void testConstructor() {
    GcloudGcsStorageProvider provider = new GcloudGcsStorageProvider(BUCKET);
    GcloudGcsStorage expectedStorage = new GcloudGcsStorage(BUCKET);

    assertEquals(BUCKET, provider.getBucket());
    assertEquals(expectedStorage, provider.getStorage());
  }

  @Test
  public void testValidateBucket() {
    // Empty values.
    assertNotNull(GcloudGcsStorageProvider.validateBucket(null));
    assertNotNull(GcloudGcsStorageProvider.validateBucket(""));
    assertNotNull(GcloudGcsStorageProvider.validateBucket("    "));

    // Valid value.
    assertNull(GcloudGcsStorageProvider.validateBucket("bucket"));
  }

  @Test
  public void testIsValidSuccess() {
    GcloudGcsStorageProvider provider = new GcloudGcsStorageProvider(BUCKET);
    mockStatic(GcloudGcsStorageProvider.class);
    when(GcloudGcsStorageProvider.validateBucket(BUCKET)).thenReturn(null);

    assertTrue(provider.isValid());
  }

  @Test
  public void testIsValidFailure() {
    GcloudGcsStorageProvider provider = new GcloudGcsStorageProvider(BUCKET);
    mockStatic(GcloudGcsStorageProvider.class);
    when(GcloudGcsStorageProvider.validateBucket(BUCKET)).thenReturn("error");

    assertFalse(provider.isValid());
  }

  @Test
  public void testDoCheckBucketSuccess() {
    GcloudGcsStorageProvider.GcloudGcsStorageProviderDescriptor descriptor =
        new GcloudGcsStorageProvider.GcloudGcsStorageProviderDescriptor();
    mockStatic(GcloudGcsStorageProvider.class);
    when(GcloudGcsStorageProvider.validateBucket(BUCKET)).thenReturn(null);

    assertEquals(FormValidation.Kind.OK, descriptor.doCheckBucket(BUCKET).kind);
  }

  @Test
  public void testDoCheckBucketFailure() {
    GcloudGcsStorageProvider.GcloudGcsStorageProviderDescriptor descriptor =
        new GcloudGcsStorageProvider.GcloudGcsStorageProviderDescriptor();
    mockStatic(GcloudGcsStorageProvider.class);
    when(GcloudGcsStorageProvider.validateBucket(BUCKET)).thenReturn("error");

    assertEquals(FormValidation.Kind.ERROR,
        descriptor.doCheckBucket(BUCKET).kind);
  }
}
