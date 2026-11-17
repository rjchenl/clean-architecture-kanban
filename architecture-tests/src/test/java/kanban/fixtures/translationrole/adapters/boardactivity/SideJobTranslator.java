package kanban.fixtures.translationrole.adapters.boardactivity;

/** 違規樣本（DDD-3，2026-11-17 獨立審查 I-4）：防腐層兼任別的角色；只違反「只實作 Output 與讀取位置儲存」這一條。 */
public final class SideJobTranslator implements Runnable {
    @Override
    public void run() {
    }
}
