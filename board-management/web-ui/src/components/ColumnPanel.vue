<script setup lang="ts">
// 一個欄位：顯示後端給的名稱、數量文字與卡片。
// 這裡不比較張數與上限、不判斷能不能放——放下卡片時只回報「放到哪一欄、排在其他卡片中的第幾個」。
import { ref } from 'vue'
import type { ColumnViewModel } from '../api/viewModels'
import { readPayload, writePayload } from '../dragPayload'
import CardItem from './CardItem.vue'

const props = defineProps<{ boardId: string; column: ColumnViewModel }>()
const emit = defineEmits<{
  'card-dropped': [cardId: string, columnId: string, position: number]
  'column-dropped': [draggedColumnId: string, ontoColumnId: string]
  'rename-column': [name: string]
  'set-wip-limit': [limit: number]
  'remove-wip-limit': []
  'remove-column': []
  'add-card': [title: string, description: string]
  'change-card': [cardId: string, title: string, description: string]
  'remove-card': [cardId: string]
}>()

const newName = ref('')
const newLimit = ref<number | ''>('')
const newCardTitle = ref('')
const newCardDescription = ref('')

// 放下的位置＝排在「其他卡片」中的第幾個（被拖的那張自己不算）。純粹是畫面上的位置，不是業務規則。
function positionBefore(cardIndex: number, draggedCardId: string): number {
  return props.column.cards.slice(0, cardIndex).filter((card) => card.cardId !== draggedCardId).length
}

function dropCard(event: DragEvent, cardIndex: number) {
  const payload = readPayload(event)
  if (payload?.kind !== 'card') {
    return
  }
  event.stopPropagation()
  emit('card-dropped', payload.id, props.column.columnId, positionBefore(cardIndex, payload.id))
}

function dropOnColumn(event: DragEvent) {
  const payload = readPayload(event)
  if (payload?.kind === 'column') {
    emit('column-dropped', payload.id, props.column.columnId)
  }
}

function renameColumn() {
  emit('rename-column', newName.value)
  newName.value = ''
}

function setWipLimit() {
  emit('set-wip-limit', newLimit.value as number)
  newLimit.value = ''
}

function addCard() {
  emit('add-card', newCardTitle.value, newCardDescription.value)
  newCardTitle.value = ''
  newCardDescription.value = ''
}
</script>

<template>
  <section class="column" data-testid="column" @dragover.prevent @drop="dropOnColumn">
    <header
      class="column-header"
      draggable="true"
      @dragstart="writePayload($event, { kind: 'column', id: column.columnId })"
    >
      <h2 data-testid="column-name">{{ column.name }}</h2>
      <span class="count" data-testid="card-count">{{ column.cardCountLabel }}</span>
    </header>

    <details class="column-settings">
      <summary>欄位設定</summary>
      <form class="inline-form" @submit.prevent="renameColumn">
        <input v-model="newName" placeholder="新的欄位名稱" aria-label="新的欄位名稱" />
        <button type="submit">改名</button>
      </form>
      <form class="inline-form" @submit.prevent="setWipLimit">
        <input v-model.number="newLimit" type="number" placeholder="在製品上限" aria-label="在製品上限" />
        <button type="submit">設定上限</button>
        <button type="button" @click="emit('remove-wip-limit')">取消上限</button>
      </form>
      <button type="button" @click="emit('remove-column')">刪除欄位</button>
    </details>

    <div class="cards">
      <div
        v-for="(card, index) in column.cards"
        :key="card.cardId"
        @dragover.prevent
        @drop="dropCard($event, index)"
      >
        <CardItem
          :board-id="boardId"
          :card="card"
          @change="(title, description) => emit('change-card', card.cardId, title, description)"
          @remove="emit('remove-card', card.cardId)"
        />
      </div>
      <div
        class="card-list-end"
        data-testid="card-list-end"
        @dragover.prevent
        @drop="dropCard($event, column.cards.length)"
      >
        拖到這裡排在最後
      </div>
    </div>

    <form class="new-card" @submit.prevent="addCard">
      <input v-model="newCardTitle" placeholder="新卡片的標題" aria-label="新卡片的標題" />
      <textarea v-model="newCardDescription" placeholder="說明（可不填）" aria-label="新卡片的說明" rows="2"></textarea>
      <button type="submit">新增卡片</button>
    </form>
  </section>
</template>

<style scoped>
.column { background: #e9ecf0; border-radius: 8px; padding: 10px; width: 280px; flex: 0 0 280px; }
.column-header { display: flex; justify-content: space-between; align-items: baseline; cursor: grab; }
.column-header h2 { font-size: 16px; margin: 0 0 6px; }
.count { font-variant-numeric: tabular-nums; color: #4b5560; }
.column-settings { font-size: 13px; margin-bottom: 8px; }
.column-settings input { width: 110px; }
.card-list-end { border: 1px dashed #b2bac3; border-radius: 6px; padding: 8px; text-align: center; color: #7b848d; font-size: 13px; margin-bottom: 8px; }
.new-card { display: flex; flex-direction: column; gap: 6px; }
</style>
