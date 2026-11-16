package ru.practicum.ewm.stats.client;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AnalyzerClient {

    private static final long DEADLINE_SECONDS = 3;

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Map<Long, Double> getInteractionsCount(Collection<Long> eventIds) {
        Map<Long, Double> ratings = new HashMap<>();
        if (eventIds.isEmpty()) {
            return ratings;
        }
        try {
            stub().getInteractionsCount(InteractionsCountRequestProto.newBuilder().addAllEventId(eventIds).build())
                    .forEachRemaining(event -> ratings.put(event.getEventId(), event.getScore()));
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинг мероприятий {}", eventIds, e);
        }
        return ratings;
    }

    public List<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        List<RecommendedEventProto> events = new ArrayList<>();
        stub().getRecommendationsForUser(UserPredictionsRequestProto.newBuilder()
                        .setUserId(userId)
                        .setMaxResults(maxResults)
                        .build())
                .forEachRemaining(events::add);
        return events;
    }

    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub stub() {
        return client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS);
    }
}
