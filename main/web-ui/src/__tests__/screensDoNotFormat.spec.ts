// @vitest-environment node
import { ESLint } from 'eslint'
import { describe, expect, it } from 'vitest'

// 規則 CA-9：畫面只顯示後端整理好的 ViewModel，不自己格式化。
// 由專案根目錄 eslint.config.js 的 noFormattingInViews 守；這裡證明它對每個業務情境的畫面都真的會擋。
const repoRoot = decodeURIComponent(new URL('../../../../', import.meta.url).pathname)

async function formattingErrors(code: string, filePath: string) {
  const eslint = new ESLint({ cwd: repoRoot })
  const [result] = await eslint.lintText(code, { filePath: repoRoot + filePath })
  return result.messages
    .map((message) => message.ruleId)
    .filter((ruleId) => ruleId === 'no-restricted-globals' || ruleId === 'no-restricted-properties')
}

const formatsDate = '<script setup lang="ts">\nconst shown = new Date().toLocaleDateString()\n</script>\n<template><p>{{ shown }}</p></template>\n'
const formatsNumber = '<script setup lang="ts">\nconst props = defineProps<{ seconds: number }>()\nconst days = (props.seconds / 86400).toFixed(1)\n</script>\n<template><p>{{ days }}</p></template>\n'

describe('畫面不自己格式化（CA-9）', () => {
  it.each(['board-management', 'flow-analysis'])('%s 的畫面自己格式化日期_被擋下', async (context) => {
    const errors = await formattingErrors(formatsDate, `${context}/web-ui/src/views/Bad.vue`)

    expect(errors).toEqual(expect.arrayContaining(['no-restricted-globals', 'no-restricted-properties']))
  })

  it.each(['board-management', 'flow-analysis'])('%s 的元件自己把秒數換成天數_被擋下', async (context) => {
    const errors = await formattingErrors(formatsNumber, `${context}/web-ui/src/components/Bad.vue`)

    expect(errors).toContain('no-restricted-properties')
  })
})
