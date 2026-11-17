import { afterEach, describe, expect, it, vi } from 'vitest'
import { removeStuckThreshold, setColumnStage, setStuckThreshold, viewFlowMetrics } from '../flowAnalysisApi'
import { ServiceUnavailable } from '../serviceUnavailable'

function respondWith(status: number, body: string) {
  const fetch = vi.fn().mockResolvedValue(new Response(body, { status }))
  vi.stubGlobal('fetch', fetch)
  return fetch
}

const notFound = {
  found: false, boardFlowId: '', boardName: '', archivedLabel: '', hasStuckThreshold: false, stuckThresholdDays: '',
  stuckThresholdLabel: '', stageOptions: [], columns: [], deliverySummary: '', weeks: [], stuckWorkItems: [],
  notFoundMessage: '流程分析還沒準備好，請稍後重新整理',
}

describe('flowAnalysisApi', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('後端回覆的 ViewModel 原樣交回（包含被拒絕的指令）', async () => {
    const fetch = respondWith(422, JSON.stringify({ outcome: 'REJECTED', message: '卡住門檻要是 1 到 365 天', createdId: '' }))

    expect(await setStuckThreshold('board-1', 0)).toEqual({ outcome: 'REJECTED', message: '卡住門檻要是 1 到 365 天', createdId: '' })
    expect(fetch).toHaveBeenCalledWith('/api/flow-analysis/boards/board-1/stuck-threshold',
      expect.objectContaining({ method: 'PUT', body: JSON.stringify({ days: 0 }) }))
  })

  it('查詢找不到時，原樣交回 found 為 false 的 ViewModel', async () => {
    respondWith(404, JSON.stringify(notFound))

    expect(await viewFlowMetrics('board-1')).toEqual(notFound)
  })

  it('後端出錯（回覆不是 ViewModel）：丟出 ServiceUnavailable，不當成「找不到」', async () => {
    respondWith(500, JSON.stringify({ timestamp: '2026-11-17T03:00:00Z', status: 500, error: 'Internal Server Error' }))

    await expect(viewFlowMetrics('board-1')).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('指令遇到後端出錯：丟出 ServiceUnavailable', async () => {
    respondWith(500, JSON.stringify({ status: 500, error: 'Internal Server Error' }))

    await expect(setColumnStage('board-1', 'col-1', 'DONE')).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('回覆不是 JSON（例如代理伺服器的錯誤頁）：丟出 ServiceUnavailable', async () => {
    respondWith(502, '<html>Bad Gateway</html>')

    await expect(removeStuckThreshold('board-1')).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('連不上後端：丟出 ServiceUnavailable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(viewFlowMetrics('board-1')).rejects.toBeInstanceOf(ServiceUnavailable)
  })
})
