package com.park.eventcenter.local;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Verifies the local profile creates the SQLite parent directory and leaves other profiles alone.
 */
class SqliteDataDirectoryEnvironmentPostProcessorTest {

  private final SqliteDataDirectoryEnvironmentPostProcessor processor =
      new SqliteDataDirectoryEnvironmentPostProcessor();

  @Test
  void createsParentDirectoryForLocalProfile() throws Exception {
    Path database =
        Files.createTempDirectory("bec-postproc-").resolve("nested/data/eventcenter.db");
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("local");
    environment.setProperty(
        "spring.datasource.url", "jdbc:sqlite:" + database + "?journal_mode=WAL");

    processor.postProcessEnvironment(environment, null);

    assertThat(database.getParent()).exists();
  }

  @Test
  void ignoresNonLocalProfile() throws Exception {
    Path database =
        Files.createTempDirectory("bec-postproc-").resolve("nested/data/eventcenter.db");
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("spring.datasource.url", "jdbc:sqlite:" + database);

    processor.postProcessEnvironment(environment, null);

    assertThat(database.getParent()).doesNotExist();
  }
}
