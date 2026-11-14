package kanban.fixtures.endpointcallsusecase.frameworks.web;

import kanban.fixtures.endpointcallsusecase.usecases.commands.movecard.MoveCardInput;

/** 違規樣本（CA-7）：HTTP 端點自己呼叫 Use Case，沒有經過 Main 的組裝。 */
public final class SmartEndpoint {
    MoveCardInput moveCard;
}
