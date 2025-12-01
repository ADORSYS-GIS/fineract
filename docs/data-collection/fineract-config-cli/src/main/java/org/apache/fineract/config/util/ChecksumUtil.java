package org.apache.fineract.config.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility for calculating checksums.
 *
 * <p>Uses SHA-256 algorithm to generate checksums for change detection.
 */
@Slf4j
@Component
public class ChecksumUtil {

  private static final String ALGORITHM = "SHA-256";

  /**
   * Calculates SHA-256 checksum of content.
   *
   * @param content content to checksum
   * @return hex-encoded checksum (64 characters)
   */
  public String calculate(String content) {
    if (content == null || content.isEmpty()) {
      log.warn("Calculating checksum for null or empty content");
      return "";
    }

    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
      byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }

  /**
   * Converts byte array to hex string.
   *
   * @param bytes byte array
   * @return hex string
   */
  private String bytesToHex(byte[] bytes) {
    StringBuilder result = new StringBuilder();
    for (byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Calculates combined checksum for multiple file contents.
   *
   * <p>Files are processed in order and combined into a single checksum. This ensures that: - Order
   * of files matters (file1+file2 != file2+file1) - All files contribute to the checksum - Changes
   * to any file or file order are detected
   *
   * @param contents list of file contents to checksum
   * @return combined hex-encoded checksum (64 characters)
   */
  public String calculateCombined(List<String> contents) {
    if (contents == null || contents.isEmpty()) {
      log.warn("Calculating combined checksum for null or empty list");
      return "";
    }

    try {
      MessageDigest digest = MessageDigest.getInstance(ALGORITHM);

      // Process each file content in order
      for (String content : contents) {
        if (content != null && !content.isEmpty()) {
          byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
          digest.update(contentBytes);
          // Add separator between files to ensure distinctness
          digest.update("\n---\n".getBytes(StandardCharsets.UTF_8));
        }
      }

      byte[] hash = digest.digest();
      return bytesToHex(hash);

    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 algorithm not available", ex);
    }
  }

  /**
   * Verifies if a checksum matches content.
   *
   * @param content content to verify
   * @param expectedChecksum expected checksum
   * @return true if matches, false otherwise
   */
  public boolean verify(String content, String expectedChecksum) {
    String actualChecksum = calculate(content);
    boolean matches = actualChecksum.equals(expectedChecksum);

    if (!matches) {
      log.debug("Checksum mismatch: expected={}, actual={}", expectedChecksum, actualChecksum);
    }

    return matches;
  }
}
