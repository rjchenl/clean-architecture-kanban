import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as api from '../../api/boardApi'
import type { BoardViewModel, CommandViewModel } from '../../api/viewModels'
import ColumnPanel from '../../components/ColumnPanel.vue'
import BoardView from '../BoardView.vue'

vi.mock('../../api/boardApi')

const board: BoardViewModel = {
  found: true,
  boardId: 'board-1',
  name: '產品開發',
  archived: false,
  archivedLabel: '',
  columns: [
    { columnId: 'col-todo', name: '待辦', cardCountLabel: '1', cards: [{ cardId: 'card-1', title: '第一張', description: '' }] },
    { columnId: 'col-doing', name: '進行中', cardCountLabel: '1 / 1', cards: [{ cardId: 'card-2', title: '第二張', description: '' }] },
  ],
}
const succeeded: CommandViewModel = { outcome: 'SUCCEEDED', message: '', createdId: '' }
const rejected: CommandViewModel = { outcome: 'REJECTED', message: '這一欄已達在製品上限，放不進去', createdId: '' }

async function mountBoard() {
  const wrapper = mount(BoardView, { props: { boardId: 'board-1' }, global: { stubs: ['RouterLink'] } })
  await flushPromises()
  return wrapper
}

describe('BoardView', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(api.viewBoard).mockResolvedValue(board)
  })

  it('顯示後端給的看板名稱與欄位', async () => {
    const wrapper = await mountBoard()

    expect(wrapper.get('[data-testid="board-name"]').text()).toBe('產品開發')
    expect(wrapper.findAllComponents(ColumnPanel)).toHaveLength(2)
  })

  it('放下卡片時送出目標欄與位置', async () => {
    vi.mocked(api.moveCard).mockResolvedValue(succeeded)
    const wrapper = await mountBoard()

    wrapper.findAllComponents(ColumnPanel)[1].vm.$emit('card-dropped', 'card-1', 'col-doing', 0)
    await flushPromises()

    expect(api.moveCard).toHaveBeenCalledWith('board-1', 'card-1', 'col-doing', 0)
  })

  it('指令被拒絕時顯示後端給的訊息並重新讀取看板', async () => {
    vi.mocked(api.moveCard).mockResolvedValue(rejected)
    const wrapper = await mountBoard()

    wrapper.findAllComponents(ColumnPanel)[1].vm.$emit('card-dropped', 'card-1', 'col-doing', 0)
    await flushPromises()

    expect(wrapper.get('[data-testid="message"]').text()).toBe('這一欄已達在製品上限，放不進去')
    expect(api.viewBoard).toHaveBeenCalledTimes(2)
  })

  it('指令成功後重新讀取看板，而且沒有訊息', async () => {
    vi.mocked(api.moveCard).mockResolvedValue(succeeded)
    const wrapper = await mountBoard()

    wrapper.findAllComponents(ColumnPanel)[0].vm.$emit('card-dropped', 'card-2', 'col-todo', 1)
    await flushPromises()

    expect(api.viewBoard).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="message"]').exists()).toBe(false)
  })

  it('前端不自己擋操作：已封存的看板照樣送出指令，由後端回覆', async () => {
    vi.mocked(api.viewBoard).mockResolvedValue({ ...board, archived: true, archivedLabel: '已封存' })
    vi.mocked(api.addColumn).mockResolvedValue({ outcome: 'REJECTED', message: '看板已封存，不能再變更', createdId: '' })
    const wrapper = await mountBoard()

    await wrapper.get('[data-testid="new-column-name"]').setValue('完成')
    await wrapper.get('[data-testid="add-column"]').trigger('submit')
    await flushPromises()

    expect(api.addColumn).toHaveBeenCalledWith('board-1', '完成')
    expect(wrapper.get('[data-testid="message"]').text()).toBe('看板已封存，不能再變更')
    expect(wrapper.get('[data-testid="archived-label"]').text()).toBe('已封存')
  })
})
