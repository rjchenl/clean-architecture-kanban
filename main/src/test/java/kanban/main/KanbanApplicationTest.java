package kanban.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class KanbanApplicationTest {

    @Autowired
    ApplicationContext context;

    @Test
    void 應用程式啟動得起來() {
        assertThat(context).isNotNull();
    }
}
