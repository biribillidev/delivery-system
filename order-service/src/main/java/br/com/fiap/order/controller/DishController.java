package br.com.fiap.order.controller;

import br.com.fiap.order.exception.ApiExceptions.DishNotFoundException;
import br.com.fiap.order.model.Dish;
import br.com.fiap.order.repository.DishRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DishController {

    private final DishRepository repository;

    public DishController(DishRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/dishes")
    public List<Dish> list() {
        return repository.findAll();
    }

    @GetMapping("/dishes/{id}")
    public Dish find(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new DishNotFoundException(id));
    }
}
