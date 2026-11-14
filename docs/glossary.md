# 用語表（通用語言）

規則 DDD-1：類別、方法、事件、指令的名稱必須與本表一致；**新名詞先進本表再寫程式**。
`architecture-tests` 的 `GlossaryTest` 會檢查：Entities 圈所有公開型別的名稱、指令端所有 `…Request` 去掉 `Request` 後的名稱，都必須出現在「程式名稱」欄。

「程式名稱」欄一格可以放多個名稱，用 `／` 分隔。

## 業務情境：看板管理（board-management）

| 中文用語 | 程式名稱 | 種類 | 意思 |
|---|---|---|---|
| 看板 | Board | 聚合 | 一組有順序的欄位，以及放在欄位裡的卡片 |
| 欄位 | Column | 聚合內部物件 | 看板上的一個工作階段，例如「待辦」「進行中」 |
| 卡片 | Card | 聚合內部物件 | 一件工作 |
| 看板識別碼 | BoardId | 值物件 | 用來指認一個看板 |
| 欄位識別碼 | ColumnId | 值物件 | 用來指認看板裡的一個欄位 |
| 卡片識別碼 | CardId | 值物件 | 用來指認看板裡的一張卡片 |
| 看板名稱 | BoardName | 值物件 | 不可空白，最長 100 字 |
| 欄位名稱 | ColumnName | 值物件 | 不可空白，最長 100 字；同一看板內不可重複 |
| 卡片標題 | CardTitle | 值物件 | 不可空白，最長 100 字 |
| 卡片說明 | CardDescription | 值物件 | 可以空白，最長 2000 字 |
| 在製品上限 | WipLimit | 值物件 | 一欄最多能同時放幾張卡片；正整數；沒設定代表無上限 |
| 位置 | Position | 值物件 | 欄位在看板上、或卡片在欄內排第幾個，從 0 起算 |
| 看板事件 | BoardEvent | 領域事件（共同型別） | 看板上已經發生的一件事 |
| 拒絕原因 | RejectionReason | 列舉 | 指令被拒絕時，違反的是哪一條規則 |
| 違反業務規則 | BusinessRuleViolation | 例外 | 指令違反業務規則而被拒絕 |
| 建立看板 | CreateBoard／BoardCreated | 指令／事件 | 開一個新的看板，一開始沒有任何欄位 |
| 看板改名 | RenameBoard／BoardRenamed | 指令／事件 | 更改看板名稱 |
| 封存看板 | ArchiveBoard／BoardArchived／Archive | 指令／事件 | 看板停止使用，之後不再接受任何變更 |
| 新增欄位 | AddColumn／ColumnAdded | 指令／事件 | 在看板最右邊加一個欄位 |
| 欄位改名 | RenameColumn／ColumnRenamed | 指令／事件 | 更改欄位名稱 |
| 調整欄位順序 | MoveColumn／ColumnMoved | 指令／事件 | 改變欄位的左右順序 |
| 設定在製品上限 | SetColumnWipLimit／ColumnWipLimitSet | 指令／事件 | 設定某一欄最多放幾張卡片 |
| 取消在製品上限 | RemoveColumnWipLimit／ColumnWipLimitRemoved | 指令／事件 | 讓某一欄恢復成無上限 |
| 刪除欄位 | RemoveColumn／ColumnRemoved | 指令／事件 | 拿掉一個欄位；欄內還有卡片時不能刪 |
| 新增卡片 | AddCard／CardAdded | 指令／事件 | 在指定欄位的最下面加一張卡片 |
| 修改卡片內容 | ChangeCardContent／CardContentChanged | 指令／事件 | 更改卡片的標題或說明 |
| 移動卡片 | MoveCard／CardMoved | 指令／事件 | 把卡片換到另一欄；會超過目標欄的在製品上限時被拒絕 |
| 欄內排序 | RepositionCard／CardRepositioned | 指令／事件 | 在同一欄內調整卡片的上下順序 |
| 刪除卡片 | RemoveCard／CardRemoved | 指令／事件 | 把卡片從看板上拿掉 |

## 暫時項目

| 中文用語 | 程式名稱 | 種類 | 意思 |
|---|---|---|---|
| 骨架探針 | EntitiesProbe | 暫時 | Task 1 骨架驗證用，Task 3 連同本列一起移除 |
