package kanban.boardmanagement.entities.board;

/** 卡片：看板聚合內部的物件，不外露（DDD-4）。 */
record Card(CardId id, CardTitle title, CardDescription description) {
}
