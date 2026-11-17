-- 防腐層已經把看板管理公布的看板動態處理到哪個位置（設計第 7.5 節）。
-- 不是事件、也不是聚合現況：只是同步的進度。弄丟了就從頭重讀，由看板流程依 F14 略過已處理的動態。
-- Flyway 遷移時把 flow_analysis 設成預設 schema，所以這裡不寫前綴。
CREATE TABLE board_activity_position (
    name     TEXT PRIMARY KEY,
    position BIGINT NOT NULL
);
