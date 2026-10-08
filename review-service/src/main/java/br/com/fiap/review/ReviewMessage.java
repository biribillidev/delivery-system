package br.com.fiap.review;

public record ReviewMessage(Long dishId, String dishName, Integer rating, String comment) {
}
