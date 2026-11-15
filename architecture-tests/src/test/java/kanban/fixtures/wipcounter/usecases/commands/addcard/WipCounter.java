package kanban.fixtures.wipcounter.usecases.commands.addcard;

/** 違規樣本（CQRS-4）：在指令 Use Case 的套件裡偷放一個 Gateway，讓外圈用讀取模型數張數。 */
public interface WipCounter {
    int cardsIn(String columnId);
}
