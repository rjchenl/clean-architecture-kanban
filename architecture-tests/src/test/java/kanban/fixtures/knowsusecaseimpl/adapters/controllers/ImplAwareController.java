package kanban.fixtures.knowsusecaseimpl.adapters.controllers;

import kanban.fixtures.knowsusecaseimpl.usecases.commands.dothing.impl.DoThingUseCase;

/** 違規樣本（CA-8）：Controller 直接認識 Use Case 的實作類別。 */
public final class ImplAwareController {
    DoThingUseCase useCase;
}
