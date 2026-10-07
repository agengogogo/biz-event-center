package com.park.eventcenter.local;

import java.io.File;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Creates the parent directory of the local SQLite database before the DataSource is initialized.
 *
 * <p>The SQLite JDBC driver refuses to open a database whose parent directory does not exist, so
 * when the {@code local} profile points {@code spring.datasource.url} at a fresh path (for example
 * {@code ./data/eventcenter.db}) this post-processor makes the directory first. It is a no-op for
 * any other profile or URL.
 */
public class SqliteDataDirectoryEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  private static final String SQLITE_PREFIX = "jdbc:sqlite:";

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    if (!Arrays.asList(environment.getActiveProfiles()).contains("local")) {
      return;
    }
    String url = environment.getProperty("spring.datasource.url");
    if (url == null || !url.startsWith(SQLITE_PREFIX)) {
      return;
    }
    String path = url.substring(SQLITE_PREFIX.length());
    int queryIndex = path.indexOf('?');
    if (queryIndex >= 0) {
      path = path.substring(0, queryIndex);
    }
    if (path.isBlank() || path.startsWith(":")) {
      return;
    }
    File parent = new File(path).getAbsoluteFile().getParentFile();
    if (parent != null && !parent.exists()) {
      parent.mkdirs();
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 20;
  }
}
