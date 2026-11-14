package kanban.main;

import kanban.boardmanagement.adapters.probe.ProbeController;
import kanban.boardmanagement.usecases.probe.impl.ProbeUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 骨架驗證用：只有 Main 能 new 出 Use Case 的實作類別。Task 7 移除。 */
@Configuration
class ProbeWiring {

    @Bean
    ProbeController probeController() {
        return new ProbeController(new ProbeUseCase());
    }
}
