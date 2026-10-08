package br.com.fiap.order.service;

import br.com.fiap.order.config.RabbitConfig;
import br.com.fiap.order.dto.ReviewMessage;
import br.com.fiap.order.dto.ReviewRequest;
import br.com.fiap.order.exception.ApiExceptions.DishNotFoundException;
import br.com.fiap.order.model.Dish;
import br.com.fiap.order.repository.DishRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Publishes the review to RabbitMQ; nothing is written to the database here. */
@Service
public class ReviewPublisher {

    private final DishRepository dishRepository;
    private final RabbitTemplate rabbitTemplate;

    public ReviewPublisher(DishRepository dishRepository, RabbitTemplate rabbitTemplate) {
        this.dishRepository = dishRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(ReviewRequest request) {
        Dish dish = dishRepository.findById(request.dishId())
                .orElseThrow(() -> new DishNotFoundException(request.dishId()));

        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE_NAME,
                RabbitConfig.REVIEWS_ROUTING_KEY,
                new ReviewMessage(dish.getId(), dish.getName(), request.rating(), request.comment()));
    }
}
