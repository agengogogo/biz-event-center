package com.park.eventcenter.common.api;

import java.util.List;

/** Simple pagination envelope shared by list endpoints. */
public record PageResponse<T>(List<T> items, long total, int page, int size) {

  public static <T> PageResponse<T> of(List<T> items, long total, int page, int size) {
    return new PageResponse<>(items, total, page, size);
  }
}
