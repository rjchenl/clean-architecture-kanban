package kanban.fixtures.viewdata.usecases.commands.dothing;

/** 違規樣本（CQRS-2）：指令的 Output Boundary 帶回畫面資料。 */
public interface DoThingOutput {
    void present(BoardView view);
}
