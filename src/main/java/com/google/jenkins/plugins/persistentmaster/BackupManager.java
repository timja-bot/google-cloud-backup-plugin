/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.model.ManagementLink;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;

/**
 * Provides UI for backup management. Currently allows a user to trigger a full
 * backup, with more config/management options to be added in future versions.
 */
@Extension
public class BackupManager extends ManagementLink {
  private static final Logger LOGGER = Logger.getLogger(
      BackupManager.class.getName());

  /** {@inheritDoc} */
  @Override
  public String getDescription() {
    return Messages.BackupManager_Description();
  }

  /** {@inheritDoc} */
  @Override
  public String getIconFileName() {
    return "folder.png";
  }

  /** {@inheritDoc} */
  @Override
  public String getUrlName() {
    return "backupConsole";
  }

  /** {@inheritDoc} */
  public String getConfigFileName() {
    return "backupConfig.xml";
  }

  /** {@inheritDoc} */
  @Override
  public String getDisplayName() {
    return Messages.BackupManager_DisplayName();
  }
  
  public Set<String> getConflictedFiles() throws IOException {
    return getFilesInConflict();
  }

  public Set<String> getFilesInConflict() throws IOException {
  //  List<String> conflictingFiles = new ArrayList<String>();
//    PersistentMasterPlugin plugin = PersistentMasterPlugin.getInstance();
//    Path jenkinsHome = plugin.calculateJenkinsHome();
//    System.out.println("Jenkins home is" + jenkinsHome.toString()); 
//    Path jenkinsStartState = jenkinsHome.resolve("backup-tmp/jenkins_start_state");
//    Storage storage = plugin.getStorageProvider().getStorage();
//    System.out.println("Storage is" + storage.getClass()); 
//    List<String> listMetadataForExistingFiles = storage.listMetadataForExistingFiles();
//    for (String file : listMetadataForExistingFiles) {
//      Path filePath = jenkinsStartState.resolve(file);
//      if(Files.exists(filePath)){
//        conflictingFiles.add(file);
//      } else {
//        System.out.println(filePath); 
//      }
//      
//    }
    PersistentMasterPlugin plugin = PersistentMasterPlugin.getInstance();
    Path jenkinsHome = plugin.calculateJenkinsHome();
    
    Path backupLog = jenkinsHome.resolve("backup-tmp");
    
    Path startState = backupLog.resolve("start-state");
    Path backupState = backupLog.resolve("backup-state");
    
    if (Files.notExists(backupState) || Files.notExists(startState)){
      return new HashSet<String>();
    }
    Set<String> startFilesSet = readFile(startState);
    Set<String> backupFilesSet = readFile(backupState);
    
    startFilesSet.retainAll(backupFilesSet);
    return startFilesSet;
      //return conflictingFiles;
  }
  
  /**
   * @param startState
   * @return
   */
  private Set<String> readFile(Path file) {
    Set<String> files = new HashSet<String>();
    Charset charset = Charset.forName("US-ASCII");
    try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        files.add(line);
      }
    } catch (IOException x) {
      System.err.format("IOException: %s%n", x);
    }
    return files;
  }

  public void doBackupNow(StaplerRequest req, StaplerResponse res)
      throws IOException, ServletException {
    LOGGER.info("Full backup manually triggered.");
    PersistentMasterPlugin plugin = PersistentMasterPlugin.getInstance();
    if (plugin == null || !plugin.isLoaded()) {
      LOGGER.warning("Persistent Master Plugin instance missing when "
          + "attempting manually triggered backup.");
      return;
    }
    if (!plugin.getEnableBackup()) {
      LOGGER.warning("Backup was manually triggered but backups are disabled.");
      return;
    }
    plugin.setManualBackupRequested(true);
    res.forwardToPreviousPage(req);
  }
  
}
