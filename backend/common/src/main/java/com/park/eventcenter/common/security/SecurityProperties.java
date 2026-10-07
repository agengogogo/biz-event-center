package com.park.eventcenter.common.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Configuration for JWT issuance and the built-in accounts used by local development. */
@Component
@ConfigurationProperties(prefix = "event-center.security")
public class SecurityProperties {

  private String jwtSecret = "change-me-in-production-change-me-in-production";
  private Duration tokenTtl = Duration.ofHours(8);
  private List<User> users = new ArrayList<>();

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public Duration getTokenTtl() {
    return tokenTtl;
  }

  public void setTokenTtl(Duration tokenTtl) {
    this.tokenTtl = tokenTtl;
  }

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  /** A configured account bound to a park and a set of roles. */
  public static class User {
    private String username;
    private String password;
    private String parkId;
    private List<String> roles = new ArrayList<>();

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getParkId() {
      return parkId;
    }

    public void setParkId(String parkId) {
      this.parkId = parkId;
    }

    public List<String> getRoles() {
      return roles;
    }

    public void setRoles(List<String> roles) {
      this.roles = roles;
    }
  }
}
