/**
 * 後端沒有給出 ViewModel：連不上、回覆不是 JSON，或回覆的是系統錯誤而不是整理好的結果。
 * 這是系統狀況，不是業務結果——畫面要顯示系統錯誤訊息，不可以把它當成「還沒準備好」或「被拒絕」。
 * 和看板管理的同名檔案各自一份：各業務情境的畫面互不引用（設計第 8 節）。
 */
export class ServiceUnavailable extends Error {
  constructor() {
    super('service unavailable')
    this.name = 'ServiceUnavailable'
  }
}

/** 畫面在後端沒有給出 ViewModel 時顯示的訊息。 */
export const SERVICE_UNAVAILABLE_MESSAGE = '系統暫時無法處理，請稍後再試'
