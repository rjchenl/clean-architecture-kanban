<script setup lang="ts">
// 一張卡片：顯示後端給的標題與說明；編輯與刪除只是把使用者的輸入往上送，不做任何判斷。
import { ref } from 'vue'
import type { CardViewModel } from '../api/viewModels'
import { writePayload } from '../dragPayload'

const props = defineProps<{ boardId: string; card: CardViewModel }>()
const emit = defineEmits<{
  change: [title: string, description: string]
  remove: []
}>()

const editing = ref(false)
const title = ref('')
const description = ref('')

function startEditing() {
  title.value = props.card.title
  description.value = props.card.description
  editing.value = true
}

function save() {
  emit('change', title.value, description.value)
  editing.value = false
}
</script>

<template>
  <article
    class="card"
    draggable="true"
    data-testid="card"
    @dragstart="writePayload($event, { kind: 'card', id: card.cardId })"
  >
    <template v-if="editing">
      <form class="card-form" @submit.prevent="save">
        <input v-model="title" aria-label="卡片標題" />
        <textarea v-model="description" aria-label="卡片說明" rows="3"></textarea>
        <div class="inline-form">
          <button type="submit">儲存</button>
          <button type="button" @click="editing = false">取消</button>
        </div>
      </form>
    </template>
    <template v-else>
      <strong data-testid="card-title">{{ card.title }}</strong>
      <p class="card-description">{{ card.description }}</p>
      <div class="inline-form">
        <button type="button" @click="startEditing">編輯</button>
        <button type="button" @click="emit('remove')">刪除</button>
        <RouterLink :to="`/boards/${boardId}/cards/${card.cardId}/history`">移動歷程</RouterLink>
      </div>
    </template>
  </article>
</template>

<style scoped>
.card { background: #fff; border: 1px solid #d5dae0; border-radius: 6px; padding: 8px; margin-bottom: 8px; cursor: grab; }
.card-description { margin: 4px 0; white-space: pre-wrap; font-size: 14px; }
.card-form { display: flex; flex-direction: column; gap: 6px; }
</style>
