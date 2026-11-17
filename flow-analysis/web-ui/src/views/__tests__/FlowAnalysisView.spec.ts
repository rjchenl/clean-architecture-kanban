import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../../api/flowAnalysisApi'
import { ServiceUnavailable } from '../../api/serviceUnavailable'
import type { CommandViewModel, FlowMetricsViewModel } from '../../api/viewModels'
import FlowAnalysisView from '../FlowAnalysisView.vue'

vi.mock('../../api/flowAnalysisApi')

const flow: FlowMetricsViewModel = {
  found: true,
  boardFlowId: 'board-1',
  boardName: '產品開發',
  archivedLabel: '',
  hasStuckThreshold: true,
  stuckThresholdDays: '5',
  stuckThresholdLabel: '卡住門檻 5 天',
  stageOptions: [
    { value: 'UNCLASSIFIED', label: '未分類' },
    { value: 'NOT_STARTED', label: '未開始' },
    { value: 'IN_PROGRESS', label: '進行中' },
    { value: 'DONE', label: '完成' },
  ],
  columns: [
    { flowColumnId: 'col-todo', name: '待辦', stage: 'NOT_STARTED', stageLabel: '未開始', workItemCount: '2', averageDwell: '—' },
    { flowColumnId: 'col-doing', name: '進行中', stage: 'IN_PROGRESS', stageLabel: '進行中', workItemCount: '1', averageDwell: '3.5 天' },
  ],
  deliverySummary: '最近 8 週：完成 12 張，交付時間平均 4.2 天、最長 9.0 天',
  weeks: [{ weekLabel: '11/09', completedLabel: '2 張' }, { weekLabel: '11/16', completedLabel: '0 張' }],
  stuckWorkItems: [{ title: '串接金流', flowColumnName: '進行中', since: '11/12 09:30' }],
  notFoundMessage: '',
}
const succeeded: CommandViewModel = { outcome: 'SUCCEEDED', message: '', createdId: '' }

async function mountFlow() {
  const wrapper = mount(FlowAnalysisView, { props: { boardId: 'board-1' } })
  await flushPromises()
  return wrapper
}

const texts = (wrapper: Awaited<ReturnType<typeof mountFlow>>, testId: string) =>
  wrapper.findAll(`[data-testid="${testId}"]`).map((element) => element.text())

describe('FlowAnalysisView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(api.viewFlowMetrics).mockResolvedValue(flow)
  })

  it('顯示各欄階段張數與平均停留_原樣顯示ViewModel字串', async () => {
    const wrapper = await mountFlow()

    expect(wrapper.get('[data-testid="board-name"]').text()).toBe('產品開發')
    expect(texts(wrapper, 'column-name')).toEqual(['待辦', '進行中'])
    expect(texts(wrapper, 'stage-label')).toEqual(['未開始', '進行中'])
    expect(texts(wrapper, 'work-item-count')).toEqual(['2', '1'])
    expect(texts(wrapper, 'average-dwell')).toEqual(['—', '3.5 天'])
    expect(wrapper.get('[data-testid="delivery-summary"]').text()).toBe('最近 8 週：完成 12 張，交付時間平均 4.2 天、最長 9.0 天')
    expect(texts(wrapper, 'week-label')).toEqual(['11/09', '11/16'])
    expect(texts(wrapper, 'week-completed')).toEqual(['2 張', '0 張'])
    expect(wrapper.get('[data-testid="stuck-threshold-label"]').text()).toBe('卡住門檻 5 天')
    expect(texts(wrapper, 'stuck-work-item')).toEqual(['串接金流 進行中 11/12 09:30'])
  })

  it('改階段_送出欄位與階段後重讀', async () => {
    vi.mocked(api.setColumnStage).mockResolvedValue(succeeded)
    const wrapper = await mountFlow()

    await wrapper.findAll('[data-testid="stage-select"]')[1].setValue('DONE')
    await flushPromises()

    expect(api.setColumnStage).toHaveBeenCalledWith('board-1', 'col-doing', 'DONE')
    expect(api.viewFlowMetrics).toHaveBeenCalledTimes(2)
  })

  it('儲存門檻_送出天數', async () => {
    vi.mocked(api.setStuckThreshold).mockResolvedValue(succeeded)
    const wrapper = await mountFlow()

    await wrapper.get('[data-testid="threshold-days"]').setValue('7')
    await wrapper.get('[data-testid="threshold-form"]').trigger('submit')
    await flushPromises()

    expect(api.setStuckThreshold).toHaveBeenCalledWith('board-1', 7)
    expect(api.viewFlowMetrics).toHaveBeenCalledTimes(2)
  })

  it('取消門檻', async () => {
    vi.mocked(api.removeStuckThreshold).mockResolvedValue(succeeded)
    const wrapper = await mountFlow()

    await wrapper.get('[data-testid="remove-threshold"]').trigger('click')
    await flushPromises()

    expect(api.removeStuckThreshold).toHaveBeenCalledWith('board-1')
    expect(api.viewFlowMetrics).toHaveBeenCalledTimes(2)
  })

  it('被拒絕_顯示訊息', async () => {
    vi.mocked(api.setStuckThreshold).mockResolvedValue({ outcome: 'REJECTED', message: '卡住門檻要是 1 到 365 天', createdId: '' })
    const wrapper = await mountFlow()

    await wrapper.get('[data-testid="threshold-days"]').setValue('0')
    await wrapper.get('[data-testid="threshold-form"]').trigger('submit')
    await flushPromises()

    expect(wrapper.get('[data-testid="message"]').text()).toBe('卡住門檻要是 1 到 365 天')
    expect(api.viewFlowMetrics).toHaveBeenCalledTimes(2)
  })

  it('找不到_顯示notFoundMessage', async () => {
    vi.mocked(api.viewFlowMetrics).mockResolvedValue({
      ...flow, found: false, boardName: '', columns: [], weeks: [], stuckWorkItems: [],
      notFoundMessage: '流程分析還沒準備好，請稍後重新整理',
    })

    const wrapper = await mountFlow()

    expect(wrapper.get('[data-testid="not-found"]').text()).toBe('流程分析還沒準備好，請稍後重新整理')
    expect(wrapper.find('[data-testid="stage-select"]').exists()).toBe(false)
  })

  it('後端出錯_顯示系統錯誤', async () => {
    vi.mocked(api.viewFlowMetrics).mockRejectedValue(new ServiceUnavailable())

    const wrapper = await mountFlow()

    expect(wrapper.get('[data-testid="message"]').text()).toBe('系統暫時無法處理，請稍後再試')
    expect(wrapper.find('[data-testid="not-found"]').exists()).toBe(false)
  })

  it('沒有設定門檻時_不顯示取消門檻', async () => {
    vi.mocked(api.viewFlowMetrics).mockResolvedValue({
      ...flow, hasStuckThreshold: false, stuckThresholdDays: '', stuckThresholdLabel: '沒有設定卡住門檻',
    })

    const wrapper = await mountFlow()

    expect(wrapper.get('[data-testid="stuck-threshold-label"]').text()).toBe('沒有設定卡住門檻')
    expect(wrapper.find('[data-testid="remove-threshold"]').exists()).toBe(false)
  })
})
