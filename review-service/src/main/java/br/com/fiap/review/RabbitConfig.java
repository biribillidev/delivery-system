package br.com.fiap.review;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "delivery.exchange";
    public static final String REVIEWS_QUEUE = "reviews.queue";
    public static final String REVIEWS_ROUTING_KEY = "reviews.new";

    @Bean
    Queue reviewsQueue() {
        return QueueBuilder.durable(REVIEWS_QUEUE).build();
    }

    @Bean
    TopicExchange deliveryExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Binding reviewsBinding() {
        return BindingBuilder
                .bind(reviewsQueue())
                .to(deliveryExchange())
                .with(REVIEWS_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
