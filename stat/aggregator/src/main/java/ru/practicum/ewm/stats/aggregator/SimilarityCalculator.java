package ru.practicum.ewm.stats.aggregator;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.ActionWeights;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SimilarityCalculator {

    private final Map<Long, Map<Long, Double>> weights = new HashMap<>();
    private final Map<Long, Double> weightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightSums = new HashMap<>();

    public List<EventSimilarityAvro> update(UserActionAvro action) {
        long userId = action.getUserId();
        long eventId = action.getEventId();
        double newWeight = ActionWeights.of(action.getActionType());

        Map<Long, Double> eventWeights = weights.computeIfAbsent(eventId, id -> new HashMap<>());
        double oldWeight = eventWeights.getOrDefault(userId, 0.0);
        if (newWeight <= oldWeight) {
            return List.of();
        }
        eventWeights.put(userId, newWeight);
        weightSums.merge(eventId, newWeight - oldWeight, Double::sum);

        List<EventSimilarityAvro> similarities = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> other : weights.entrySet()) {
            long otherId = other.getKey();
            Double otherWeight = other.getValue().get(userId);
            if (otherId == eventId || otherWeight == null) {
                continue;
            }
            long first = Math.min(eventId, otherId);
            long second = Math.max(eventId, otherId);
            double minDelta = Math.min(newWeight, otherWeight) - Math.min(oldWeight, otherWeight);
            double minSum = minWeightSums.computeIfAbsent(first, id -> new HashMap<>())
                    .merge(second, minDelta, Double::sum);
            double score = minSum / (Math.sqrt(weightSums.get(eventId)) * Math.sqrt(weightSums.get(otherId)));

            similarities.add(EventSimilarityAvro.newBuilder()
                    .setEventA(first)
                    .setEventB(second)
                    .setScore(score)
                    .setTimestamp(action.getTimestamp())
                    .build());
        }
        return similarities;
    }
}
