package kanban.boardmanagement.usecases.commands.gateway;

/** 新識別碼的來源。聚合不產生亂數，由 Use Case 取得後傳入（ES-4）。 */
public interface IdentityGenerator {

    String nextId();
}
