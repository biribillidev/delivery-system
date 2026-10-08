package br.com.fiap.order.config;

import br.com.fiap.order.model.Dish;
import br.com.fiap.order.repository.DishRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    private final DishRepository repository;

    public DataLoader(DishRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) return;

        // Dish #1 is the flash promotion: only 10 units
        repository.save(new Dish("House Burger", "Brioche bun, beef patty, cheddar and caramelized onion", new BigDecimal("39.90"), 10));
        repository.save(new Dish("Margherita Pizza", "Tomato sauce, mozzarella and fresh basil (vegetarian)", new BigDecimal("45.00"), 50));
        repository.save(new Dish("Vegan Bowl", "Quinoa, grilled tofu, avocado, chickpeas and tahini (vegan)", new BigDecimal("34.50"), 30));
        repository.save(new Dish("Caesar Salad", "Romaine lettuce, grilled chicken, parmesan and croutons", new BigDecimal("29.90"), 40));
        repository.save(new Dish("Chocolate Brownie", "Warm brownie with vanilla ice cream (vegetarian)", new BigDecimal("18.00"), 25));
        repository.save(new Dish("Fresh Orange Juice", "500 ml, squeezed to order (vegan)", new BigDecimal("12.00"), 100));
    }
}
