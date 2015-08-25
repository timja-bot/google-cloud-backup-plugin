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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.annotation.Nullable;
import javax.servlet.ServletException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.kohsuke.stapler.StaplerRequest;

import com.google.jenkins.plugins.persistentmaster.backup.BackupProcedure;
import com.google.jenkins.plugins.persistentmaster.history.KeepAllBackupHistory;
import com.google.jenkins.plugins.persistentmaster.scope.ConfigurableScope;
import com.google.jenkins.plugins.persistentmaster.scope.DefaultBackupScope;
import com.google.jenkins.plugins.persistentmaster.scope.FilteringScope;
import com.google.jenkins.plugins.persistentmaster.scope.IncrementalScope;
import com.google.jenkins.plugins.persistentmaster.scope.MultiScope;
import com.google.jenkins.plugins.persistentmaster.storage.AbstractStorageProvider;
import com.google.jenkins.plugins.persistentmaster.storage.IncrementalBackupStorage;
import com.google.jenkins.plugins.persistentmaster.storage.LocalFileStorageProvider;
import com.google.jenkins.plugins.persistentmaster.storage.Storage;
import com.google.jenkins.plugins.persistentmaster.trigger.BackupTrigger;
import hudson.DescriptorExtensionList;
import hudson.Plugin;
import hudson.StructuredForm;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

/**
 * A plugin for creating scheduled backups of JENKINS_HOME to different
 * locations such as a GCS bucket.
 */
public class PersistentMasterPlugin extends Plugin {

  private static final Logger logger =
      Logger.getLogger(PersistentMasterPlugin.class.getName());

  private static final String SCRATCH_DIRECTORY = "backup-tmp";
  private static final String INCREMENTAL_BACKUP_NAME_SUFFIX = "-incremental";
  private static final int DEFAULT_FULL_BACKUP_INTERVAL_HOURS = 1;
  private static final int DEFAULT_INCREMENTAL_BACKUP_INTERVAL_MINUTES = 3;

  /**
   * Returns the instance of this plugin created by Jenkins.
   */
  public static PersistentMasterPlugin getInstance() {
    return Jenkins.getActiveInstance().getPlugin(PersistentMasterPlugin.class);
  }

  // These fields will be persisted in the configs.
  private boolean enableBackup = false;
  private boolean enableAutoRestore = false;
  private boolean restoreOverwritesData = false;
  private int fullBackupIntervalHours = DEFAULT_FULL_BACKUP_INTERVAL_HOURS;
  private int incrementalBackupIntervalMinutes =
      DEFAULT_INCREMENTAL_BACKUP_INTERVAL_MINUTES;
  private AbstractStorageProvider storageProvider =
      new LocalFileStorageProvider();
  private List<ConfigurableScope> backupScopes = new ArrayList<>(
      Arrays.<ConfigurableScope>asList(new DefaultBackupScope()));

  // These fields will not be persisted in the configs.
  private final transient AtomicBoolean backupOrRestoreInProgress =
      new AtomicBoolean(true); // start in a 'restoring' state.
  private final transient PersistentMasterJenkinsModule module;
  private transient BackupProcedure fullBackupProcedure;
  private transient boolean loaded = false;
  private transient DateTime lastBackupTime;
  private transient DateTime lastFullBackupTime;
  private transient boolean lastBackupFailed = false;
  private transient boolean skipBackupOnNextRestart = false;

  public PersistentMasterPlugin() {
    module = new PersistentMasterJenkinsModule();
    updateBackupProcedure();
  }

  public boolean getRestoreOverwritesData() {
    return restoreOverwritesData;
  }

  /**
   * Get the list of all registered {@link AbstractStorageProvider}.
   */
  public DescriptorExtensionList<AbstractStorageProvider,
      AbstractStorageProvider.StorageProviderDescriptor> getAllProviders() {
    return AbstractStorageProvider.getAllDescriptors();
  }

  /**
   * Get the list of all registered {@link ConfigurableScope}.
   */
  public DescriptorExtensionList<ConfigurableScope,
      ConfigurableScope.ConfigurableScopeDescriptor> getAllScopes() {
    return ConfigurableScope.getAllDescriptors();
  }

  /**
   * Get the list of {@link ConfigurableScope} objects for this instance.
   */
  public Iterable<ConfigurableScope> getBackupScopes() {
    return backupScopes;
  }

