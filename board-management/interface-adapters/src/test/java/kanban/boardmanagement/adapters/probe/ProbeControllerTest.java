package kanban.boardmanagement.adapters.probe;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.entities.probe.EntitiesProbe;
import org.junit.jupiter.api.Test;

class ProbeControllerTest {

    @Test
    void 測試在有模組宣告的模組裡跑得起來() {
        ProbeController controller = new ProbeController(EntitiesProbe::new);

        assertThat(controller.handle("ok")).isEqualTo("ok");
        assertThat(ProbeController.class.getModule().getName()).isEqualTo("kanban.boardmanagement.adapters");
    }
}
