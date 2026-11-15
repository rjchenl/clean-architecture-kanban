package kanban.fixtures.controllerusesgateway.adapters.controllers;

import kanban.fixtures.controllerusesgateway.usecases.commands.gateway.BoardRepository;

/** 違規樣本（CA-11 DIP）：Controller 跳過 Use Case，直接用 Repository。 */
public final class ShortcutController {
    BoardRepository boards;
}
