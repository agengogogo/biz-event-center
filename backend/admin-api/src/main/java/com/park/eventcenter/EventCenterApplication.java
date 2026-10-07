package com.park.eventcenter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Runnable backend that exposes the park business event center management API. */
@SpringBootApplication(scanBasePackages = "com.park.eventcenter")
@EnableJpaAuditing
@EnableScheduling
public class EventCenterApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventCenterApplication.class, args);
  }
}
