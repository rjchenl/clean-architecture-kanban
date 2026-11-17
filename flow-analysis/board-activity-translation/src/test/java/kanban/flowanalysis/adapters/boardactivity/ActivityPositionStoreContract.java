package kanban.flowanalysis.adapters.boardactivity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 防腐層讀取位置的契約測試（CA-6）：記憶體版與 PostgreSQL 版各跑一次。
 * 子類別每次 {@link #store()} 都要給一個還沒處理過任何動態的儲存。
 */
public abstract class ActivityPositionStoreContract {

    protected abstract ActivityPositionStore store();

    @Test
    void 一開始是0() {
        assertThat(store().lastPosition()).isZero();
    }

    @Test
    void 前進後讀得回來() {
        ActivityPositionStore store = store();

        store.advanceTo(7);

        assertThat(store.lastPosition()).isEqualTo(7);
    }

    @Test
    void 不能倒退_丟IllegalArgumentException() {
        ActivityPositionStore store = store();
        store.advanceTo(7);

        assertThatThrownBy(() -> store.advanceTo(6)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store.lastPosition()).isEqualTo(7);
    }

    @Test
    void 前進到同一個位置_不變() {
        ActivityPositionStore store = store();
        store.advanceTo(7);

        store.advanceTo(7);

        assertThat(store.lastPosition()).isEqualTo(7);
    }
}
