<script setup lang="ts">
// 看板清單：顯示後端給的清單；建立看板後顯示後端給的訊息並重新讀取清單。
import { onMounted, ref } from 'vue'
import * as api from '../api/boardApi'
import type { BoardListViewModel } from '../api/viewModels'
import MessageBar from '../components/MessageBar.vue'

const list = ref<BoardListViewModel>({ boards: [] })
const message = ref('')
const newBoardName = ref('')

async function reload() {
  list.value = await api.listBoards()
}

async function createBoard() {
  const result = await api.createBoard(newBoardName.value)
  message.value = result.message
  newBoardName.value = ''
  await reload()
}

onMounted(reload)
</script>

<template>
  <h1>看板清單</h1>
  <MessageBar :message="message" />

  <form class="inline-form" data-testid="create-board" @submit.prevent="createBoard">
    <input v-model="newBoardName" placeholder="新看板的名稱" aria-label="新看板的名稱" data-testid="new-board-name" />
    <button type="submit">建立看板</button>
  </form>

  <ul class="boards">
    <li v-for="item in list.boards" :key="item.boardId" data-testid="board-item">
      <RouterLink :to="`/boards/${item.boardId}`">{{ item.name }}</RouterLink>
      <span v-if="item.archivedLabel" class="tag">{{ item.archivedLabel }}</span>
    </li>
  </ul>
</template>

<style scoped>
.boards { padding-left: 20px; line-height: 1.9; }
.boards .tag { margin-left: 8px; }
</style>
