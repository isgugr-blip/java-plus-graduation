package ru.practicum.ewm.stats.aggregator;

import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.practicum.ewm.stats.avro.ActionTypeAvro.LIKE;
import static ru.practicum.ewm.stats.avro.ActionTypeAvro.REGISTER;
import static ru.practicum.ewm.stats.avro.ActionTypeAvro.VIEW;

class SimilarityCalculatorTest {

    private static final long A = 1;
    private static final long B = 2;
    private static final long C = 3;
    private static final long D = 4;

    private final SimilarityCalculator calculator = new SimilarityCalculator();

    @Test
    void update_shouldMatchLessonExample() {
        act(1, A, VIEW);
        act(1, A, LIKE);
        act(1, C, VIEW);
        act(2, A, LIKE);
        act(2, B, REGISTER);
        act(2, D, VIEW);
        act(3, B, VIEW);
        act(3, B, LIKE);
        act(3, C, VIEW);
        act(3, B, VIEW);
        act(4, A, VIEW);
        act(4, A, REGISTER);
        act(4, A, VIEW);
        act(4, D, VIEW);
        act(5, B, LIKE);
        act(5, C, REGISTER);
        act(5, C, LIKE);
        List<EventSimilarityAvro> lastForU5 = act(5, D, VIEW);

        assertScore(lastForU5, B, D, 0.8 / Math.sqrt(2.8 * 1.2));

        List<EventSimilarityAvro> afterLike = act(4, A, LIKE);
        assertEquals(1, afterLike.size());
        assertScore(afterLike, A, D, 0.421);
    }

    @Test
    void update_whenWeightNotIncreased_shouldReturnNothing() {
        act(1, A, LIKE);
        act(1, B, LIKE);

        assertTrue(act(1, A, VIEW).isEmpty());
    }

    private List<EventSimilarityAvro> act(long userId, long eventId, ActionTypeAvro type) {
        return calculator.update(new UserActionAvro(userId, eventId, type, Instant.EPOCH));
    }

    private static void assertScore(List<EventSimilarityAvro> similarities, long eventA, long eventB, double expected) {
        EventSimilarityAvro similarity = similarities.stream()
                .filter(s -> s.getEventA() == eventA && s.getEventB() == eventB)
                .findFirst()
                .orElseThrow();
        assertEquals(expected, similarity.getScore(), 0.001);
    }
}
