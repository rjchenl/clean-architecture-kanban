package kanban.boardmanagement.usecases.probe.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProbeUseCaseTest {

    @Test
    void 測試在有模組宣告的模組裡跑得起來() {
        assertThat(new ProbeUseCase().execute("ok").value()).isEqualTo("ok");
        assertThat(ProbeUseCase.class.getModule().getName()).isEqualTo("kanban.boardmanagement.usecases");
    }
}
