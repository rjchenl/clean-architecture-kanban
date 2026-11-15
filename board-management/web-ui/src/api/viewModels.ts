// 與後端 Presenter 產生的 ViewModel 同形（規則 CA-9）：只有字串、布林、列舉與它們的清單。
// 畫面拿到什麼就顯示什麼，不需要再轉換。

export type CommandOutcome = 'SUCCEEDED' | 'REJECTED' | 'NOT_FOUND' | 'CONFLICT'

export interface CommandViewModel {
  outcome: CommandOutcome
  message: string
  createdId: string
}

export interface CardViewModel {
  cardId: string
  title: string
  description: string
}

export interface ColumnViewModel {
  columnId: string
  name: string
  cardCountLabel: string
  cards: CardViewModel[]
}

export interface BoardViewModel {
  found: boolean
  boardId: string
  name: string
  archived: boolean
  archivedLabel: string
  columns: ColumnViewModel[]
}

export interface BoardListItemViewModel {
  boardId: string
  name: string
  archived: boolean
  archivedLabel: string
}

export interface BoardListViewModel {
  boards: BoardListItemViewModel[]
}

export interface CardHistoryLineViewModel {
  occurredAt: string
  description: string
}

export interface CardHistoryViewModel {
  found: boolean
  cardTitle: string
  lines: CardHistoryLineViewModel[]
}
