package br.com.fiap.order.service;

import br.com.fiap.order.dto.PaymentRequest;
import br.com.fiap.order.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Kept in its own bean so the @Retryable proxy is applied when OrderService calls it.
 * The URL uses the Eureka service name; the load balancer picks an instance.
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            includes = {RestClientException.class, IllegalStateException.class},
            maxRetries = 4,
            delay = 200,
            multiplier = 2,
            jitter = 100,
            maxDelay = 2000
    )
    public PaymentResponse pay(BigDecimal amount) {
        log.info("Calling payment-service for amount {}", amount);
        PaymentResponse response = restTemplate.postForObject(
                "http://PAYMENT-SERVICE/payments",
                new PaymentRequest(amount),
                PaymentResponse.class);
        log.info("Payment approved by instance {}", response == null ? null : response.instance());
        return response;
    }
}