  /**
   * Update the local plugin data used by
   * {@link PersistentMasterAsyncPeriodicWork}.
   */
  private void updateBackupProcedure() {
    module.setFullBackupIntervalHours(fullBackupIntervalHours);
    module.setIncrementalBackupIntervalMinutes(
        incrementalBackupIntervalMinutes);
    module.setStorage(storageProvider.getStorage());
    MultiScope combinedScope = new MultiScope();
    for (ConfigurableScope scope : backupScopes) {
      combinedScope.addSubScope(scope, scope.getScopeName() + "/");
    }
    module.setScope(combinedScope);

    if (enableBackup) {
      fullBackupProcedure = new BackupProcedure(
          module.getVolume(), module.getScope(), module.getStorage(),
          module.getBackupHistory(), calculateJenkinsHome(),
          getScratchDirectory(), null);
      lastBackupTime = calculateLastBackupTime(module.getStorage());
    }
  }

  /**
   * Configures plugin based on the corresponding Jenkins settings UI.
   *
   * <p>The configuration under "Persistent Master Backup Plugin" is read,
   * processed and stored.
   */
  @Override
  public void configure(StaplerRequest req, JSONObject formData)
      throws IOException, ServletException, FormException {
    // Extract values from formData.
    boolean shouldEnableBackup = formData.optBoolean("enableBackup", false);
    boolean shouldEnableAutoRestore = formData.optBoolean(
        "enableAutoRestore", false);
    restoreOverwritesData = formData.optBoolean(
        "restoreOverwritesData", false);
    fullBackupIntervalHours = formData.optInt(
        "fullBackupIntervalHours", DEFAULT_FULL_BACKUP_INTERVAL_HOURS);
    incrementalBackupIntervalMinutes = formData.optInt(
        "incrementalBackupIntervalMinutes",
        DEFAULT_INCREMENTAL_BACKUP_INTERVAL_MINUTES);
    String providerClazz = formData.optJSONObject("storageProvider")
        .getString("stapler-class");
    Descriptor<?> descriptor =
        Jenkins.getActiveInstance().getDescriptor(providerClazz);
    AbstractStorageProvider previousStorageProvider = storageProvider;
    storageProvider = (AbstractStorageProvider) descriptor
        .newInstance(req, formData.optJSONObject("storageProvider"));

    List<ConfigurableScope> newScopes = new ArrayList<>();
    for (JSONObject o : StructuredForm.toList(formData, "backupScopeList")) {
      Descriptor<?> scopeDescriptor = Jenkins.getActiveInstance()
          .getDescriptor(o.getString("stapler-class"));
      ConfigurableScope scope = (ConfigurableScope) scopeDescriptor
          .newInstance(req, o);
      newScopes.add(scope);
    }
    this.backupScopes = newScopes;

    // If the user incorrectly configured the storage provider do not enable
    // backups or restores.
    if ((shouldEnableBackup || shouldEnableAutoRestore)
        && !storageProvider.isValid()) {
      logger.severe("Storage provider configuration incorrect, not enabling: " +
          storageProvider.getDescriptor().getDisplayName());
      shouldEnableBackup = false;
      shouldEnableAutoRestore = false;
    }

    // If the storage location has been updated reset the backup timings.
    if (!previousStorageProvider.equals(storageProvider)) {
      lastBackupTime = null;
      lastFullBackupTime = null;
      lastBackupFailed = false;
    }

    // Enable backups/restores if specified.
    enableBackup = shouldEnableBackup;
    enableAutoRestore = shouldEnableAutoRestore;
    updateBackupProcedure();

    // Save the serializable fields of this class.
    save();
  }

  /**
   * Reload fields from persistent storage on startup.
   */
  @Override
  public void start() throws Exception {
    load();
    updateBackupProcedure();
    save();
    loaded = true;
  }

  /**
   * Whether or not the plugin has finished loading.
   */
  public boolean isLoaded() {
    return loaded;
  }

  /**
   * Indicates whether the auto-restore system needs to restart Jenkins to
   * load restored data. In this case, the restore listener should not
   * attempt to create a new backup.
   */
  public boolean isSkipBackupOnNextRestart() {
    return skipBackupOnNextRestart;
  }

  /**
   * Set by the auto-restore system to indicate that the next restart should not
   * trigger a backup; this avoids a backup-restart loop on Jenkins
   * initialization.
   */
  public void setSkipBackupOnNextRestart(boolean skipBackup) {
    skipBackupOnNextRestart = skipBackup;
  }

  /**
   * Gets the {@link AbstractStorageProvider} selected and configured by the
   * user.
   *
   * <p>This method isused by Jenkins via config.jelly.
   */
  public AbstractStorageProvider getStorageProvider() {
    return storageProvider;
  }

