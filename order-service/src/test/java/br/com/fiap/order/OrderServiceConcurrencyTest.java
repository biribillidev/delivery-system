package br.com.fiap.order;

import br.com.fiap.order.dto.PaymentResponse;
import br.com.fiap.order.exception.ApiExceptions.OutOfStockException;
import br.com.fiap.order.exception.ApiExceptions.PaymentUnavailableException;
import br.com.fiap.order.model.Dish;
import br.com.fiap.order.repository.DishRepository;
import br.com.fiap.order.service.OrderService;
import br.com.fiap.order.service.PaymentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class OrderServiceConcurrencyTest {

    @Autowired OrderService orderService;
    @Autowired DishRepository dishRepository;
    @MockitoBean PaymentClient paymentClient;

    @Test
    void fiftyConcurrentOrdersConfirmExactlyTen() throws Exception {
        when(paymentClient.pay(any())).thenReturn(new PaymentResponse("APPROVED", 8081));
        Long promoId = dishRepository.findAll().stream()
                .filter(d -> d.getStock() == 10).findFirst().orElseThrow().getId();

        ExecutorService pool = Executors.newFixedThreadPool(50);
        AtomicInteger confirmed = new AtomicInteger();
        AtomicInteger outOfStock = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            futures.add(pool.submit((Callable<Void>) () -> {
                try {
                    orderService.createOrder(promoId, 1);
                    confirmed.incrementAndGet();
                } catch (OutOfStockException e) {
                    outOfStock.incrementAndGet();
                }
                return null;
            }));
        }
        for (Future<?> f : futures) f.get();
        pool.shutdown();

        assertEquals(10, confirmed.get());
        assertEquals(40, outOfStock.get());
        assertEquals(0, dishRepository.findById(promoId).orElseThrow().getStock());
    }

    @Test
    void paymentFailureLeavesStockIntact() {
        when(paymentClient.pay(any())).thenThrow(new IllegalStateException("payment down"));
        Dish dish = dishRepository.findAll().stream()
                .filter(d -> d.getPrice().compareTo(new BigDecimal("45.00")) == 0).findFirst().orElseThrow();
        int before = dish.getStock();

        assertThrows(PaymentUnavailableException.class, () -> orderService.createOrder(dish.getId(), 2));

        assertEquals(before, dishRepository.findById(dish.getId()).orElseThrow().getStock());
    }
}
