package com.park.eventcenter.common.idempotency;

import java.time.Duration;

/** Pluggable idempotency/dedup store used by ingestion and dispatch. */
public interface IdempotencyStore {

  /** Atomically marks the key and returns true if it was newly created, false if it existed. */
  boolean markIfAbsent(String key, Duration ttl);

  boolean exists(String key);

  void mark(String key, Duration ttl);
}
