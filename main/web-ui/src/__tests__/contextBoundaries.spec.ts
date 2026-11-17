// @vitest-environment node
import { ESLint } from 'eslint'
import { describe, expect, it } from 'vitest'

// 設計第 8 節：各業務情境的畫面互不引用，只由前端的 Main（main/web-ui）的路由接起來。
// 由專案根目錄 eslint.config.js 的 no-restricted-imports 守；這裡證明它真的會擋。
const repoRoot = decodeURIComponent(new URL('../../../../', import.meta.url).pathname)

async function restrictedImports(code: string, filePath: string) {
  const eslint = new ESLint({ cwd: repoRoot })
  const [result] = await eslint.lintText(code, { filePath: repoRoot + filePath })
  return result.messages.filter((message) => message.ruleId === 'no-restricted-imports')
}

describe('各業務情境的畫面互不引用', () => {
  it('看板管理的畫面引用流程分析_被擋下', async () => {
    const errors = await restrictedImports(
      "import { FlowAnalysisView } from '@kanban/flow-analysis-web-ui'\nexport const x = FlowAnalysisView\n",
      'board-management/web-ui/src/views/Bad.ts',
    )
    expect(errors).toHaveLength(1)
  })

  it('看板管理的畫面用相對路徑引用流程分析_被擋下', async () => {
    const errors = await restrictedImports(
      "import x from '../../../../flow-analysis/web-ui/src/views/FlowAnalysisView.vue'\nexport const y = x\n",
      'board-management/web-ui/src/views/Bad.ts',
    )
    expect(errors).toHaveLength(1)
  })

  it('流程分析的畫面引用看板管理_被擋下', async () => {
    const errors = await restrictedImports(
      "import { BoardView } from '@kanban/board-management-web-ui'\nexport const x = BoardView\n",
      'flow-analysis/web-ui/src/views/Bad.ts',
    )
    expect(errors).toHaveLength(1)
  })

  it('進入點可以引用兩者', async () => {
    const errors = await restrictedImports(
      "import { BoardView } from '@kanban/board-management-web-ui'\n"
        + "import { FlowAnalysisView } from '@kanban/flow-analysis-web-ui'\n"
        + 'export const x = [BoardView, FlowAnalysisView]\n',
      'main/web-ui/src/router.ts',
    )
    expect(errors).toHaveLength(0)
  })
})
