package kanban.flowanalysis.entities.boardflow;

/**
 * 顯示用文字（看板名稱、欄位名稱、工作項目標題）的共用檢查。
 * 這些文字來自看板管理、已經驗過，流程分析一律接受（F14），不另驗長度；只擋 null，那代表程式有錯。
 */
final class DisplayText {

    private DisplayText() {
    }

    static String of(String raw, String what) {
        if (raw == null) {
            throw new IllegalArgumentException(what + "不可以是 null");
        }
        return raw.strip();
    }
}
