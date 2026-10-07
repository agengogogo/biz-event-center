package com.park.eventcenter.common.security;

import java.util.List;

/** Authenticated caller identity with the park it belongs to and granted roles. */
public record AuthenticatedUser(String username, String parkId, List<String> roles) {

  public AuthenticatedUser {
    roles = roles == null ? List.of() : List.copyOf(roles);
  }

  public boolean hasRole(String role) {
    return roles.contains(role);
  }
}
