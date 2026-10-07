package com.park.eventcenter.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.park.eventcenter.common.error.UnauthorizedException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Minimal HMAC-SHA256 JWT encoder/decoder built on the JDK so no extra runtime dependency is
 * required. Tokens carry the caller's park and roles for multi-park isolation.
 */
@Component
public class JwtService {

  private static final byte[] HEADER =
      "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8);

  private final ObjectMapper mapper;
  private final byte[] key;
  private final Duration ttl;

  public JwtService(ObjectMapper mapper, SecurityProperties properties) {
    this.mapper = mapper;
    this.key = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    this.ttl = properties.getTokenTtl();
  }

  public String issue(AuthenticatedUser user) {
    long issuedAt = Instant.now().getEpochSecond();
    Map<String, Object> claims = new LinkedHashMap<>();
    claims.put("sub", user.username());
    claims.put("park", user.parkId());
    claims.put("roles", user.roles());
    claims.put("iat", issuedAt);
    claims.put("exp", issuedAt + ttl.getSeconds());
    try {
      String header = base64Url(HEADER);
      String payload = base64Url(mapper.writeValueAsBytes(claims));
      String signingInput = header + "." + payload;
      return signingInput + "." + base64Url(hmac(signingInput));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to encode JWT claims", ex);
    }
  }

  @SuppressWarnings("unchecked")
  public AuthenticatedUser parse(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new UnauthorizedException("Malformed token");
    }
    String signingInput = parts[0] + "." + parts[1];
    byte[] expected = hmac(signingInput);
    byte[] actual;
    try {
      actual = Base64.getUrlDecoder().decode(parts[2]);
    } catch (IllegalArgumentException ex) {
      throw new UnauthorizedException("Malformed token");
    }
    if (!MessageDigest.isEqual(expected, actual)) {
      throw new UnauthorizedException("Invalid token signature");
    }
    try {
      Map<String, Object> claims =
          mapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
      long exp = ((Number) claims.getOrDefault("exp", 0L)).longValue();
      if (Instant.now().getEpochSecond() > exp) {
        throw new UnauthorizedException("Token expired");
      }
      String username = (String) claims.get("sub");
      String parkId = (String) claims.get("park");
      List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
      if (username == null || parkId == null) {
        throw new UnauthorizedException("Malformed token claims");
      }
      return new AuthenticatedUser(username, parkId, roles);
    } catch (IOException ex) {
      throw new UnauthorizedException("Malformed token");
    }
  }

  private byte[] hmac(String input) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC algorithm unavailable", ex);
    }
  }

  private String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
