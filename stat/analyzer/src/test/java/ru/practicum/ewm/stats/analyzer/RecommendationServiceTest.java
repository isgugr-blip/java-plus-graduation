package ru.practicum.ewm.stats.analyzer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private static final long USER = 7;

    private final AnalyzerRepository repository = mock(AnalyzerRepository.class);
    private final RecommendationService service = new RecommendationService(repository);

    @Test
    void getRecommendationsForUser_shouldPredictWeightedScore() {
        List<Similarity> similarities = List.of(
                new Similarity(1, 2, 0.9),
                new Similarity(1, 3, 0.7),
                new Similarity(1, 4, 0.6),
                new Similarity(2, 5, 0.1));
        when(repository.findUserWeights(USER)).thenReturn(Map.of(2L, 0.8, 3L, 0.4, 4L, 0.8));
        when(repository.findRecentEvents(USER, 1)).thenReturn(List.of(2L));
        when(repository.findSimilarities(List.of(2L))).thenReturn(List.of(similarities.get(0), similarities.get(3)));
        when(repository.findSimilarities(List.of(1L))).thenReturn(similarities.subList(0, 3));

        List<Map.Entry<Long, Double>> result = service.getRecommendationsForUser(USER, 1);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getKey());
        assertEquals(1.48 / 2.2, result.get(0).getValue(), 0.001);
    }

    @Test
    void getRecommendationsForUser_withoutInteractions_shouldReturnEmpty() {
        when(repository.findUserWeights(USER)).thenReturn(Map.of());

        assertTrue(service.getRecommendationsForUser(USER, 10).isEmpty());
    }

    @Test
    void getSimilarEvents_shouldSkipInteractedAndSortByScore() {
        when(repository.findUserWeights(USER)).thenReturn(Map.of(3L, 1.0));
        when(repository.findSimilarities(List.of(1L))).thenReturn(List.of(
                new Similarity(1, 2, 0.3),
                new Similarity(1, 3, 0.9),
                new Similarity(0, 1, 0.5)));

        List<Map.Entry<Long, Double>> result = service.getSimilarEvents(1, USER, 5);

        assertEquals(List.of(0L, 2L), result.stream().map(Map.Entry::getKey).toList());
    }
}
