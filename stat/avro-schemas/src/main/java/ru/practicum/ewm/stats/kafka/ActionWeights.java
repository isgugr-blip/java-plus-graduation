package ru.practicum.ewm.stats.kafka;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;

public final class ActionWeights {

    private ActionWeights() {
    }

    public static double of(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
        };
    }
}
