package kanban.fixtures.createsadapter.adapters.controllers;

import kanban.fixtures.createsadapter.adapters.presenters.SomePresenter;

/** 違規樣本（CA-8）：Controller 自己 new 出 Presenter，不經過 Main。 */
public final class SelfWiringController {
    private final SomePresenter presenter = new SomePresenter();
}
