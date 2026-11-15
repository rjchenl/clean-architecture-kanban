import { createRouter, createWebHistory } from 'vue-router'
import BoardListView from './views/BoardListView.vue'
import BoardView from './views/BoardView.vue'
import CardHistoryView from './views/CardHistoryView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: BoardListView },
    { path: '/boards/:boardId', component: BoardView, props: true },
    { path: '/boards/:boardId/cards/:cardId/history', component: CardHistoryView, props: true },
  ],
})
