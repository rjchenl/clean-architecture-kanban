package kanban.boardmanagement.usecases.commands.gateway;

/** 寫入事件時版本號不符：有別人先改了同一個聚合。 */
public final class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(String streamId, long expectedVersion) {
        super("stream " + streamId + " is no longer at version " + expectedVersion);
    }
}
