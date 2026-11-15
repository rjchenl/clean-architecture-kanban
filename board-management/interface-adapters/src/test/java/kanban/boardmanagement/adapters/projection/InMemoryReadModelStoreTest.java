package kanban.boardmanagement.adapters.projection;

class InMemoryReadModelStoreTest extends ReadModelStoreContract {

    private final InMemoryReadModelStore store = new InMemoryReadModelStore();

    @Override
    protected ReadModelStore store() {
        return store;
    }
}
