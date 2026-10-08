package br.com.fiap.order.dto;

public record ReviewMessage(Long dishId, String dishName, Integer rating, String comment) {
}
