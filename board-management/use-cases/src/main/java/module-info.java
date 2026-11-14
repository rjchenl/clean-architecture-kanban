/** 第 2 圈 Use Cases：只 requires Entities（CA-3）；實作類別所在的 impl 套件不公開（CA-16）。 */
module kanban.boardmanagement.usecases {
    requires transitive kanban.boardmanagement.entities;

    exports kanban.boardmanagement.usecases.probe;
}
