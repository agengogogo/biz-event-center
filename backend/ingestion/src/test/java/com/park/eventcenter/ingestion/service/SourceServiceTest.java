package com.park.eventcenter.ingestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.park.eventcenter.common.error.UnauthorizedException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.ingestion.dto.SourceRequest;
import com.park.eventcenter.ingestion.dto.SourceView;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.ingestion.repository.SourceRepository;
import com.park.eventcenter.model.SourceType;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifies source registration issues a secret and that only valid credentials authenticate. */
class SourceServiceTest {

  private SourceRepository repository;
  private SourceService service;

  @BeforeEach
  void setUp() {
    ParkContext.set("PARK-A");
    repository = mock(SourceRepository.class);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    service = new SourceService(repository);
  }

  @AfterEach
  void tearDown() {
    ParkContext.clear();
  }

  @Test
  void registeredSourceAcceptsValidCredentials() {
    SourceView created = service.create(new SourceRequest("gate", SourceType.ACCESS, true));

    SourceEntity stored = new SourceEntity();
    stored.setId(created.id());
    stored.setSourceType(SourceType.ACCESS);
    stored.setParkId("PARK-A");
    stored.setSecretHash(hashOf(created.secret()));
    stored.setEnabled(true);
    when(repository.findById(created.id())).thenReturn(Optional.of(stored));

    assertThat(created.secret()).isNotBlank();
    assertThat(service.authenticate(created.id(), created.secret()).getParkId())
        .isEqualTo("PARK-A");
  }

  @Test
  void unknownSourceIsRejected() {
    when(repository.findById("nope")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.authenticate("nope", "secret"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void invalidCredentialIsRejected() {
    SourceEntity stored = new SourceEntity();
    stored.setId("src-1");
    stored.setSourceType(SourceType.ACCESS);
    stored.setParkId("PARK-A");
    stored.setSecretHash(hashOf("correct-secret"));
    stored.setEnabled(true);
    when(repository.findById("src-1")).thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> service.authenticate("src-1", "wrong-secret"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void disabledSourceIsRejected() {
    SourceEntity stored = new SourceEntity();
    stored.setId("src-1");
    stored.setParkId("PARK-A");
    stored.setSecretHash(hashOf("secret"));
    stored.setEnabled(false);
    when(repository.findById("src-1")).thenReturn(Optional.of(stored));

    assertThatThrownBy(() -> service.authenticate("src-1", "secret"))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessageContaining("disabled");
  }

  private String hashOf(String secret) {
    try {
      java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      return java.util.HexFormat.of()
          .formatHex(digest.digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
