package kanban.fixtures.entrypointelsewhere.frameworks.jobs;

import org.springframework.scheduling.annotation.Scheduled;

/** 違規樣本（CA-7，2026-11-17 獨立審查 I-1）：排程放在 scheduling 以外的套件，躲開「背景排程只轉手」。 */
public final class QuickSync {

    @Scheduled(fixedDelay = 1000)
    void sync() {
    }
}
