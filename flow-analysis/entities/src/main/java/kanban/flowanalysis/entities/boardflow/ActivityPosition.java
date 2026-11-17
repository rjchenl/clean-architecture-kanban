package kanban.flowanalysis.entities.boardflow;

/**
 * 一筆看板動態在看板管理公布紀錄裡的位置，從 1 起算。看板流程用它認出重送的動態（F14）。
 * 值來自看板管理；不合法代表程式有錯，所以丟 IllegalArgumentException，不是業務規則的拒絕。
 */
public record ActivityPosition(long value) {

    public ActivityPosition {
        if (value < 1) {
            throw new IllegalArgumentException("看板動態位置從 1 起算，收到 " + value);
        }
    }

    public boolean isAfter(ActivityPosition other) {
        return value > other.value;
    }
}
