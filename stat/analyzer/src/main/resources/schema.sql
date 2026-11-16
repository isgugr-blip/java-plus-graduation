CREATE TABLE IF NOT EXISTS user_actions (
    user_id     BIGINT           NOT NULL,
    event_id    BIGINT           NOT NULL,
    weight      DOUBLE PRECISION NOT NULL,
    action_time TIMESTAMP        NOT NULL,
    PRIMARY KEY (user_id, event_id)
);

CREATE INDEX IF NOT EXISTS user_actions_event_id_idx ON user_actions (event_id);

CREATE TABLE IF NOT EXISTS event_similarities (
    event_a    BIGINT           NOT NULL,
    event_b    BIGINT           NOT NULL,
    score      DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP        NOT NULL,
    PRIMARY KEY (event_a, event_b)
);

CREATE INDEX IF NOT EXISTS event_similarities_event_b_idx ON event_similarities (event_b);
