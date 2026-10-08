package br.com.fiap.payment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequest(@NotNull @Positive BigDecimal amount) {
}
