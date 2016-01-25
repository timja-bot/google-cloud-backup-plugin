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

/**
 * Utils for {@link BackupTrigger}.
 */
public final class BackupTriggers {

  private BackupTriggers() {}

  /**
   * Creates a composite {@link BackupTrigger}, which returns a logical OR
   * of the given {@link BackupTrigger}s' return values.
   *
   * @param t1 first BackupTrigger to include in the OR.
   * @param t2 second BackupTrigger to include in the OR.
   * @param tn optional further BackupTriggers to include in the OR.
   * @return A composite {@link BackupTrigger}, which returns a logical OR
   * of the given {@link BackupTrigger}s' return values.
   */
  public static BackupTrigger or(
      final BackupTrigger t1, final BackupTrigger t2,
      final BackupTrigger... tn) {
    return new BackupTrigger() {
      @Override
      public boolean shouldCreateBackup(DateTime lastBackupTime) {
        boolean retVal =
            t1.shouldCreateBackup(lastBackupTime)
                || t2.shouldCreateBackup(lastBackupTime);
        if (!retVal && tn.length > 0) {
          for (BackupTrigger t : tn) {
            if (t.shouldCreateBackup(lastBackupTime)) {
              retVal = true;
              break;
            }
          }
        }
        return retVal;
      }
    };
  }

  /**
   * Creates a composite {@link BackupTrigger}, which returns a logical AND
   * of the given {@link BackupTrigger}s' return values.
   *
   * @param t1 first BackupTrigger to include in the AND.
   * @param t2 second BackupTrigger to include in the AND.
   * @param tn optional further BackupTriggers to include in the AND.
   * @return A composite {@link BackupTrigger}, which returns a logical AND
   * of the given {@link BackupTrigger}s' return values.
   */
  public static BackupTrigger and(
      final BackupTrigger t1, final BackupTrigger t2,
      final BackupTrigger... tn) {
    return new BackupTrigger() {
      @Override
      public boolean shouldCreateBackup(DateTime lastBackupTime) {
        boolean retVal =
            t1.shouldCreateBackup(lastBackupTime)
                && t2.shouldCreateBackup(lastBackupTime);
        if (retVal && tn.length > 0) {
          for (BackupTrigger t : tn) {
            if (!t.shouldCreateBackup(lastBackupTime)) {
              retVal = false;
              break;
            }
          }
        }
        return retVal;
      }
    };
  }

}
