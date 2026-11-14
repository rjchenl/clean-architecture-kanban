package kanban.boardmanagement.entities.probe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EntitiesProbeTest {

    @Test
    void 測試在有模組宣告的模組裡跑得起來() {
        assertThat(new EntitiesProbe("ok").value()).isEqualTo("ok");
        assertThat(EntitiesProbe.class.getModule().getName()).isEqualTo("kanban.boardmanagement.entities");
    }
}
