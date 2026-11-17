package kanban.flowanalysis.usecases.commands;

/**
 * 流程分析指令的 Response Model（CQRS-2）：只有成功或失敗、失敗原因，以及必要的識別碼，不帶任何畫面資料。
 *
 * @param failureCode 成功時為 null；失敗時是拒絕原因的名稱，或 BOARD_FLOW_NOT_FOUND、CONCURRENT_UPDATE、INVALID_REQUEST
 * @param createdId   這次指令新建立的看板流程識別碼；沒有新建立東西時為 null
 */
public record CommandResponse(boolean succeeded, String failureCode, String createdId) {

    public static final String BOARD_FLOW_NOT_FOUND = "BOARD_FLOW_NOT_FOUND";
    public static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
    public static final String INVALID_REQUEST = "INVALID_REQUEST";

    public static CommandResponse success(String createdId) {
        return new CommandResponse(true, null, createdId);
    }

    public static CommandResponse failure(String failureCode) {
        return new CommandResponse(false, failureCode, null);
    }
}
