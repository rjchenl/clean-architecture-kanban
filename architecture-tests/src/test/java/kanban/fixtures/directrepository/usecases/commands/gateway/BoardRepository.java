package kanban.fixtures.directrepository.usecases.commands.gateway;

public interface BoardRepository {

    Object load(String id);

    void save(Object board);
}
