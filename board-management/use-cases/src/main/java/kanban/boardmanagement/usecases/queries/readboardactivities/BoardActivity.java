package kanban.boardmanagement.usecases.queries.readboardactivities;

import java.time.Instant;

/**
 * 看板動態：看板管理公布給其他業務情境讀的、已經發生的一件事（Published Language，DDD-3）。
 * 只含簡單資料（CA-5）；格式只增不改，由 PublishedActivityContractTest 守住。
 *
 * <p>每一種動態用到的欄位如下，沒用到的一律是 null：
 * <ul>
 *   <li>BOARD_CREATED、BOARD_RENAMED：boardName</li>
 *   <li>BOARD_ARCHIVED：無</li>
 *   <li>COLUMN_ADDED（加在最右邊）、COLUMN_RENAMED：columnId、columnName</li>
 *   <li>COLUMN_MOVED：columnId、columnPosition（在現有欄位中的新位置，從 0 起算）</li>
 *   <li>COLUMN_REMOVED：columnId</li>
 *   <li>CARD_ADDED：cardId、columnId、cardTitle</li>
 *   <li>CARD_CONTENT_CHANGED：cardId、cardTitle（不帶卡片說明）</li>
 *   <li>CARD_MOVED：cardId、fromColumnId、toColumnId</li>
 *   <li>CARD_REMOVED：cardId、columnId（被刪除時所在的欄）</li>
 * </ul>
 *
 * @param position   來源事件在事件紀錄裡的全域位置，從 1 起算；看板動態紀錄清掉重建後位置不變
 * @param occurredAt 看板上實際發生的時間
 */
public record BoardActivity(long position, BoardActivityKind kind, String boardId, Instant occurredAt,
        String boardName, String columnId, String columnName, Integer columnPosition,
        String cardId, String cardTitle, String fromColumnId, String toColumnId) {
}
