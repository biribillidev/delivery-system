package br.com.fiap.order.service;

import br.com.fiap.order.exception.ApiExceptions.DishNotFoundException;
import br.com.fiap.order.exception.ApiExceptions.OrderNotFoundException;
import br.com.fiap.order.exception.ApiExceptions.OutOfStockException;
import br.com.fiap.order.exception.ApiExceptions.PaymentUnavailableException;
import br.com.fiap.order.model.CustomerOrder;
import br.com.fiap.order.model.Dish;
import br.com.fiap.order.model.OrderStatus;
import br.com.fiap.order.repository.CustomerOrderRepository;
import br.com.fiap.order.repository.DishRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final DishRepository dishRepository;
    private final CustomerOrderRepository orderRepository;
    private final PaymentClient paymentClient;

    public OrderService(DishRepository dishRepository, CustomerOrderRepository orderRepository,
                        PaymentClient paymentClient) {
        this.dishRepository = dishRepository;
        this.orderRepository = orderRepository;
        this.paymentClient = paymentClient;
    }

    /**
     * The dish row stays locked (PESSIMISTIC_WRITE) until the transaction ends, so concurrent
     * orders are serialized and stock can never go below zero. If payment fails, the
     * exception rolls the transaction back and the stock is left untouched.
     */
    @Transactional
    public CustomerOrder createOrder(Long dishId, int quantity) {
        Dish dish = dishRepository.findByIdForUpdate(dishId)
                .orElseThrow(() -> new DishNotFoundException(dishId));

        if (dish.getStock() < quantity) {
            throw new OutOfStockException();
        }

        BigDecimal total = dish.getPrice().multiply(BigDecimal.valueOf(quantity));

        try {
            paymentClient.pay(total);
        } catch (Exception e) {
            log.error("Payment failed after all retries for dish {}: {}", dishId, e.toString());
            throw new PaymentUnavailableException(e);
        }

        dish.decreaseStock(quantity);
        return orderRepository.save(new CustomerOrder(dishId, quantity, total, OrderStatus.CONFIRMED));
    }

    @Transactional(readOnly = true)
    public CustomerOrder findOrder(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
