<script setup lang="ts">
// 看板現況。這個畫面只做三件事：
// 1. 把後端給的 ViewModel 顯示出來；
// 2. 把使用者的動作送成指令；
// 3. 不論指令成功或被拒絕，都顯示後端給的訊息並重新讀取看板。
// 畫面不先改資料、不判斷操作能不能做（規則 CA-9）。
import { onMounted, ref } from 'vue'
import * as api from '../api/boardApi'
import { SERVICE_UNAVAILABLE_MESSAGE } from '../api/serviceUnavailable'
import type { BoardViewModel, CommandViewModel } from '../api/viewModels'
import ColumnPanel from '../components/ColumnPanel.vue'
import MessageBar from '../components/MessageBar.vue'

const props = defineProps<{ boardId: string }>()

const board = ref<BoardViewModel | null>(null)
const message = ref('')
const newBoardName = ref('')
const newColumnName = ref('')

async function reload() {
  try {
    board.value = await api.viewBoard(props.boardId)
  } catch {
    // 後端沒有給出 ViewModel：顯示系統錯誤訊息，畫面上原本的看板保持不動
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

function renameBoard() {
  run(api.renameBoard(props.boardId, newBoardName.value))
  newBoardName.value = ''
}

function addColumn() {
  run(api.addColumn(props.boardId, newColumnName.value))
  newColumnName.value = ''
}

// 把欄位拖到另一個欄位上：排到那個欄位目前的位置。
function moveColumnOnto(draggedColumnId: string, ontoColumnId: string) {
  const position = board.value?.columns.findIndex((column) => column.columnId === ontoColumnId) ?? 0
  run(api.moveColumn(props.boardId, draggedColumnId, position))
}

onMounted(reload)
</script>

<template>
  <p v-if="board && !board.found">找不到這個看板。<RouterLink to="/">回看板清單</RouterLink></p>

  <div v-if="board && board.found" class="board-heading">
    <h1 data-testid="board-name">{{ board.name }}</h1>
    <span v-if="board.archivedLabel" class="tag" data-testid="archived-label">{{ board.archivedLabel }}</span>
  </div>

  <MessageBar :message="message" />

  <template v-if="board && board.found">
    <div class="board-actions">
      <form class="inline-form" @submit.prevent="renameBoard">
        <input v-model="newBoardName" placeholder="新的看板名稱" aria-label="新的看板名稱" />
        <button type="submit">看板改名</button>
      </form>
      <form class="inline-form" data-testid="add-column" @submit.prevent="addColumn">
        <input v-model="newColumnName" placeholder="新欄位的名稱" aria-label="新欄位的名稱" data-testid="new-column-name" />
        <button type="submit">新增欄位</button>
      </form>
      <button type="button" @click="run(api.archiveBoard(boardId))">封存看板</button>
    </div>
    <p class="muted">拖動卡片可以換欄或排序；拖動欄位標題可以調整欄位順序。</p>

    <div class="columns">
      <ColumnPanel
        v-for="column in board.columns"
        :key="column.columnId"
        :board-id="boardId"
        :column="column"
        @card-dropped="(cardId, columnId, position) => run(api.moveCard(boardId, cardId, columnId, position))"
        @column-dropped="moveColumnOnto"
        @rename-column="(name) => run(api.renameColumn(boardId, column.columnId, name))"
        @set-wip-limit="(limit) => run(api.setColumnWipLimit(boardId, column.columnId, limit))"
        @remove-wip-limit="run(api.removeColumnWipLimit(boardId, column.columnId))"
        @remove-column="run(api.removeColumn(boardId, column.columnId))"
        @add-card="(title, description) => run(api.addCard(boardId, column.columnId, title, description))"
        @change-card="(cardId, title, description) => run(api.changeCardContent(boardId, cardId, title, description))"
        @remove-card="(cardId) => run(api.removeCard(boardId, cardId))"
      />
    </div>
  </template>
</template>

<style scoped>
.board-heading { display: flex; align-items: center; gap: 10px; }
.board-heading h1 { margin: 0; font-size: 22px; }
.board-actions { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
.columns { display: flex; gap: 12px; align-items: flex-start; overflow-x: auto; padding-bottom: 12px; }
</style>
