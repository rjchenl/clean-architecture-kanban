import { afterEach, describe, expect, it, vi } from 'vitest'
import { createBoard, listBoards, viewBoard } from '../boardApi'
import { ServiceUnavailable } from '../serviceUnavailable'

function respondWith(status: number, body: string) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { status })))
}

describe('boardApi', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('後端回覆的 ViewModel 原樣交回（包含被拒絕的指令）', async () => {
    respondWith(422, JSON.stringify({ outcome: 'REJECTED', message: '名稱不可空白', createdId: '' }))

    expect(await createBoard(' ')).toEqual({ outcome: 'REJECTED', message: '名稱不可空白', createdId: '' })
  })

  it('查詢找不到時，原樣交回 found 為 false 的 ViewModel', async () => {
    respondWith(404, JSON.stringify({ found: false, boardId: '', name: '', archived: false, archivedLabel: '', columns: [] }))

    expect((await viewBoard('nope')).found).toBe(false)
  })

  it('後端出錯（回覆不是 ViewModel）：丟出 ServiceUnavailable，不當成「找不到」', async () => {
    respondWith(500, JSON.stringify({ timestamp: '2026-11-14T03:00:00Z', status: 500, error: 'Internal Server Error' }))

    await expect(viewBoard('board-1')).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('指令遇到後端出錯：丟出 ServiceUnavailable', async () => {
    respondWith(500, JSON.stringify({ status: 500, error: 'Internal Server Error' }))

    await expect(createBoard('看板')).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('回覆不是 JSON（例如代理伺服器的錯誤頁）：丟出 ServiceUnavailable', async () => {
    respondWith(502, '<html>Bad Gateway</html>')

    await expect(listBoards()).rejects.toBeInstanceOf(ServiceUnavailable)
  })

  it('連不上後端：丟出 ServiceUnavailable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    await expect(listBoards()).rejects.toBeInstanceOf(ServiceUnavailable)
  })
})
