<script setup lang="ts">
// 卡住的工作項目：標題、在哪一欄、從什麼時候開始卡住，都原樣顯示 ViewModel 的字串（規則 CA-9）。
import type { StuckWorkItemViewModel } from '../api/viewModels'

defineProps<{ items: StuckWorkItemViewModel[] }>()
</script>

<template>
  <ul class="stuck-list" data-testid="stuck-list">
    <li v-for="item in items" :key="item.title + item.since" data-testid="stuck-work-item">
      <strong>{{ item.title }}</strong> <span>{{ item.flowColumnName }}</span> <span class="muted">{{ item.since }}</span>
    </li>
  </ul>
</template>

<style scoped>
.stuck-list { margin: 6px 0; padding-left: 20px; }
.stuck-list li { margin: 2px 0; }
</style>
