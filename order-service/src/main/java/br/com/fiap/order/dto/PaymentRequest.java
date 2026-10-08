package br.com.fiap.order.dto;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {
}
