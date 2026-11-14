package kanban.boardmanagement.usecases.commands.support;

import java.util.Optional;
import java.util.function.Supplier;
import kanban.boardmanagement.entities.board.Board;
import kanban.boardmanagement.entities.board.BoardId;
import kanban.boardmanagement.entities.board.BusinessRuleViolation;
import kanban.boardmanagement.usecases.commands.CommandResponse;
import kanban.boardmanagement.usecases.commands.gateway.BoardRepository;
import kanban.boardmanagement.usecases.commands.gateway.ConcurrentUpdateException;

/**
 * 每個指令 Use Case 共用的協調流程：取出聚合 → 呼叫它 → 存回去 → 整理成 Response Model（DDD-9）。
 * 這裡沒有任何業務判斷；能不能做由聚合決定。這個套件不對外公開（module-info）。
 */
public final class BoardCommand {

    private BoardCommand() {
    }

    /** 對聚合做一件事；回傳這次新建立的識別碼，沒有就回傳 null。 */
    @FunctionalInterface
    public interface Action {
        String on(Board board);
    }

    /** 檢查請求欄位都有給，並回傳看板識別碼。 */
    public static String required(String boardId, Object... otherFields) {
        if (boardId == null) {
            throw new MissingField();
        }
        for (Object field : otherFields) {
            if (field == null) {
                throw new MissingField();
            }
        }
        return boardId;
    }

    /** 對既有的看板執行一個指令。 */
    public static CommandResponse onExistingBoard(BoardRepository boards, Supplier<String> boardId, Action action) {
        return guarded(() -> {
            Optional<Board> found = boards.load(new BoardId(boardId.get()));
            if (found.isEmpty()) {
                return CommandResponse.failure(CommandResponse.BOARD_NOT_FOUND);
            }
            Board board = found.get();
            String createdId = action.on(board);
            boards.save(board);
            return CommandResponse.success(createdId);
        });
    }

    /** 建立一個新的看板。 */
    public static CommandResponse onNewBoard(BoardRepository boards, Supplier<Board> creation) {
        return guarded(() -> {
            Board board = creation.get();
            boards.save(board);
            return CommandResponse.success(board.id().value());
        });
    }

    /** 只把預期內的失敗轉成失敗原因；其他例外（例如儲存設備故障）照常往外丟。 */
    private static CommandResponse guarded(Supplier<CommandResponse> work) {
        try {
            return work.get();
        } catch (BusinessRuleViolation violation) {
            return CommandResponse.failure(violation.reason().name());
        } catch (ConcurrentUpdateException conflict) {
            return CommandResponse.failure(CommandResponse.CONCURRENT_UPDATE);
        } catch (MissingField | NullPointerException | IllegalArgumentException invalid) {
            return CommandResponse.failure(CommandResponse.INVALID_REQUEST);
        }
    }

    private static final class MissingField extends RuntimeException {
    }
}
