package com.park.eventcenter.common.security;

import com.park.eventcenter.common.error.UnauthorizedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Issues JWTs for the configured accounts. This is the only anonymous management endpoint. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final SecurityProperties properties;
  private final JwtService jwtService;

  public AuthController(SecurityProperties properties, JwtService jwtService) {
    this.properties = properties;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public LoginResponse login(@RequestBody @Valid LoginRequest request) {
    SecurityProperties.User user =
        properties.getUsers().stream()
            .filter(
                candidate ->
                    candidate.getUsername().equals(request.username())
                        && candidate.getPassword().equals(request.password()))
            .findFirst()
            .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));
    AuthenticatedUser principal =
        new AuthenticatedUser(user.getUsername(), user.getParkId(), user.getRoles());
    return new LoginResponse(jwtService.issue(principal), principal.parkId(), principal.roles());
  }

  /** Login request body. */
  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  /** Login response carrying the bearer token. */
  public record LoginResponse(String token, String parkId, List<String> roles) {}
}
