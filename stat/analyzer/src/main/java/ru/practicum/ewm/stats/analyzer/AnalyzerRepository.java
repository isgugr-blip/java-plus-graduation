package ru.practicum.ewm.stats.analyzer;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AnalyzerRepository {

    private final JdbcClient jdbc;

    public void saveAction(long userId, long eventId, double weight, Instant timestamp) {
        jdbc.sql("""
                        INSERT INTO user_actions (user_id, event_id, weight, action_time)
                        VALUES (:userId, :eventId, :weight, :time)
                        ON CONFLICT (user_id, event_id) DO UPDATE
                        SET weight = GREATEST(user_actions.weight, EXCLUDED.weight),
                            action_time = GREATEST(user_actions.action_time, EXCLUDED.action_time)
                        """)
                .param("userId", userId)
                .param("eventId", eventId)
                .param("weight", weight)
                .param("time", Timestamp.from(timestamp))
                .update();
    }

    public void saveSimilarity(long eventA, long eventB, double score, Instant timestamp) {
        jdbc.sql("""
                        INSERT INTO event_similarities (event_a, event_b, score, updated_at)
                        VALUES (:eventA, :eventB, :score, :time)
                        ON CONFLICT (event_a, event_b) DO UPDATE
                        SET score = EXCLUDED.score, updated_at = EXCLUDED.updated_at
                        """)
                .param("eventA", eventA)
                .param("eventB", eventB)
                .param("score", score)
                .param("time", Timestamp.from(timestamp))
                .update();
    }

    public Map<Long, Double> findUserWeights(long userId) {
        Map<Long, Double> weights = new HashMap<>();
        jdbc.sql("SELECT event_id, weight FROM user_actions WHERE user_id = :userId")
                .param("userId", userId)
                .query(rs -> {
                    weights.put(rs.getLong("event_id"), rs.getDouble("weight"));
                });
        return weights;
    }

    public List<Long> findRecentEvents(long userId, int limit) {
        return jdbc.sql("""
                        SELECT event_id FROM user_actions
                        WHERE user_id = :userId
                        ORDER BY action_time DESC
                        LIMIT :limit
                        """)
                .param("userId", userId)
                .param("limit", limit)
                .query(Long.class)
                .list();
    }

    public List<Similarity> findSimilarities(Collection<Long> eventIds) {
        return jdbc.sql("""
                        SELECT event_a, event_b, score FROM event_similarities
                        WHERE event_a IN (:ids) OR event_b IN (:ids)
                        """)
                .param("ids", eventIds)
                .query((rs, rowNum) -> new Similarity(rs.getLong("event_a"), rs.getLong("event_b"), rs.getDouble("score")))
                .list();
    }

    public Map<Long, Double> sumWeights(Collection<Long> eventIds) {
        Map<Long, Double> sums = new HashMap<>();
        jdbc.sql("""
                        SELECT event_id, SUM(weight) AS total FROM user_actions
                        WHERE event_id IN (:ids)
                        GROUP BY event_id
                        """)
                .param("ids", eventIds)
                .query(rs -> {
                    sums.put(rs.getLong("event_id"), rs.getDouble("total"));
                });
        return sums;
    }
}
