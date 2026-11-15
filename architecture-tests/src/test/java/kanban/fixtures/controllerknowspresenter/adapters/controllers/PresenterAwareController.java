package kanban.fixtures.controllerknowspresenter.adapters.controllers;

import kanban.fixtures.controllerknowspresenter.adapters.presenters.SomePresenter;

/** 違規樣本（CA-8）：Controller 認識 Presenter；兩者應該只由 Main 接在一起。 */
public final class PresenterAwareController {
    SomePresenter presenter;
}
