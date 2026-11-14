// 拖放時夾帶的資料：拖的是卡片還是欄位，以及它的識別碼。這是純粹的畫面互動，沒有業務意義。
export interface DragPayload {
  kind: 'card' | 'column'
  id: string
}

const TYPE = 'application/json'

export function writePayload(event: DragEvent, payload: DragPayload): void {
  event.dataTransfer?.setData(TYPE, JSON.stringify(payload))
}

export function readPayload(event: DragEvent): DragPayload | null {
  const raw = event.dataTransfer?.getData(TYPE)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as DragPayload
  } catch {
    return null
  }
}
