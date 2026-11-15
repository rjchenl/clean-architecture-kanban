package kanban.boardmanagement.frameworks.web;

import kanban.boardmanagement.adapters.presenters.BoardListViewModel;
import kanban.boardmanagement.adapters.presenters.BoardViewModel;
import kanban.boardmanagement.adapters.presenters.CardHistoryViewModel;
import kanban.boardmanagement.adapters.presenters.CommandViewModel;

/**
 * HTTP 薄殼要的東西：每個請求對應一個方法，收原始輸入、回傳整理好的 ViewModel。
 * 介面定義在這一圈，由 Main 實作——薄殼不認識 Use Case、Controller、Presenter 怎麼接（CA-8）。
 */
public interface BoardManagementRequests {

    /** 建立看板。 */
    CommandViewModel createBoard(String name);

    /** 看板改名。 */
    CommandViewModel renameBoard(String boardId, String name);

    /** 封存看板。 */
    CommandViewModel archiveBoard(String boardId);

    /** 新增欄位。 */
    CommandViewModel addColumn(String boardId, String name);

    /** 欄位改名。 */
    CommandViewModel renameColumn(String boardId, String columnId, String name);

    /** 調整欄位順序。 */
    CommandViewModel moveColumn(String boardId, String columnId, Integer toPosition);

    /** 設定在製品上限。 */
    CommandViewModel setColumnWipLimit(String boardId, String columnId, Integer limit);

    /** 取消在製品上限。 */
    CommandViewModel removeColumnWipLimit(String boardId, String columnId);

    /** 刪除欄位。 */
    CommandViewModel removeColumn(String boardId, String columnId);

    /** 新增卡片。 */
    CommandViewModel addCard(String boardId, String columnId, String title, String description);

    /** 修改卡片內容。 */
    CommandViewModel changeCardContent(String boardId, String cardId, String title, String description);

    /** 移動卡片。 */
    CommandViewModel moveCard(String boardId, String cardId, String toColumnId, Integer toPosition);

    /** 欄內排序。 */
    CommandViewModel repositionCard(String boardId, String cardId, Integer toPosition);

    /** 刪除卡片。 */
    CommandViewModel removeCard(String boardId, String cardId);

    /** 看板現況。 */
    BoardViewModel viewBoard(String boardId);

    /** 看板清單。 */
    BoardListViewModel listBoards();

    /** 卡片移動歷程。 */
    CardHistoryViewModel viewCardHistory(String boardId, String cardId);

    /** 請求內容根本讀不懂（例如不是合法的 JSON）時要回覆的內容。訊息文字仍然出自 Presenter。 */
    CommandViewModel invalidRequest();
}
