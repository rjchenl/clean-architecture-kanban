-- 流程分析寫入端唯一的資料表：事件紀錄（規則 ES-1）。和看板管理的事件表同一種設計，放在流程分析自己的 schema。
-- 寫入端沒有任何「看板流程現況」資料表；看板流程的狀態一律由重播這裡的事件算出。
-- Flyway 遷移時把 flow_analysis 設成預設 schema，所以這裡不寫前綴。
CREATE TABLE board_flow_events (
    global_position BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    stream_id       TEXT        NOT NULL,
    stream_version  BIGINT      NOT NULL,
    event_type      TEXT        NOT NULL,
    schema_version  INT         NOT NULL,
    payload         JSONB       NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    -- 樂觀鎖（規則 ES-6）：同一條串流的同一個版本號只能有一筆
    UNIQUE (stream_id, stream_version)
);

-- 事件紀錄只能新增（規則 ES-2）：修改、刪除、清空一律由資料庫拒絕。
CREATE FUNCTION board_flow_events_append_only() RETURNS trigger
    LANGUAGE plpgsql AS
$$
BEGIN
    RAISE EXCEPTION 'board_flow_events is append-only';
END
$$;

CREATE TRIGGER board_flow_events_no_update_or_delete
    BEFORE UPDATE OR DELETE ON board_flow_events
    FOR EACH ROW EXECUTE FUNCTION board_flow_events_append_only();

CREATE TRIGGER board_flow_events_no_truncate
    BEFORE TRUNCATE ON board_flow_events
    FOR EACH STATEMENT EXECUTE FUNCTION board_flow_events_append_only();
