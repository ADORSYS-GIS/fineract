package org.apache.fineract.config.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.fineract.config.exception.ValidationException;
import org.apache.fineract.config.model.FineractConfig;
import org.apache.fineract.config.properties.ImportProperties;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for parsing YAML configuration files.
 *
 * <p>Parses YAML files into FineractConfig objects using Jackson.
 */
@Slf4j
@Service
public class YamlParserService {

  private final ObjectMapper yamlMapper;
  private final ImportProperties importProperties;

  public YamlParserService(ImportProperties importProperties) {
    this.importProperties = importProperties;
    this.yamlMapper = new ObjectMapper(new YAMLFactory());
    this.yamlMapper.findAndRegisterModules(); // Register Java 8 date/time module
  }

  /**
   * Parses configuration from file location(s).
   *
   * <p>Supports:
   *
   * <ul>
   *   <li>Single file: /config/demo.yml
   *   <li>Multiple files (comma-separated): /config/system.yml,/config/products.yml
   *   <li>Glob pattern: /config/*.yml
   * </ul>
   *
   * @return parsed configuration
   * @throws ValidationException if parsing fails
   */
  public FineractConfig parse() {
    String locations = importProperties.getFiles().getLocations();
    log.info("Parsing configuration files from: {}", locations);

    List<File> files = resolveFiles(locations);

    if (files.isEmpty()) {
      throw new ValidationException("No configuration files found at: " + locations);
    }

    log.info("Found {} configuration file(s)", files.size());

    // For now, only support single file
    // TODO: Add support for merging multiple files
    if (files.size() > 1) {
      log.warn("Multiple files found, but merging not yet supported. Using first file only.");
    }

    return parseFile(files.get(0));
  }

  /**
   * Parses a single configuration file.
   *
   * @param file file to parse
   * @return parsed configuration
   * @throws ValidationException if parsing fails
   */
  public FineractConfig parseFile(File file) {
    log.debug("Parsing file: {}", file.getAbsolutePath());

    try {
      FineractConfig config = yamlMapper.readValue(file, FineractConfig.class);
      log.info("Successfully parsed configuration from: {}", file.getName());
      return config;
    } catch (IOException ex) {
      log.error("Failed to parse YAML file: {}", file.getAbsolutePath(), ex);
      throw new ValidationException(
          "Failed to parse YAML file: " + file.getName() + " - " + ex.getMessage(), ex);
    }
  }

  /**
   * Reads file content as string (for checksum calculation).
   *
   * @param file file to read
   * @return file content
   * @throws ValidationException if reading fails
   */
  public String readFileContent(File file) {
    try {
      return Files.readString(file.toPath());
    } catch (IOException ex) {
      throw new ValidationException("Failed to read file: " + file.getName(), ex);
    }
  }

  /**
   * Resolves file locations to actual files.
   *
   * <p>Supports glob patterns and comma-separated paths.
   *
   * @param locations file location(s)
   * @return list of files
   */
  private List<File> resolveFiles(String locations) {
    List<File> files = new ArrayList<>();

    // Split by comma for multiple locations
    String[] locationArray = locations.split(",");

    for (String location : locationArray) {
      location = location.trim();

      if (location.contains("*")) {
        // Glob pattern
        files.addAll(resolveGlobPattern(location));
      } else {
        // Single file
        File file = new File(location);
        if (file.exists() && file.isFile()) {
          files.add(file);
        } else {
          log.warn("File not found: {}", location);
        }
      }
    }

    return files;
  }

  /**
   * Resolves glob pattern to files.
   *
   * @param pattern glob pattern (e.g., /config/*.yml)
   * @return list of matching files
   */
  private List<File> resolveGlobPattern(String pattern) {
    List<File> files = new ArrayList<>();

    try {
      // Extract directory and pattern
      int lastSlash = pattern.lastIndexOf('/');
      String directory = pattern.substring(0, lastSlash);
      String filePattern = pattern.substring(lastSlash + 1);

      Path dirPath = Paths.get(directory);

      if (!Files.exists(dirPath)) {
        log.warn("Directory does not exist: {}", directory);
        return files;
      }

      // Simple glob matching (*.yml -> ends with .yml)
      try (Stream<Path> paths = Files.list(dirPath)) {
        paths
            .filter(Files::isRegularFile)
            .filter(
                p -> {
                  String fileName = p.getFileName().toString();
                  if (filePattern.equals("*.yml") || filePattern.equals("*.yaml")) {
                    return fileName.endsWith(".yml") || fileName.endsWith(".yaml");
                  }
                  return fileName.matches(filePattern.replace("*", ".*"));
                })
            .forEach(p -> files.add(p.toFile()));
      }

      log.debug("Glob pattern '{}' matched {} file(s)", pattern, files.size());

    } catch (IOException ex) {
      log.error("Error resolving glob pattern: {}", pattern, ex);
    }

    return files;
  }
}
