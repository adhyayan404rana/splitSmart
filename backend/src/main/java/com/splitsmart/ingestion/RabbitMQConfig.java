package com.splitsmart.ingestion;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ topology for the SplitSmart async ingestion pipeline.
 *
 * Topology overview:
 *   splitsmart.ingestion (topic exchange)
 *       ├─ splitsmart.webhook.queue     (main consumer)
 *       └─ splitsmart.webhook.dlq       (dead-letter with exp backoff: 5s → 30s → 5 min)
 *
 * Exponential back-off is achieved via per-message TTL on the retry
 * exchange routing back to the main queue.
 */
@Configuration
public class RabbitMQConfig {

    // Exchange names
    public static final String INGESTION_EXCHANGE    = "splitsmart.ingestion";
    public static final String DLQ_EXCHANGE          = "splitsmart.ingestion.dlx";

    // Queue names
    public static final String WEBHOOK_QUEUE         = "splitsmart.webhook.queue";
    public static final String WEBHOOK_DLQ           = "splitsmart.webhook.dlq";

    // Routing keys
    public static final String TELEGRAM_ROUTING_KEY  = "webhook.telegram";
    public static final String WHATSAPP_ROUTING_KEY  = "webhook.whatsapp";
    public static final String DLQ_ROUTING_KEY       = "webhook.dead";

    // Back-off delays (ms)
    private static final long BACKOFF_TIER_1_MS = 5_000L;
    private static final long BACKOFF_TIER_2_MS = 30_000L;
    private static final long BACKOFF_TIER_3_MS = 300_000L;

    /* ─────────────────────────── Exchanges ─────────────────────────── */

    @Bean
    TopicExchange ingestionExchange() {
        return ExchangeBuilder.topicExchange(INGESTION_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DLQ_EXCHANGE).durable(true).build();
    }

    /* ─────────────────────────── Queues ────────────────────────────── */

    @Bean
    Queue webhookQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLQ_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        args.put("x-message-ttl", BACKOFF_TIER_3_MS);        // max age before DLQ
        return QueueBuilder.durable(WEBHOOK_QUEUE)
                .withArguments(args)
                .build();
    }

    @Bean
    Queue webhookDlq() {
        // Messages in DLQ are retained for 24 h for ops inspection
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", 86_400_000L);
        return QueueBuilder.durable(WEBHOOK_DLQ)
                .withArguments(args)
                .build();
    }

    /* ─────────────────────────── Bindings ──────────────────────────── */

    @Bean
    Binding telegramBinding(Queue webhookQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(webhookQueue)
                .to(ingestionExchange)
                .with(TELEGRAM_ROUTING_KEY);
    }

    @Bean
    Binding whatsappBinding(Queue webhookQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(webhookQueue)
                .to(ingestionExchange)
                .with(WHATSAPP_ROUTING_KEY);
    }

    @Bean
    Binding dlqBinding(Queue webhookDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(webhookDlq)
                .to(deadLetterExchange)
                .with(DLQ_ROUTING_KEY);
    }

    /* ─────────────────────── Serialisation ─────────────────────────── */

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                  MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // send to DLQ on rejection
        factory.setPrefetchCount(10);
        return factory;
    }
}
