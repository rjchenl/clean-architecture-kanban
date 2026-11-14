import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import type { ColumnViewModel } from '../../api/viewModels'
import ColumnPanel from '../ColumnPanel.vue'

const column: ColumnViewModel = {
  columnId: 'col-doing',
  name: '進行中',
  cardCountLabel: '2 / 3',
  cards: [
    { cardId: 'card-1', title: '串接金流', description: '' },
    { cardId: 'card-2', title: '寫架構測試', description: '先寫違規樣本' },
  ],
}

describe('ColumnPanel', () => {
  it('把 cardCountLabel 原樣顯示', () => {
    const wrapper = mount(ColumnPanel, { props: { boardId: 'board-1', column }, global: { stubs: ['RouterLink'] } })

    expect(wrapper.get('[data-testid="card-count"]').text()).toBe('2 / 3')
  })

  it('後端給什麼文字就顯示什麼文字（無上限時只有張數）', () => {
    const wrapper = mount(ColumnPanel, {
      props: { boardId: 'board-1', column: { ...column, cardCountLabel: '2' } },
      global: { stubs: ['RouterLink'] },
    })

    expect(wrapper.get('[data-testid="card-count"]').text()).toBe('2')
  })

  it('依後端給的順序顯示卡片的標題與說明', () => {
    const wrapper = mount(ColumnPanel, { props: { boardId: 'board-1', column }, global: { stubs: ['RouterLink'] } })

    const titles = wrapper.findAll('[data-testid="card-title"]').map((node) => node.text())
    expect(titles).toEqual(['串接金流', '寫架構測試'])
    expect(wrapper.text()).toContain('先寫違規樣本')
  })

  it('把卡片放到某張卡片上：送出「排在其他卡片中的第幾個」', async () => {
    const wrapper = mount(ColumnPanel, { props: { boardId: 'board-1', column }, global: { stubs: ['RouterLink'] } })
    const dataTransfer = { getData: () => JSON.stringify({ kind: 'card', id: 'card-9' }) }

    await wrapper.findAll('[data-testid="card"]')[1].trigger('drop', { dataTransfer })

    expect(wrapper.emitted('card-dropped')).toEqual([['card-9', 'col-doing', 1]])
  })

  it('把卡片放到欄位空白處：排在最後', async () => {
    const wrapper = mount(ColumnPanel, { props: { boardId: 'board-1', column }, global: { stubs: ['RouterLink'] } })
    const dataTransfer = { getData: () => JSON.stringify({ kind: 'card', id: 'card-9' }) }

    await wrapper.get('[data-testid="card-list-end"]').trigger('drop', { dataTransfer })

    expect(wrapper.emitted('card-dropped')).toEqual([['card-9', 'col-doing', 2]])
  })

  it('同一欄內往下拖：位置不把自己算進去', async () => {
    const wrapper = mount(ColumnPanel, { props: { boardId: 'board-1', column }, global: { stubs: ['RouterLink'] } })
    const dataTransfer = { getData: () => JSON.stringify({ kind: 'card', id: 'card-1' }) }

    await wrapper.get('[data-testid="card-list-end"]').trigger('drop', { dataTransfer })

    expect(wrapper.emitted('card-dropped')).toEqual([['card-1', 'col-doing', 1]])
  })
})
