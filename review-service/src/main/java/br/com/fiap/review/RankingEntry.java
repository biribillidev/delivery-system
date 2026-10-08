package br.com.fiap.review;

public record RankingEntry(Long dishId, String dishName, double average, long count) {
}
