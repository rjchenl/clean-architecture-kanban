package kanban.fixtures.staterepository.adapters.eventstore;

import javax.sql.DataSource;
import kanban.fixtures.staterepository.usecases.commands.gateway.BoardRepository;

/** 違規樣本（ES-1）：Repository 不經過事件紀錄，直接存取資料庫。 */
public final class TableBackedBoardRepository implements BoardRepository {
    DataSource dataSource;
}
