package br.com.fiap.payment;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Random;

@RestController
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final Random random = new Random();
    private final int port;

    public PaymentController(@Value("${server.port}") int port) {
        this.port = port;
    }

    @PostMapping("/payments")
    public PaymentResponse pay(@Valid @RequestBody PaymentRequest request) {
        // Simulates an unstable gateway: about half of the calls fail
        if (random.nextInt(100) < 50) {
            log.warn("[instance {}] Payment of {} FAILED (simulated)", port, request.amount());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment failed");
        }
        log.info("[instance {}] Payment of {} APPROVED", port, request.amount());
        return new PaymentResponse("APPROVED", port);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment request"));
    }

}
