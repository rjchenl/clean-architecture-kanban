package kanban.flowanalysis.adapters.projection;

class InMemoryReadModelStoreTest extends ReadModelStoreContract {

    private final InMemoryReadModelStore store = new InMemoryReadModelStore(CALENDAR);

    @Override
    protected ReadModelStore store() {
        return store;
    }
}
