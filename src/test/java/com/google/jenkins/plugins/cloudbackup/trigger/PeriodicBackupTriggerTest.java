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
package com.google.jenkins.plugins.cloudbackup.trigger;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link PeriodicBackupTrigger}.
 */
public class PeriodicBackupTriggerTest {

  private PeriodicBackupTrigger periodicBackupTrigger;
  private DateTime now;

  @Before
  public void setUp() throws Exception {
    now = new DateTime(DateTimeZone.UTC);
    periodicBackupTrigger = new PeriodicBackupTrigger(Minutes.minutes(5));
  }

  @After
  public void tearDown() throws Exception {
    periodicBackupTrigger = null;
    now = null;
  }

  @Test
  public void testNow_shouldReturnFalse() throws Exception {
    Assert.assertFalse(periodicBackupTrigger.shouldCreateBackup(now));
  }

  @Test
  public void testLessThan5minAgo_shouldReturnFalse() throws Exception {
    DateTime nowMinus4Min = now.minusMinutes(5).plusSeconds(1);
    Assert.assertFalse(periodicBackupTrigger.shouldCreateBackup(nowMinus4Min));
  }

  @Test
  public void testMoreThan5minAgo_shouldReturnTrue() throws Exception {
    DateTime nowMinus5Min = now.minusMinutes(5);
    Assert.assertTrue(periodicBackupTrigger.shouldCreateBackup(nowMinus5Min));
  }

}