package com.splitsmart.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookProducer {

    private final RabbitTemplate rabbitTemplate;

    public void enqueueWebhookPayload(Object payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INGESTION_EXCHANGE,
                    RabbitMQConfig.INGESTION_ROUTING_KEY,
                    payload
            );
            log.info("Successfully enqueued webhook payload to RabbitMQ ingestion queue");
        } catch (Exception ex) {
            log.error("Failed to enqueue payload to RabbitMQ", ex);
        }
    }
}
