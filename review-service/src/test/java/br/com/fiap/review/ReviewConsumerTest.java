package br.com.fiap.review;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class ReviewConsumerTest {

    @Autowired ReviewConsumer consumer;
    @Autowired ReviewController controller;
    @Autowired ReviewSummaryRepository repository;

    @Test
    void bufferIsFlushedToTheDatabaseAndRankingIsSortedByAverage() {
        // 100 reviews for dish 1 (rating 5) and 100 for dish 2 (rating 3): nothing is written before the flush
        for (int i = 0; i < 100; i++) {
            consumer.onReview(new ReviewMessage(1L, "House Burger", 5, "great"));
            consumer.onReview(new ReviewMessage(2L, "Vegan Bowl", 3, "ok"));
        }
        assertEquals(0, repository.count());

        consumer.flush();

        assertEquals(2, repository.count());
        List<RankingEntry> ranking = controller.ranking();
        assertEquals("House Burger", ranking.get(0).dishName());
        assertEquals(5.0, ranking.get(0).average());
        assertEquals(100, ranking.get(0).count());
        assertEquals(3.0, ranking.get(1).average());

        // a second batch accumulates on top of what is already stored
        consumer.onReview(new ReviewMessage(2L, "Vegan Bowl", 5, "better"));
        consumer.flush();
        RankingEntry second = controller.ranking().stream()
                .filter(r -> r.dishId() == 2L).findFirst().orElseThrow();
        assertEquals(101, second.count());
        assertTrue(second.average() > 3.0);
    }
}
