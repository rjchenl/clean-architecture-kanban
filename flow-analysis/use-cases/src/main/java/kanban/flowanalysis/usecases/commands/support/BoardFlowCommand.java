package kanban.flowanalysis.usecases.commands.support;

import java.util.Optional;
import java.util.function.Supplier;
import kanban.flowanalysis.entities.boardflow.BoardFlow;
import kanban.flowanalysis.entities.boardflow.BoardFlowId;
import kanban.flowanalysis.entities.boardflow.BusinessRuleViolation;
import kanban.flowanalysis.usecases.commands.CommandResponse;
import kanban.flowanalysis.usecases.commands.gateway.BoardFlowRepository;
import kanban.flowanalysis.usecases.commands.gateway.ConcurrentUpdateException;

/**
 * 流程分析每個指令 Use Case 共用的協調流程：取出看板流程 → 呼叫它 → 存回去 → 整理成 Response Model（DDD-9）。
 * 這裡沒有任何業務判斷；能不能做由看板流程決定。這個套件不對外公開（module-info）。
 * 和看板管理的 BoardCommand 長得像，但各自演變（意外的重複，DDD-2）。
 */
public final class BoardFlowCommand {

    private BoardFlowCommand() {
    }

    /** 對看板流程做一件事；回傳這次新建立的識別碼，沒有就回傳 null。 */
    @FunctionalInterface
    public interface Action {
        String on(BoardFlow flow);
    }

    /** 檢查請求欄位都有給，並回傳第一個欄位（通常是看板流程識別碼）。 */
    public static String required(String first, Object... otherFields) {
        if (first == null) {
            throw new MissingField();
        }
        for (Object field : otherFields) {
            if (field == null) {
                throw new MissingField();
            }
        }
        return first;
    }

    /** 對既有的看板流程執行一個指令；找不到就回覆 BOARD_FLOW_NOT_FOUND。 */
    public static CommandResponse onExistingBoardFlow(BoardFlowRepository flows, Supplier<String> boardFlowId,
            Action action) {
        return onBoardFlow(flows, boardFlowId, () -> null, action);
    }

    /**
     * 看板流程存在就對它執行 whenExisting；不存在時改用 whenMissing 建立一個（whenMissing 回傳 null 代表不該建立，
     * 回覆 BOARD_FLOW_NOT_FOUND）。一個指令只載入、只存回一個看板流程（DDD-5）。
     */
    public static CommandResponse onBoardFlow(BoardFlowRepository flows, Supplier<String> boardFlowId,
            Supplier<BoardFlow> whenMissing, Action whenExisting) {
        Optional<String> id = readRequest(boardFlowId);
        if (id.isEmpty()) {
            return CommandResponse.failure(CommandResponse.INVALID_REQUEST);
        }
        return guarded(() -> {
            Optional<BoardFlow> found = flows.load(new BoardFlowId(id.get()));
            if (found.isPresent()) {
                BoardFlow flow = found.get();
                String createdId = whenExisting.on(flow);
                flows.save(flow);
                return CommandResponse.success(createdId);
            }
            BoardFlow created = whenMissing.get();
            if (created == null) {
                return CommandResponse.failure(CommandResponse.BOARD_FLOW_NOT_FOUND);
            }
            flows.save(created);
            return CommandResponse.success(created.id().value());
        });
    }

    /**
     * 讀取請求欄位。只有這一步會把「請求本身是 null」或「必要欄位沒給」當成請求不完整；
     * 之後執行指令時發生的 NullPointerException、IllegalArgumentException、IllegalStateException 都是程式錯誤，不在這裡處理。
     */
    private static Optional<String> readRequest(Supplier<String> requestFields) {
        try {
            return Optional.of(requestFields.get());
        } catch (MissingField | NullPointerException requestIncomplete) {
            return Optional.empty();
        }
    }

    /** 只把預期內的失敗轉成失敗原因；其他例外（程式錯誤、儲存設備故障）照常往外丟。 */
    private static CommandResponse guarded(Supplier<CommandResponse> work) {
        try {
            return work.get();
        } catch (BusinessRuleViolation violation) {
            return CommandResponse.failure(violation.reason().name());
        } catch (ConcurrentUpdateException conflict) {
            return CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE);
        }
    }

    private static final class MissingField extends RuntimeException {
    }
}
