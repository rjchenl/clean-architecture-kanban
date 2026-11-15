package kanban.fixtures.noboundary.usecases.commands.dothing;

/** 違規樣本（CA-4）：只有 Input Boundary，沒有 Output Boundary 也沒有實作。 */
public interface DoThingInput {
    void execute(String request);
}
