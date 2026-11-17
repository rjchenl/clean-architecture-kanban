package kanban.boardmanagement.adapters.projection;

class InMemoryBoardActivityLogTest extends BoardActivityLogStoreContract {

    private final InMemoryBoardActivityLog store = new InMemoryBoardActivityLog();

    @Override
    protected BoardActivityLogStore store() {
        return store;
    }
}
