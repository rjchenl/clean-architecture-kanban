<script setup lang="ts">
// 把後端給的訊息原樣顯示。訊息是空字串時不顯示。
defineProps<{ message: string }>()
</script>

<template>
  <p v-if="message" class="message" role="alert" data-testid="message">{{ message }}</p>
</template>

<style scoped>
.message { background: #fde2e2; border: 1px solid #e59a9a; border-radius: 4px; padding: 8px 12px; margin: 8px 0; }
</style>
