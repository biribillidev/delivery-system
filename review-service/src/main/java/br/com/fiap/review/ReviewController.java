package br.com.fiap.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
public class ReviewController {

    private final ReviewSummaryRepository repository;

    public ReviewController(ReviewSummaryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/reviews/ranking")
    public List<RankingEntry> ranking() {
        return repository.findAll().stream()
                .map(s -> new RankingEntry(s.getDishId(), s.getDishName(),
                        Math.round(s.average() * 100.0) / 100.0, s.getReviewCount()))
                .sorted(Comparator.comparingDouble(RankingEntry::average).reversed()
                        .thenComparing(Comparator.comparingLong(RankingEntry::count).reversed()))
                .toList();
    }

}
