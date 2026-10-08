package br.com.fiap.order.controller;

import br.com.fiap.order.dto.OrderRequest;
import br.com.fiap.order.model.CustomerOrder;
import br.com.fiap.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerOrder create(@Valid @RequestBody OrderRequest request) {
        return service.createOrder(request.dishId(), request.quantity());
    }

    @GetMapping("/orders/{id}")
    public CustomerOrder find(@PathVariable Long id) {
        return service.findOrder(id);
    }
}
