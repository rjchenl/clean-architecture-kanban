<script setup lang="ts">
// 前端的 Main 組的看板頁：看板管理的 BoardView，加上通往流程分析的連結。
// 入口放在這裡而不是看板管理的畫面裡：看板管理的畫面不知道流程分析存在（設計第 8 節）。
import { BoardView } from '@kanban/board-management-web-ui'

defineProps<{ boardId: string }>()
</script>

<template>
  <nav class="page-links">
    <RouterLink :to="`/boards/${boardId}/flow`" data-testid="flow-analysis-link">流程分析</RouterLink>
  </nav>
  <BoardView :board-id="boardId" />
</template>
