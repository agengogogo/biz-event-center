package com.park.eventcenter.ingestion.service;

import com.park.eventcenter.common.error.ForbiddenException;
import com.park.eventcenter.common.error.NotFoundException;
import com.park.eventcenter.common.error.UnauthorizedException;
import com.park.eventcenter.common.security.ParkContext;
import com.park.eventcenter.ingestion.dto.SourceRequest;
import com.park.eventcenter.ingestion.dto.SourceView;
import com.park.eventcenter.ingestion.entity.SourceEntity;
import com.park.eventcenter.ingestion.repository.SourceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Source registration and credential authentication. */
@Service
public class SourceService {

  private final SourceRepository repository;

  public SourceService(SourceRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public SourceView create(SourceRequest request) {
    SourceEntity entity = new SourceEntity();
    entity.setId(UUID.randomUUID().toString());
    entity.setParkId(ParkContext.require());
    entity.setCreatedAt(Instant.now());
    String secret = UUID.randomUUID().toString().replace("-", "");
    entity.setSecretHash(hash(secret));
    apply(entity, request);
    SourceView view = toView(repository.save(entity), secret);
    return view;
  }

  @Transactional
  public SourceView update(String id, SourceRequest request) {
    SourceEntity entity = require(id);
    apply(entity, request);
    return toView(repository.save(entity), null);
  }

  @Transactional(readOnly = true)
  public List<SourceView> list() {
    return repository.findByParkIdOrderByCreatedAtDesc(ParkContext.require()).stream()
        .map(entity -> toView(entity, null))
        .toList();
  }

  @Transactional(readOnly = true)
  public SourceView get(String id) {
    return toView(require(id), null);
  }

  @Transactional
  public SourceView setEnabled(String id, boolean enabled) {
    SourceEntity entity = require(id);
    entity.setEnabled(enabled);
    entity.setUpdatedAt(Instant.now());
    return toView(repository.save(entity), null);
  }

  @Transactional
  public void delete(String id) {
    repository.delete(require(id));
  }

  /** Authenticates an inbound source by id/secret, regardless of park scope. */
  @Transactional(readOnly = true)
  public SourceEntity authenticate(String sourceId, String secret) {
    if (sourceId == null || secret == null) {
      throw new UnauthorizedException("Missing source credentials");
    }
    SourceEntity entity =
        repository
            .findById(sourceId)
            .orElseThrow(() -> new UnauthorizedException("Unknown source"));
    if (!entity.isEnabled()) {
      throw new UnauthorizedException("Source is disabled");
    }
    if (!entity.getSecretHash().equals(hash(secret))) {
      throw new UnauthorizedException("Invalid source credentials");
    }
    return entity;
  }

  private void apply(SourceEntity entity, SourceRequest request) {
    if (request.name() == null || request.name().isBlank()) {
      throw new com.park.eventcenter.common.error.BadRequestException("name is required");
    }
    if (request.sourceType() == null) {
      throw new com.park.eventcenter.common.error.BadRequestException("sourceType is required");
    }
    entity.setName(request.name());
    entity.setSourceType(request.sourceType());
    entity.setEnabled(request.enabled() == null || request.enabled());
    entity.setUpdatedAt(Instant.now());
  }

  private SourceEntity require(String id) {
    SourceEntity entity =
        repository
            .findByIdAndParkId(id, ParkContext.require())
            .orElseThrow(() -> new NotFoundException("Source not found: " + id));
    if (!entity.getParkId().equals(ParkContext.require())) {
      throw new ForbiddenException("Cross-park access denied");
    }
    return entity;
  }

  private String hash(String secret) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private SourceView toView(SourceEntity entity, String secret) {
    return new SourceView(
        entity.getId(),
        entity.getName(),
        entity.getSourceType(),
        entity.getParkId(),
        entity.isEnabled(),
        secret,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
