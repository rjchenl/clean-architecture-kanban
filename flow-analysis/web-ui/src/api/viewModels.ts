// 後端流程分析 Presenter 交出的 ViewModel，欄位與 kanban.flowanalysis.adapters.presenters 逐欄對應。
// 全部是可以直接顯示的字串與布林——畫面不做任何格式轉換（規則 CA-9）。

export type CommandOutcome = 'SUCCEEDED' | 'REJECTED' | 'NOT_FOUND' | 'CONFLICT'

export interface CommandViewModel {
  outcome: CommandOutcome
  message: string
  createdId: string
}

export interface StageOptionViewModel {
  value: string
  label: string
}

export interface FlowColumnViewModel {
  flowColumnId: string
  name: string
  stage: string
  stageLabel: string
  workItemCount: string
  averageDwell: string
}

export interface WeekViewModel {
  weekLabel: string
  completedLabel: string
}

export interface StuckWorkItemViewModel {
  title: string
  flowColumnName: string
  since: string
}

export interface FlowMetricsViewModel {
  found: boolean
  boardFlowId: string
  boardName: string
  archivedLabel: string
  hasStuckThreshold: boolean
  stuckThresholdDays: string
  stuckThresholdLabel: string
  stageOptions: StageOptionViewModel[]
  columns: FlowColumnViewModel[]
  deliverySummary: string
  weeks: WeekViewModel[]
  stuckWorkItems: StuckWorkItemViewModel[]
  notFoundMessage: string
}

export interface BoardFlowListViewModel {
  boardFlowIds: string[]
}
