<script setup lang="ts">
// 各欄的階段、張數、平均停留：原樣顯示 ViewModel 的字串。
// 改階段時把欄位與選到的階段交給上層送出；能不能改、改了算什麼，都由後端決定（規則 CA-9）。
import type { FlowColumnViewModel, StageOptionViewModel } from '../api/viewModels'

defineProps<{ columns: FlowColumnViewModel[]; stageOptions: StageOptionViewModel[] }>()
const emit = defineEmits<{ 'set-stage': [flowColumnId: string, stage: string] }>()

function choose(flowColumnId: string, event: Event) {
  const select = event.target as HTMLSelectElement
  emit('set-stage', flowColumnId, select.value)
  // 選單回到「改成…」：畫面上的階段以重讀後的 ViewModel 為準
  select.value = ''
}
</script>

<template>
  <table class="stage-table" data-testid="stage-table">
    <thead>
      <tr><th>欄位</th><th>階段</th><th>張數</th><th>平均停留</th><th>改階段</th></tr>
    </thead>
    <tbody>
      <tr v-for="column in columns" :key="column.flowColumnId" data-testid="flow-column">
        <td data-testid="column-name">{{ column.name }}</td>
        <td data-testid="stage-label">{{ column.stageLabel }}</td>
        <td data-testid="work-item-count">{{ column.workItemCount }}</td>
        <td data-testid="average-dwell">{{ column.averageDwell }}</td>
        <td>
          <select :aria-label="column.name" data-testid="stage-select" @change="choose(column.flowColumnId, $event)">
            <option value="" disabled selected>改成…</option>
            <option v-for="option in stageOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </td>
      </tr>
    </tbody>
  </table>
</template>

<style scoped>
.stage-table { border-collapse: collapse; background: #fff; }
.stage-table th, .stage-table td { border: 1px solid #dde1e6; padding: 4px 10px; text-align: left; }
.stage-table th { background: #eef1f4; font-weight: 600; }
</style>
