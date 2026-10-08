package br.com.fiap.order.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bonus: token bucket limiting POST /orders to N requests per second.
 * The bucket holds N tokens and refills continuously at N tokens/second;
 * a request without a token gets 429.
 */
@Component
@ConditionalOnProperty(name = "rate-limit.orders.enabled", havingValue = "true")
public class OrderRateLimitFilter extends OncePerRequestFilter {

    private final double capacity;
    private double tokens;
    private long lastRefillNanos = System.nanoTime();

    public OrderRateLimitFilter(@Value("${rate-limit.orders.requests-per-second:20}") int requestsPerSecond) {
        this.capacity = requestsPerSecond;
        this.tokens = requestsPerSecond;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && "/orders".equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!tryAcquire()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests, try again in a moment\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private synchronized boolean tryAcquire() {
        long now = System.nanoTime();
        tokens = Math.min(capacity, tokens + (now - lastRefillNanos) / 1_000_000_000.0 * capacity);
        lastRefillNanos = now;
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }
}