  /**
   * Returns whether backup is enabled or disabled. This is a plugin state,
   * different from the plugin being enabled/disabled.
   *
   * <p>The method is used by Jenkins via config.jelly.
   *
   * @return  whether backup is enabled.
   */
  public boolean getEnableBackup() {
    return enableBackup;
  }

  /**
   * Returns whether automatic restores are enabled or disabled.
   * This is a plugin state, different from the plugin being enabled/disabled.
   *
   * <p>The method is used by Jenkins via config.jelly.
   *
   * @return  whether automatic restores are enabled.
   */
  public boolean getEnableAutoRestore() {
    return enableAutoRestore;
  }

  /**
   * Returns the number of hours between full backups should be performed.
   *
   * <p>The method is used by Jenkins via config.jelly.
   *
   * @return  the number of hours between full backups.
   */
  public int getFullBackupIntervalHours() {
    return fullBackupIntervalHours;
  }

  /**
   * Returns the number of minutes between incremental backups should be
   * performed.
   *
   * <p>The method is used by Jenkins via config.jelly.
   *
   * @return  the number of minutes between incremental backups.
   *
   */
  public int getIncrementalBackupIntervalMinutes() {
    return incrementalBackupIntervalMinutes;
  }

  public Path calculateJenkinsHome() {
    return Jenkins.getActiveInstance().getRootDir().toPath();
  }

  public BackupTrigger getFullBackupTrigger() {
    return module.getFullBackupTrigger();
  }

  public BackupTrigger getIncrementalBackupTrigger() {
    return module.getIncrementalBackupTrigger();
  }

  public boolean isBackupOrRestoreInProgress() {
    return backupOrRestoreInProgress.get();
  }

  /**
   * Atomically sets the flag that indicates a backup or restore is in
   * progress. If the flag was previously false, this method returns true;
   * if the flag was previously true, this method returns false. Callers can
   * use this as a signal that a backup or restore has already been initiated.
   */
  public synchronized boolean beginBackupOrRestore() {
    return backupOrRestoreInProgress.compareAndSet(false, true);
  }

  /**
   * Atomically clears the flag that indicates a backup or restore is in
   * progress.
   */
  public synchronized void endBackupOrRestore() {
    backupOrRestoreInProgress.set(false);
  }

  @Nullable
  public DateTime getLastBackupTime() {
    return lastBackupTime;
  }

  public void setLastBackupTime(DateTime lastBackupTime) {
    this.lastBackupTime = lastBackupTime;
  }

  @Nullable
  public DateTime getLastFullBackupTime() {
    return lastFullBackupTime;
  }

  public void setLastFullBackupTime(DateTime lastFullBackupTime) {
    this.lastFullBackupTime = lastFullBackupTime;
  }

  public BackupProcedure getFullBackupProcedure() {
    return fullBackupProcedure;
  }

  public PersistentMasterMainModule getPersistentMasterMainModule() {
    return module;
  }

  public Path getScratchDirectory() {
    return calculateJenkinsHome().resolve(SCRATCH_DIRECTORY);
  }

  public BackupProcedure getIncrementalBackupProcedure() {
    FilteringScope filteringScope = new FilteringScope(
        new IncrementalScope(
            module.getScope(),
            FileTime.fromMillis(lastBackupTime.getMillis())));
    // exclude the log file of the periodic worker, because this file will
    // always be dirty when checking for incremental updates.
    filteringScope.addExclusion(
            PersistentMasterAsyncPeriodicWork.getLogFileName());

    return new BackupProcedure(
        module.getVolume(),
        filteringScope,
        new IncrementalBackupStorage(module.getStorage()),
        new KeepAllBackupHistory(),  // keep backups until next full backup
        calculateJenkinsHome(),
        getScratchDirectory(),
        INCREMENTAL_BACKUP_NAME_SUFFIX);
  }

  public void setLastBackupFailed(boolean lastBackupFailed) {
    this.lastBackupFailed = lastBackupFailed;
  }

  public boolean isLastBackupFailed() {
    return lastBackupFailed;
  }

  private static DateTime calculateLastBackupTime(Storage storage) {
    List<String> lastBackupFiles;
    try {
      lastBackupFiles = storage.findLatestBackup();
    } catch (IOException e) {
      return null;  // assume no backup exists
    }
    if (lastBackupFiles != null && !lastBackupFiles.isEmpty()) {
      // this means that a backup does exist, and since we just started jenkins,
      // we just applied the latest backup, therefore the last backup time is
      // (about) now.
      return new DateTime(DateTimeZone.UTC);
    }
    return null;
  }

  @Override
  public String toString() {
    return "PersistentMasterPlugin{" +
            "enableBackup=" + enableBackup +
            ", storageProvider=" + storageProvider +
            '}';
  }
}
