-- 流程分析的讀取模型（規則 CQRS-3、ES-8）：只由流程分析的 Projection 寫入，可以整批清掉後重播事件重建。

-- 看板流程文件；created_order 記住第一次出現的順序，給看板流程清單排序用
CREATE TABLE board_flow_view (
    board_flow_id TEXT PRIMARY KEY,
    created_order BIGINT GENERATED ALWAYS AS IDENTITY,
    doc           JSONB NOT NULL
);

-- 完成的工作項目：週的起點由 Projection 依統計方式 S1 算好存進來，讀的時候只做計數、平均、最長（S2）
CREATE TABLE completed_work_item_view (
    board_flow_id     TEXT        NOT NULL,
    work_item_id      TEXT        NOT NULL,
    completed_at      TIMESTAMPTZ NOT NULL,
    week_starts_at    TIMESTAMPTZ NOT NULL,
    lead_time_seconds BIGINT      NOT NULL,
    PRIMARY KEY (board_flow_id, work_item_id)
);

-- 流程分析的 Projection 已經處理到事件紀錄的哪個位置
CREATE TABLE projection_checkpoint (
    name     TEXT PRIMARY KEY,
    position BIGINT NOT NULL
);
