package br.com.fiap.order.controller;

import br.com.fiap.order.dto.ReviewRequest;
import br.com.fiap.order.service.ReviewPublisher;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewPublisher publisher;

    public ReviewController(ReviewPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void create(@Valid @RequestBody ReviewRequest request) {
        publisher.publish(request);
    }
}
