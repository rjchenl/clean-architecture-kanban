<script setup lang="ts">
// 流程分析頁。這個畫面只做三件事：
// 1. 把後端給的 ViewModel 顯示出來；
// 2. 把使用者的動作（改階段、設定或取消卡住門檻）送成指令；
// 3. 不論指令成功或被拒絕，都顯示後端給的訊息並重新讀取。
// 畫面不先改資料、不判斷操作能不能做、不做格式轉換（規則 CA-9）。
import { onMounted, ref } from 'vue'
import * as api from '../api/flowAnalysisApi'
import { SERVICE_UNAVAILABLE_MESSAGE } from '../api/serviceUnavailable'
import type { CommandViewModel, FlowMetricsViewModel } from '../api/viewModels'
import StageTable from '../components/StageTable.vue'
import StuckList from '../components/StuckList.vue'

const props = defineProps<{ boardId: string }>()

const flow = ref<FlowMetricsViewModel | null>(null)
const message = ref('')
const newDays = ref<number | ''>('')

async function reload() {
  try {
    flow.value = await api.viewFlowMetrics(props.boardId)
  } catch {
    // 後端沒有給出 ViewModel：顯示系統錯誤訊息，畫面上原本的數字保持不動
    message.value = SERVICE_UNAVAILABLE_MESSAGE
  }
}

async function run(command: Promise<CommandViewModel>) {
  try {
    const result = await command
    message.value = result.message
  } catch {
    message.value = SERVICE_UNAVAILABLE_MESSAGE
    return
  }
  await reload()
}

function saveThreshold() {
  run(api.setStuckThreshold(props.boardId, newDays.value as number))
  newDays.value = ''
}

onMounted(reload)
</script>

<template>
  <p v-if="flow && !flow.found" class="muted" data-testid="not-found">{{ flow.notFoundMessage }}</p>

  <div v-if="flow && flow.found" class="flow-heading">
    <h1 data-testid="board-name">{{ flow.boardName }}</h1>
    <span class="muted">流程分析</span>
    <span v-if="flow.archivedLabel" class="tag" data-testid="archived-label">{{ flow.archivedLabel }}</span>
  </div>

  <p v-if="message" class="message" role="alert" data-testid="message">{{ message }}</p>

  <template v-if="flow && flow.found">
    <section>
      <h2>各欄的階段</h2>
      <StageTable
        :columns="flow.columns"
        :stage-options="flow.stageOptions"
        @set-stage="(flowColumnId, stage) => run(api.setColumnStage(boardId, flowColumnId, stage))"
      />
    </section>

    <section>
      <h2>交付</h2>
      <p data-testid="delivery-summary">{{ flow.deliverySummary }}</p>
      <table class="weeks">
        <thead><tr><th>週</th><th>完成</th></tr></thead>
        <tbody>
          <tr v-for="week in flow.weeks" :key="week.weekLabel" data-testid="week">
            <td data-testid="week-label">{{ week.weekLabel }}</td>
            <td data-testid="week-completed">{{ week.completedLabel }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section>
      <h2>卡住的工作項目</h2>
      <p data-testid="stuck-threshold-label">{{ flow.stuckThresholdLabel }}</p>
      <form class="inline-form" data-testid="threshold-form" @submit.prevent="saveThreshold">
        <input v-model.number="newDays" type="number" placeholder="天數" aria-label="卡住門檻（天）" data-testid="threshold-days" />
        <button type="submit">儲存門檻</button>
        <button v-if="flow.hasStuckThreshold" type="button" data-testid="remove-threshold" @click="run(api.removeStuckThreshold(boardId))">取消門檻</button>
      </form>
      <StuckList :items="flow.stuckWorkItems" />
    </section>
  </template>
</template>

<style scoped>
.flow-heading { display: flex; align-items: center; gap: 10px; }
.flow-heading h1 { margin: 0; font-size: 22px; }
h2 { font-size: 16px; margin: 18px 0 6px; }
.message { background: #fde2e2; border: 1px solid #e59a9a; border-radius: 4px; padding: 8px 12px; margin: 8px 0; }
.weeks { border-collapse: collapse; background: #fff; }
.weeks th, .weeks td { border: 1px solid #dde1e6; padding: 2px 10px; }
</style>
