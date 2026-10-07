package com.park.eventcenter.dispatch.service;

/** Outcome of delivering one event to one subscription. */
enum DeliveryOutcome {
  DELIVERED,
  FAILED,
  DEAD_LETTER,
  SKIPPED
}
