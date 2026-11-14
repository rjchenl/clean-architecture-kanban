package kanban.boardmanagement.adapters.presenters;

/** Presenter 共用的顯示文字。 */
final class Labels {

    private Labels() {
    }

    static String archived(boolean archived) {
        return archived ? "已封存" : "";
    }
}
