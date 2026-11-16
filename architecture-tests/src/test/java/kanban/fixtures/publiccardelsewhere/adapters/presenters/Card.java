package kanban.fixtures.publiccardelsewhere.adapters.presenters;

/** 違規樣本（DDD-4）：用語表標為「聚合內部物件」的名稱（Card），出現在 Entities 圈以外、而且是 public。 */
public record Card(String title) {
}
