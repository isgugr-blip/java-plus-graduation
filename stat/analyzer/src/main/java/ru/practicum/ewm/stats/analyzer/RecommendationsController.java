package ru.practicum.ewm.stats.analyzer;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService service;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver, () -> service.getRecommendationsForUser(request.getUserId(), request.getMaxResults()));
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver,
                () -> service.getSimilarEvents(request.getEventId(), request.getUserId(), request.getMaxResults()));
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        respond(responseObserver, () -> service.getInteractionsCount(request.getEventIdList()).entrySet());
    }

    private static void respond(StreamObserver<RecommendedEventProto> responseObserver,
                                Supplier<Collection<Map.Entry<Long, Double>>> events) {
        try {
            events.get().forEach(event -> responseObserver.onNext(RecommendedEventProto.newBuilder()
                    .setEventId(event.getKey())
                    .setScore(event.getValue())
                    .build()));
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Не удалось получить рекомендации", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}
