package kanban.fixtures.godpresenter.adapters.presenters;

import kanban.fixtures.godpresenter.usecases.commands.doother.DoOtherOutput;
import kanban.fixtures.godpresenter.usecases.commands.dothing.DoThingOutput;

/** 違規樣本（CA-11 OCP）：一個 Presenter 包辦多個 Use Case 的 Output Boundary，新增 Use Case 就得改它。 */
public final class EverythingPresenter implements DoThingOutput, DoOtherOutput {
}
