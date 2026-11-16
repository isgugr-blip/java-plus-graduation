package ru.practicum.ewm.stats.analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.ActionWeights;

@Component
@RequiredArgsConstructor
public class StatsListener {

    private final AnalyzerRepository repository;

    @KafkaListener(topics = "${stats.topics.user-actions}",
            properties = "value.deserializer=ru.practicum.ewm.stats.kafka.UserActionDeserializer")
    public void onUserAction(UserActionAvro action) {
        repository.saveAction(action.getUserId(), action.getEventId(),
                ActionWeights.of(action.getActionType()), action.getTimestamp());
    }

    @KafkaListener(topics = "${stats.topics.events-similarity}",
            properties = "value.deserializer=ru.practicum.ewm.stats.kafka.EventSimilarityDeserializer")
    public void onEventSimilarity(EventSimilarityAvro similarity) {
        repository.saveSimilarity(similarity.getEventA(), similarity.getEventB(),
                similarity.getScore(), similarity.getTimestamp());
    }
}
