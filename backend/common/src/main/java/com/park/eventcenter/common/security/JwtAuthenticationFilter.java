package com.park.eventcenter.common.security;

import com.park.eventcenter.common.error.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates bearer JWTs and binds the caller's park to {@link ParkContext}. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    String token = null;
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      token = header.substring(BEARER_PREFIX.length());
    } else {
      token = request.getParameter("access_token");
    }
    if (token != null && !token.isBlank()) {
      try {
        AuthenticatedUser user = jwtService.parse(token);
        var authorities =
            user.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        ParkContext.set(user.parkId());
      } catch (UnauthorizedException ex) {
        SecurityContextHolder.clearContext();
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      ParkContext.clear();
    }
  }
}
