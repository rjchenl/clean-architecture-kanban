package kanban.boardmanagement.usecases.queries.readboardactivities;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 公布格式的契約測試（設計第 4.4 節）：看板動態是看板管理對其他業務情境的承諾，只能新增、不能刪除或改名。
 * 新增種類或欄位，這裡照樣通過；刪除或改名既有的，這裡就失敗——改之前要先和下游的防腐層一起決定。
 */
class PublishedActivityContractTest {

    @Test
    void 公布格式的欄位只能新增() {
        List<String> components = Arrays.stream(BoardActivity.class.getRecordComponents())
                .map(PublishedActivityContractTest::describe)
                .toList();

        assertThat(components).containsSubsequence(
                "position: long",
                "kind: BoardActivityKind",
                "boardId: String",
                "occurredAt: " + Instant.class.getSimpleName(),
                "boardName: String",
                "columnId: String",
                "columnName: String",
                "columnPosition: Integer",
                "cardId: String",
                "cardTitle: String",
                "fromColumnId: String",
                "toColumnId: String");
    }

    @Test
    void 公布的種類只能新增() {
        assertThat(Arrays.stream(BoardActivityKind.values()).map(Enum::name)).contains(
                "BOARD_CREATED", "BOARD_RENAMED", "BOARD_ARCHIVED",
                "COLUMN_ADDED", "COLUMN_RENAMED", "COLUMN_MOVED", "COLUMN_REMOVED",
                "CARD_ADDED", "CARD_CONTENT_CHANGED", "CARD_MOVED", "CARD_REMOVED");
    }

    private static String describe(RecordComponent component) {
        return component.getName() + ": " + component.getType().getSimpleName();
    }
}
