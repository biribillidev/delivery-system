package br.com.fiap.review;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ReviewSummary {

    @Id
    private Long dishId;
    private String dishName;
    private long totalRating;
    private long reviewCount;

    protected ReviewSummary() {
    }

    public ReviewSummary(Long dishId, String dishName) {
        this.dishId = dishId;
        this.dishName = dishName;
    }

    public void add(String dishName, long ratingSum, long count) {
        this.dishName = dishName;
        this.totalRating += ratingSum;
        this.reviewCount += count;
    }

    public double average() {
        return reviewCount == 0 ? 0 : (double) totalRating / reviewCount;
    }

    public Long getDishId() { return dishId; }
    public String getDishName() { return dishName; }
    public long getTotalRating() { return totalRating; }
    public long getReviewCount() { return reviewCount; }
}
