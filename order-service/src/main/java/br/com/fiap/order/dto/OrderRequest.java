package br.com.fiap.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotNull Long dishId, @NotNull @Min(1) Integer quantity) {
}
