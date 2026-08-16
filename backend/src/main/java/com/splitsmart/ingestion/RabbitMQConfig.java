package com.splitsmart.ingestion;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitMQConfig {

    public static final String INGESTION_EXCHANGE = "ingestion.exchange";
    public static final String INGESTION_QUEUE = "ingestion.queue";
    public static final String INGESTION_ROUTING_KEY = "ingestion.raw";

    public static final String DLX_EXCHANGE = "ingestion.dlx";
    public static final String DLQ_QUEUE = "ingestion.dlq";
    public static final String DLQ_ROUTING_KEY = "ingestion.dlq.routingKey";

    @Bean
    public TopicExchange ingestionExchange() {
        return new TopicExchange(INGESTION_EXCHANGE, true, false);
    }

    @Bean
    public Queue ingestionQueue() {
        return QueueBuilder.durable(INGESTION_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", DLX_EXCHANGE,
                        "x-dead-letter-routing-key", DLQ_ROUTING_KEY
                ))
                .build();
    }

    @Bean
    public Binding ingestionBinding(Queue ingestionQueue, TopicExchange ingestionExchange) {
        return BindingBuilder.bind(ingestionQueue).to(ingestionExchange).with(INGESTION_ROUTING_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding dlqBinding(Queue dlqQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlqQueue).to(dlxExchange).with(DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
