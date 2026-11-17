package kanban.boardmanagement.adapters.projection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivity;
import kanban.boardmanagement.usecases.queries.readboardactivities.BoardActivityKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 看板動態紀錄的契約測試（CA-6）：記憶體版與 PostgreSQL 版各跑一次。
 * 子類別在同一支測試裡每次 {@link #store()} 都要回同一個儲存。
 */
public abstract class BoardActivityLogStoreContract {

    protected static final Instant AT = Instant.parse("2026-11-16T01:00:00Z");

    protected abstract BoardActivityLogStore store();

    protected static BoardActivity renamed(long position, String name) {
        return new BoardActivity(position, BoardActivityKind.BOARD_RENAMED, "board-1", AT, name,
                null, null, null, null, null, null, null);
    }

    protected static BoardActivity cardMoved(long position) {
        return new BoardActivity(position, BoardActivityKind.CARD_MOVED, "board-1", AT, null,
                null, null, null, "card-1", null, "col-todo", "col-doing");
    }

    @BeforeEach
    void startEmpty() {
        store().inExclusiveTransaction(store()::reset);
    }

    @Test
    void 一開始是空的_checkpoint為0() {
        assertThat(store().activitiesAfter(0, 500)).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 寫入的動態依位置讀得回來_每個欄位都保留() {
        store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "產品開發"));
            store().append(cardMoved(4));
        });

        assertThat(store().activitiesAfter(0, 500)).containsExactly(renamed(1, "產品開發"), cardMoved(4));
    }

    @Test
    void 同一個位置再寫一次_覆蓋而不是多一筆() {
        store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "舊名稱"));
            store().append(renamed(1, "新名稱"));
        });

        assertThat(store().activitiesAfter(0, 500)).containsExactly(renamed(1, "新名稱"));
    }

    @Test
    void activitiesAfter只回傳位置之後_最多maxCount筆() {
        store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "一"));
            store().append(renamed(2, "二"));
            store().append(renamed(5, "五"));
            store().append(renamed(9, "九"));
        });

        assertThat(store().activitiesAfter(1, 2)).containsExactly(renamed(2, "二"), renamed(5, "五"));
        assertThat(store().activitiesAfter(9, 10)).isEmpty();
    }

    @Test
    void checkpoint可以前進() {
        store().inExclusiveTransaction(() -> store().advanceCheckpoint(7));

        assertThat(store().checkpoint()).isEqualTo(7);
    }

    @Test
    void 交易內讀得到自己剛寫的資料() {
        store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "產品開發"));
            store().advanceCheckpoint(1);
            assertThat(store().activitiesAfter(0, 10)).containsExactly(renamed(1, "產品開發"));
            assertThat(store().checkpoint()).isEqualTo(1);
        });
    }

    @Test
    void reset後全部清空且checkpoint為0() {
        store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "產品開發"));
            store().advanceCheckpoint(1);
        });

        store().inExclusiveTransaction(store()::reset);

        assertThat(store().activitiesAfter(0, 500)).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 交易內丟例外_寫入與checkpoint都不保留() {
        assertThatThrownBy(() -> store().inExclusiveTransaction(() -> {
            store().append(renamed(1, "產品開發"));
            store().advanceCheckpoint(1);
            throw new IllegalStateException("中途失敗");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store().activitiesAfter(0, 500)).isEmpty();
        assertThat(store().checkpoint()).isZero();
    }

    @Test
    void 寫入方法只能在交易內呼叫() {
        assertThatThrownBy(() -> store().append(renamed(1, "產品開發"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().advanceCheckpoint(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> store().reset()).isInstanceOf(IllegalStateException.class);
    }
}
