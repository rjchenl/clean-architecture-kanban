package kanban.flowanalysis.frameworks.persistence;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;

/**
 * 流程分析的資料庫結構：放在自己的 schema flow_analysis，有自己的資料庫版本紀錄（設計第 8 節）。
 * 和看板管理用不同的 Flyway 實例與不同的遷移檔位置，兩邊互不影響。
 */
public final class FlowAnalysisSchemaMigration {

    private FlowAnalysisSchemaMigration() {
    }

    public static void migrate(DataSource dataSource) {
        Flyway.configure()
                .dataSource(dataSource)
                .schemas("flow_analysis")
                .locations("classpath:db/flow-analysis")
                .load()
                .migrate();
    }
}
