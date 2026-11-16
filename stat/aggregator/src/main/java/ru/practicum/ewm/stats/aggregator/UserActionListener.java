package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionListener {

    private final SimilarityCalculator calculator;
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;

    @Value("${stats.topics.events-similarity}")
    private String topic;

    @KafkaListener(topics = "${stats.topics.user-actions}")
    public void onUserAction(UserActionAvro action) {
        List<EventSimilarityAvro> similarities = calculator.update(action);
        similarities.forEach(similarity -> kafkaTemplate.send(topic, similarity));
        log.debug("Действие {} обработано, пересчитано коэффициентов сходства: {}", action, similarities.size());
    }
}
