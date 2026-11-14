<script setup lang="ts">
// 卡片移動歷程：時間與描述都是後端整理好的文字，這裡原樣顯示。
import { onMounted, ref } from 'vue'
import * as api from '../api/boardApi'
import { SERVICE_UNAVAILABLE_MESSAGE } from '../api/serviceUnavailable'
import type { CardHistoryViewModel } from '../api/viewModels'
import MessageBar from '../components/MessageBar.vue'

const props = defineProps<{ boardId: string; cardId: string }>()
const history = ref<CardHistoryViewModel | null>(null)
const message = ref('')

onMounted(async () => {
  try {
    history.value = await api.viewCardHistory(props.boardId, props.cardId)
  } catch {
    message.value = SERVICE_UNAVAILABLE_MESSAGE
  }
})
</script>

<template>
  <p><RouterLink :to="`/boards/${boardId}`">← 回看板</RouterLink></p>

  <MessageBar :message="message" />

  <p v-if="history && !history.found">找不到這張卡片的移動歷程。</p>

  <template v-if="history && history.found">
    <h1 data-testid="history-title">{{ history.cardTitle }}</h1>
    <table class="history">
      <thead>
        <tr><th>時間</th><th>發生了什麼</th></tr>
      </thead>
      <tbody>
        <tr v-for="(line, index) in history.lines" :key="index" data-testid="history-line">
          <td>{{ line.occurredAt }}</td>
          <td>{{ line.description }}</td>
        </tr>
      </tbody>
    </table>
  </template>
</template>

<style scoped>
.history { border-collapse: collapse; background: #fff; }
.history th, .history td { border: 1px solid #d5dae0; padding: 6px 12px; text-align: left; }
.history td:first-child { font-variant-numeric: tabular-nums; white-space: nowrap; }
</style>
