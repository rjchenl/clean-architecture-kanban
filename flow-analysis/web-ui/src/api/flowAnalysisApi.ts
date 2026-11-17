// 每個函式一對一對應後端流程分析的一個端點。這裡只負責送出請求、把回覆的 ViewModel 原樣交回，不做任何判斷。
import type { CommandViewModel, FlowMetricsViewModel } from './viewModels'
import { ServiceUnavailable } from './serviceUnavailable'

// 只認回覆的「形狀」：有沒有這種 ViewModel 一定會有的欄位。不看內容、不做任何業務判斷。
async function send<T>(method: string, path: string, body: unknown, viewModelField: string): Promise<T> {
  let payload: unknown
  try {
    const response = await fetch(`/api/flow-analysis/boards${path}`, {
      method,
      headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
      body: body === undefined ? undefined : JSON.stringify(body),
    })
    payload = await response.json()
  } catch {
    throw new ServiceUnavailable()
  }
  if (typeof payload !== 'object' || payload === null || !(viewModelField in payload)) {
    throw new ServiceUnavailable()
  }
  return payload as T
}

const command = (method: string, path: string, body?: unknown) =>
  send<CommandViewModel>(method, path, body, 'outcome')

// ---- 查詢（看板流程的識別碼＝看板的識別碼）
export const viewFlowMetrics = (boardId: string) =>
  send<FlowMetricsViewModel>('GET', `/${boardId}`, undefined, 'found')

// ---- 指令
export const setColumnStage = (boardId: string, columnId: string, stage: string) =>
  command('PUT', `/${boardId}/columns/${columnId}/stage`, { stage })
export const setStuckThreshold = (boardId: string, days: number) =>
  command('PUT', `/${boardId}/stuck-threshold`, { days })
export const removeStuckThreshold = (boardId: string) => command('DELETE', `/${boardId}/stuck-threshold`)
