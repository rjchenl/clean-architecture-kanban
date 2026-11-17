package kanban.flowanalysis.entities.boardflow;

import java.time.Instant;

/** 停留紀錄：工作項目在什麼時候進入了哪一欄（看板流程的內部物件，不公開）。 */
record ColumnVisit(FlowColumnId flowColumnId, Instant enteredAt) {
}
