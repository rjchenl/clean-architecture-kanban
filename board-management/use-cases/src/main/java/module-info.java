/** 第 2 圈 Use Cases：只 requires Entities（CA-3）；實作類別所在的 impl、support 套件不公開（CA-16）。 */
module kanban.boardmanagement.usecases {
    requires transitive kanban.boardmanagement.entities;

    exports kanban.boardmanagement.usecases.commands;
    exports kanban.boardmanagement.usecases.commands.gateway;
    exports kanban.boardmanagement.usecases.commands.createboard;
    exports kanban.boardmanagement.usecases.commands.renameboard;
    exports kanban.boardmanagement.usecases.commands.archiveboard;
    exports kanban.boardmanagement.usecases.commands.addcolumn;
    exports kanban.boardmanagement.usecases.commands.renamecolumn;
    exports kanban.boardmanagement.usecases.commands.movecolumn;
    exports kanban.boardmanagement.usecases.commands.setcolumnwiplimit;
    exports kanban.boardmanagement.usecases.commands.removecolumnwiplimit;
    exports kanban.boardmanagement.usecases.commands.removecolumn;
    exports kanban.boardmanagement.usecases.commands.addcard;
    exports kanban.boardmanagement.usecases.commands.changecardcontent;
    exports kanban.boardmanagement.usecases.commands.movecard;
    exports kanban.boardmanagement.usecases.commands.repositioncard;
    exports kanban.boardmanagement.usecases.commands.removecard;
    exports kanban.boardmanagement.usecases.eventfeed;
}
