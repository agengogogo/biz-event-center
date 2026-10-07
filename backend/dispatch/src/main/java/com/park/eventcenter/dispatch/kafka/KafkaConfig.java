package com.park.eventcenter.dispatch.kafka;

import com.park.eventcenter.model.CanonicalEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/** Event bus topics and the shared consumer error handler with dead-letter publishing. */
@Configuration
@Profile("!local")
public class KafkaConfig {

  @Bean
  public NewTopic eventsTopic() {
    return TopicBuilder.name(EventTopics.EVENTS).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic eventsDeadLetterTopic() {
    return TopicBuilder.name(EventTopics.EVENTS_DLT).partitions(1).replicas(1).build();
  }

  @Bean
  public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, CanonicalEvent> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer =
        new DeadLetterPublishingRecoverer(
            kafkaTemplate,
            (record, ex) -> new org.apache.kafka.common.TopicPartition(EventTopics.EVENTS_DLT, 0));
    ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
    backOff.setMaxInterval(5_000L);
    backOff.setMaxElapsedTime(30_000L);
    return new DefaultErrorHandler(recoverer, backOff);
  }
}
