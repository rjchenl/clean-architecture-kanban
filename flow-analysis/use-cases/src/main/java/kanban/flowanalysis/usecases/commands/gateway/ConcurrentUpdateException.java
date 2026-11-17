package kanban.flowanalysis.usecases.commands.gateway;

/** 樂觀鎖衝突：載入之後，有別人先存了同一個看板流程（ES-6）。 */
public final class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String streamId, long expectedVersion) {
        super("stream " + streamId + " is no longer at version " + expectedVersion);
    }
}
