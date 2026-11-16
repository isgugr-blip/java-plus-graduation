package ru.practicum.ewm.stats.client;

import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CollectorClient {

    private static final long DEADLINE_SECONDS = 3;

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendView(long userId, long eventId) {
        sendQuietly(userId, eventId, ActionTypeProto.ACTION_VIEW);
    }

    public void sendRegistration(long userId, long eventId) {
        sendQuietly(userId, eventId, ActionTypeProto.ACTION_REGISTER);
    }

    public void sendLike(long userId, long eventId) {
        send(userId, eventId, ActionTypeProto.ACTION_LIKE);
    }

    private void sendQuietly(long userId, long eventId, ActionTypeProto type) {
        try {
            send(userId, eventId, type);
        } catch (Exception e) {
            log.warn("Не удалось отправить действие {} пользователя {} с мероприятием {}", type, userId, eventId, e);
        }
    }

    private void send(long userId, long eventId, ActionTypeProto type) {
        Instant now = Instant.now();
        client.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                .collectUserAction(UserActionProto.newBuilder()
                        .setUserId(userId)
                        .setEventId(eventId)
                        .setActionType(type)
                        .setTimestamp(Timestamp.newBuilder()
                                .setSeconds(now.getEpochSecond())
                                .setNanos(now.getNano()))
                        .build());
    }
}
