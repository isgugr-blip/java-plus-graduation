package ru.practicum.ewm.stats.analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int NEIGHBOURS = 20;

    private final AnalyzerRepository repository;

    public List<Map.Entry<Long, Double>> getSimilarEvents(long eventId, long userId, int maxResults) {
        Set<Long> interacted = repository.findUserWeights(userId).keySet();
        Map<Long, Double> similar = new HashMap<>();
        for (Similarity similarity : repository.findSimilarities(List.of(eventId))) {
            long other = similarity.other(eventId);
            if (!interacted.contains(other)) {
                similar.put(other, similarity.score());
            }
        }
        return top(similar, maxResults);
    }

    public List<Map.Entry<Long, Double>> getRecommendationsForUser(long userId, int maxResults) {
        Map<Long, Double> weights = repository.findUserWeights(userId);
        if (weights.isEmpty()) {
            return List.of();
        }
        List<Long> recent = repository.findRecentEvents(userId, maxResults);

        Map<Long, Double> candidates = new HashMap<>();
        for (Similarity similarity : repository.findSimilarities(recent)) {
            long other = recent.contains(similarity.eventA()) ? similarity.eventB() : similarity.eventA();
            if (!weights.containsKey(other)) {
                candidates.merge(other, similarity.score(), Math::max);
            }
        }

        Map<Long, Double> predictions = new HashMap<>();
        for (Map.Entry<Long, Double> candidate : top(candidates, maxResults)) {
            long eventId = candidate.getKey();
            List<Similarity> neighbours = repository.findSimilarities(List.of(eventId)).stream()
                    .filter(similarity -> weights.containsKey(similarity.other(eventId)))
                    .sorted(Comparator.comparingDouble(Similarity::score).reversed())
                    .limit(NEIGHBOURS)
                    .toList();
            double weightedSum = 0;
            double scoreSum = 0;
            for (Similarity neighbour : neighbours) {
                weightedSum += neighbour.score() * weights.get(neighbour.other(eventId));
                scoreSum += neighbour.score();
            }
            predictions.put(eventId, scoreSum == 0 ? 0 : weightedSum / scoreSum);
        }
        return top(predictions, maxResults);
    }

    public Map<Long, Double> getInteractionsCount(Collection<Long> eventIds) {
        return eventIds.isEmpty() ? Map.of() : repository.sumWeights(eventIds);
    }

    private static List<Map.Entry<Long, Double>> top(Map<Long, Double> scores, int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .toList();
    }
}
