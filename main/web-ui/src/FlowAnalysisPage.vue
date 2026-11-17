<script setup lang="ts">
// 前端的 Main 組的流程分析頁：流程分析的 FlowAnalysisView，加上回到看板的連結（設計第 8 節）。
import { FlowAnalysisView } from '@kanban/flow-analysis-web-ui'

defineProps<{ boardId: string }>()
</script>

<template>
  <nav class="page-links">
    <RouterLink :to="`/boards/${boardId}`" data-testid="back-to-board">回到看板</RouterLink>
  </nav>
  <FlowAnalysisView :board-id="boardId" />
</template>
