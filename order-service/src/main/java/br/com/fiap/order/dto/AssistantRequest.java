package br.com.fiap.order.dto;

import jakarta.validation.constraints.NotBlank;

public record AssistantRequest(@NotBlank String question) {
}
