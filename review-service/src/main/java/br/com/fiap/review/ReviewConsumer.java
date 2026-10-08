package br.com.fiap.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Buffers reviews in memory and writes them to the database in batches, so the
 * database sees one write per dish every 5 seconds instead of one per message.
 */
@Component
public class ReviewConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReviewConsumer.class);

    record Accumulated(String dishName, long ratingSum, long count) {
        Accumulated merge(Accumulated other) {
            return new Accumulated(other.dishName, ratingSum + other.ratingSum, count + other.count);
        }
    }

    private final ConcurrentHashMap<Long, Accumulated> buffer = new ConcurrentHashMap<>();
    private final ReviewSummaryRepository repository;

    public ReviewConsumer(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = RabbitConfig.REVIEWS_QUEUE)
    public void onReview(ReviewMessage message) {
        buffer.merge(message.dishId(),
                new Accumulated(message.dishName(), message.rating(), 1),
                Accumulated::merge);
        log.info("Review received for dish {} (rating {})", message.dishId(), message.rating());
    }

    @Scheduled(fixedDelay = 5_000)
    public void flush() {
        if (buffer.isEmpty()) return;

        // remove() is atomic per key: a review arriving during the flush stays for the next one
        for (Long dishId : new ArrayList<>(buffer.keySet())) {
            Accumulated acc = buffer.remove(dishId);
            if (acc != null) persist(dishId, acc);
        }
    }

    private void persist(Long dishId, Accumulated acc) {
        ReviewSummary summary = repository.findById(dishId)
                .orElseGet(() -> new ReviewSummary(dishId, acc.dishName()));
        summary.add(acc.dishName(), acc.ratingSum(), acc.count());
        repository.save(summary);
        log.info("Flushed {} review(s) for dish {}", acc.count(), dishId);
    }

}
