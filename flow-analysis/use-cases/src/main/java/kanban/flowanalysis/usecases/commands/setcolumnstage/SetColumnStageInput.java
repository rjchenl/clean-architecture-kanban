package kanban.flowanalysis.usecases.commands.setcolumnstage;

/** 指定欄位階段：改一欄的階段，並照新設定重算整段歷史（F1、F6）。 */
public interface SetColumnStageInput {

    void execute(SetColumnStageRequest request);
}
