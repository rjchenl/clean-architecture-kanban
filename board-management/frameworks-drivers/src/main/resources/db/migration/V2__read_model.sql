-- 查詢端的讀取模型（規則 CQRS-3、ES-8）：只由 Projection 寫入，可以整批清掉後重播事件重建。

-- 看板現況；created_order 記住看板第一次出現的順序，給看板清單排序用
CREATE TABLE board_view (
    board_id      TEXT PRIMARY KEY,
    created_order BIGINT GENERATED ALWAYS AS IDENTITY,
    doc           JSONB NOT NULL
);

-- 卡片移動歷程
CREATE TABLE card_history_view (
    board_id TEXT  NOT NULL,
    card_id  TEXT  NOT NULL,
    doc      JSONB NOT NULL,
    PRIMARY KEY (board_id, card_id)
);

-- Projection 已經處理到事件紀錄的哪個位置
CREATE TABLE projection_checkpoint (
    name     TEXT PRIMARY KEY,
    position BIGINT NOT NULL
);
