package com.google.jenkins.plugins.persistentmaster;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for handling Jenkins upgrade versions
 */
public final class VersionUtility {
  private static final String VERSION_FILE = "jenkins_upgrade_version";
  private static final String COMMENT_PREFIX = "#";

  private static final Logger logger =
      Logger.getLogger(VersionUtility.class.getName());

  
  public static String getFileSystemVersion(Path jenkinsHome) {
    Path versionPath = jenkinsHome.resolve(VERSION_FILE);
    String version = null;
    try {
      if (versionPath != null && Files.exists(versionPath)) {
        List<String> lines = Files.readAllLines(versionPath, StandardCharsets.UTF_8);
        for (String line : lines) {
          if (line != null && !line.trim().isEmpty() && !line.startsWith(COMMENT_PREFIX)) {
            version = line;
          }
        }
      }
      return version;
    } catch (IOException e) {
      logger.fine("Exception trying to read filesystem version " + e);
      return null;
    }
  }

}

