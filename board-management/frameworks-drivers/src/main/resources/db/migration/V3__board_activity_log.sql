-- 看板動態紀錄（第二階段設計第 4.3 節）：看板管理公布給其他業務情境讀的已發生的事。
-- 這是讀取模型，只由 BoardActivityProjection 寫入，可以整批清掉後重播事件重建（CQRS-5、ES-8）。
-- position＝來源事件在 board_events 的全域位置，重建後完全一樣，下游已經讀到的位置仍然有效。
CREATE TABLE board_activity_log (
    position BIGINT PRIMARY KEY,
    doc      JSONB  NOT NULL
);
