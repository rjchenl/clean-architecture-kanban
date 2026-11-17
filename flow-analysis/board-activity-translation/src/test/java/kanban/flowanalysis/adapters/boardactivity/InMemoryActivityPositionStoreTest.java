package kanban.flowanalysis.adapters.boardactivity;

class InMemoryActivityPositionStoreTest extends ActivityPositionStoreContract {

    @Override
    protected ActivityPositionStore store() {
        return new InMemoryActivityPositionStore();
    }
}
