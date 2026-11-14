// 每個函式一對一對應後端的一個端點。這裡只負責送出請求、把回覆的 ViewModel 原樣交回，不做任何判斷。
import type {
  BoardListViewModel,
  BoardViewModel,
  CardHistoryViewModel,
  CommandViewModel,
} from './viewModels'

async function send<T>(method: string, path: string, body?: unknown): Promise<T> {
  const response = await fetch(`/api/boards${path}`, {
    method,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  return (await response.json()) as T
}

const command = (method: string, path: string, body?: unknown) => send<CommandViewModel>(method, path, body)

// ---- 查詢
export const listBoards = () => send<BoardListViewModel>('GET', '')
export const viewBoard = (boardId: string) => send<BoardViewModel>('GET', `/${boardId}`)
export const viewCardHistory = (boardId: string, cardId: string) =>
  send<CardHistoryViewModel>('GET', `/${boardId}/cards/${cardId}/history`)

// ---- 看板
export const createBoard = (name: string) => command('POST', '', { name })
export const renameBoard = (boardId: string, name: string) => command('PUT', `/${boardId}/name`, { name })
export const archiveBoard = (boardId: string) => command('POST', `/${boardId}/archive`)

// ---- 欄位
export const addColumn = (boardId: string, name: string) => command('POST', `/${boardId}/columns`, { name })
export const renameColumn = (boardId: string, columnId: string, name: string) =>
  command('PUT', `/${boardId}/columns/${columnId}/name`, { name })
export const moveColumn = (boardId: string, columnId: string, position: number) =>
  command('PUT', `/${boardId}/columns/${columnId}/position`, { position })
export const setColumnWipLimit = (boardId: string, columnId: string, limit: number) =>
  command('PUT', `/${boardId}/columns/${columnId}/wip-limit`, { limit })
export const removeColumnWipLimit = (boardId: string, columnId: string) =>
  command('DELETE', `/${boardId}/columns/${columnId}/wip-limit`)
export const removeColumn = (boardId: string, columnId: string) =>
  command('DELETE', `/${boardId}/columns/${columnId}`)

// ---- 卡片
export const addCard = (boardId: string, columnId: string, title: string, description: string) =>
  command('POST', `/${boardId}/cards`, { columnId, title, description })
export const changeCardContent = (boardId: string, cardId: string, title: string, description: string) =>
  command('PUT', `/${boardId}/cards/${cardId}/content`, { title, description })
export const moveCard = (boardId: string, cardId: string, columnId: string, position: number) =>
  command('PUT', `/${boardId}/cards/${cardId}/column`, { columnId, position })
export const repositionCard = (boardId: string, cardId: string, position: number) =>
  command('PUT', `/${boardId}/cards/${cardId}/position`, { position })
export const removeCard = (boardId: string, cardId: string) => command('DELETE', `/${boardId}/cards/${cardId}`)
