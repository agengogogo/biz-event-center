package com.park.eventcenter.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.UnauthorizedException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies JWT issuance/parsing carries the park and rejects tampered tokens. */
class JwtServiceTest {

  private JwtService service() {
    SecurityProperties properties = new SecurityProperties();
    properties.setJwtSecret("unit-test-secret-unit-test-secret-1234");
    properties.setTokenTtl(Duration.ofHours(1));
    return new JwtService(new ObjectMapper(), properties);
  }

  @Test
  void issuesAndParsesTokenWithParkAndRoles() {
    JwtService jwtService = service();
    String token = jwtService.issue(new AuthenticatedUser("admin", "PARK-A", List.of("ADMIN")));

    AuthenticatedUser parsed = jwtService.parse(token);

    assertThat(parsed.username()).isEqualTo("admin");
    assertThat(parsed.parkId()).isEqualTo("PARK-A");
    assertThat(parsed.hasRole("ADMIN")).isTrue();
  }

  @Test
  void rejectsTamperedToken() {
    JwtService jwtService = service();
    String token = jwtService.issue(new AuthenticatedUser("admin", "PARK-A", List.of("ADMIN")));

    assertThatThrownBy(() -> jwtService.parse(token + "deadbeef"))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void rejectsMalformedToken() {
    assertThatThrownBy(() -> service().parse("not-a-jwt"))
        .isInstanceOf(UnauthorizedException.class);
  }
}
