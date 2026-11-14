package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import kanban.boardmanagement.adapters.probe.ProbeController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class KanbanApplicationTest {

    @Autowired
    ProbeController probeController;

    @Test
    void 應用程式啟動得起來而且物件由Main接好() {
        assertThat(probeController.handle("ok")).isEqualTo("ok");
    }
}
