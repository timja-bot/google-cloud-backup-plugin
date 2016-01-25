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
package com.google.jenkins.plugins.cloudbackup;

import com.google.jenkins.plugins.cloudbackup.initiation.InitiationStrategy;
import com.google.jenkins.plugins.cloudbackup.initiation.NoActionInitiationStrategy;
import com.google.jenkins.plugins.cloudbackup.scope.DefaultBackupScope;
import com.google.jenkins.plugins.cloudbackup.scope.Scope;
import com.google.jenkins.plugins.cloudbackup.storage.GcloudGcsStorage;
import com.google.jenkins.plugins.cloudbackup.storage.Storage;
import com.google.jenkins.plugins.cloudbackup.volume.Volume;
import com.google.jenkins.plugins.cloudbackup.volume.zip.ZipVolume;

/**
 * This class binds base extension points to their respective implementations.
 *
 * Consider it a very lightweight version of guice's module mechanism.
 * This is the base implementation providing the configuration for inter
 * module extension points. Every specific module must provide its subclass
 * of this module for configuring module specific extension points.
 */
public class CloudBackupMainModule {

  private final Volume volume;
  private Scope scope;
  private Storage storage;
  private final InitiationStrategy initiationStrategy;
  private String backupBucketName = null;

  public CloudBackupMainModule() {
    volume = new ZipVolume();
    scope = new DefaultBackupScope();
    storage = null;
    initiationStrategy = new NoActionInitiationStrategy();
  }

  public String getBackupBucketName() {
    return backupBucketName;
  }

  public void setBackupBucketName(String bucketName) {
    backupBucketName = bucketName;
    storage = new GcloudGcsStorage(backupBucketName);
  }

  public Volume getVolume() {
    return volume;
  }

  public Scope getScope() {
    return scope;
  }

  public void setScope(Scope scope) {
    this.scope = scope;
  }

  public Storage getStorage() {
    return storage;
  }

  public InitiationStrategy getInitiationStrategy() {
    return initiationStrategy;
  }

  public void setStorage(Storage storage) {
    this.storage = storage;
  }
}
