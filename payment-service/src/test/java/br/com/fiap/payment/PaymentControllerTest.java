package br.com.fiap.payment;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentControllerTest {

    @Test
    void failsAboutHalfOfTheCallsAndReportsTheInstancePort() {
        PaymentController controller = new PaymentController(8082);
        int approved = 0;
        int failed = 0;

        for (int i = 0; i < 2000; i++) {
            try {
                PaymentResponse response = controller.pay(new PaymentRequest(new BigDecimal("79.80")));
                assertEquals("APPROVED", response.status());
                assertEquals(8082, response.instance());
                approved++;
            } catch (ResponseStatusException e) {
                assertEquals(500, e.getStatusCode().value());
                failed++;
            }
        }

        double failureRate = failed / 2000.0;
        assertTrue(failureRate > 0.4 && failureRate < 0.6, "failure rate was " + failureRate);
        assertEquals(2000, approved + failed);
    }
}
