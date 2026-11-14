package kanban.fixtures.fatboundary.usecases.commands.dothing;

/** 違規樣本（CA-11 ISP）：一個邊界介面塞了兩個操作。 */
public interface DoThingInput {
    void execute(String request);

    void executeAnother(String request);
}
