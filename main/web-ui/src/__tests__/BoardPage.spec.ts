import { mount, RouterLinkStub } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import BoardPage from '../BoardPage.vue'
import FlowAnalysisPage from '../FlowAnalysisPage.vue'

// 看板頁的「流程分析」入口由前端的 Main 提供：看板管理的畫面元件不知道流程分析存在（設計第 8 節）
describe('BoardPage', () => {
  it('看板頁有流程分析連結_指向該看板', () => {
    const wrapper = mount(BoardPage, {
      props: { boardId: 'board-1' },
      global: { stubs: { BoardView: true, RouterLink: RouterLinkStub } },
    })

    const link = wrapper.getComponent(RouterLinkStub)
    expect(link.text()).toBe('流程分析')
    expect(link.props('to')).toBe('/boards/board-1/flow')
  })

  it('流程分析頁有回到看板的連結', () => {
    const wrapper = mount(FlowAnalysisPage, {
      props: { boardId: 'board-1' },
      global: { stubs: { FlowAnalysisView: true, RouterLink: RouterLinkStub } },
    })

    const link = wrapper.getComponent(RouterLinkStub)
    expect(link.text()).toBe('回到看板')
    expect(link.props('to')).toBe('/boards/board-1')
  })
})
