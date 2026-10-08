package br.com.fiap.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull Long dishId, @NotNull @Min(1) @Max(5) Integer rating, String comment) {
}
