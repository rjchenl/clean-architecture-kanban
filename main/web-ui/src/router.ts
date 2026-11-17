import { BoardListView, CardHistoryView } from '@kanban/board-management-web-ui'
import { createRouter, createWebHistory } from 'vue-router'
import BoardPage from './BoardPage.vue'
import FlowAnalysisPage from './FlowAnalysisPage.vue'

// 前端的 Main：各業務情境的畫面只在這裡接起來，彼此互不引用（設計第 8 節）
export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: BoardListView },
    { path: '/boards/:boardId', component: BoardPage, props: true },
    { path: '/boards/:boardId/flow', component: FlowAnalysisPage, props: true },
    { path: '/boards/:boardId/cards/:cardId/history', component: CardHistoryView, props: true },
  ],
})
