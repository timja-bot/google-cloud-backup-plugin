package com.google.jenkins.plugins.persistentmaster;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for handling Jenkins upgrade versions
 */
public final class VersionUtility {
  private static final String VERSION_FILE = "jenkins_upgrade_version";
  private static final String COMMENT_PREFIX = "#";
  private static final String COMMENT_LINE =
      COMMENT_PREFIX + "This file contains the upgrade version for the jenkins instance";

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

  public static void updateFileSystemVersion(Path storageDir, String version) throws IOException {
    Path path = storageDir.resolve(VERSION_FILE);
    Deque<String> content = new LinkedList<>(Arrays.asList(version));
    content.addFirst(COMMENT_LINE);
    // write to file, overwriting any existing content
    Files.write(path, content, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE, StandardOpenOption.WRITE,
        StandardOpenOption.TRUNCATE_EXISTING);
  }
}

