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
| 看板動態 | BoardActivity | 對外公布的資料 | 看板管理公布給其他業務情境讀的、已經發生的一件事；只用看板管理的用語，格式只增不改 |
| 看板動態種類 | BoardActivityKind | 列舉 | 看板動態是哪一種事：看板、欄位、卡片的建立、改名、移動、刪除等 |
| 讀取看板動態 | ReadBoardActivities | 查詢 | 讀出某個位置之後的看板動態 |

## 業務情境：流程分析（flow-analysis）

同一件事在兩個情境的名字不同（DDD-2）：看板管理的「看板／欄位／卡片」，在流程分析叫「看板流程／流程欄位／工作項目」。

| 中文用語 | 程式名稱 | 種類 | 意思 |
|---|---|---|---|
| 看板流程 | BoardFlow | 聚合 | 一個看板在流程分析裡的全部紀錄：階段設定、卡住門檻、工作項目的移動與判斷結果 |
| 流程欄位 | FlowColumn | 聚合內部物件 | 看板上的一欄，在流程分析裡多一個階段；刪掉後階段仍保留，用來判斷歷史 |
| 工作項目 | WorkItem | 聚合內部物件 | 看板上的一張卡片，在流程分析裡只看它什麼時候進出哪一欄 |
| 停留紀錄 | ColumnVisit | 聚合內部物件 | 工作項目在什麼時候進入了哪一欄 |
| 流程結果 | FlowResult | 聚合內部物件 | 一個工作項目目前的開始時間、完成時間、交付時間 |
| 看板流程識別碼 | BoardFlowId | 值物件 | 值和看板管理的看板識別碼相同：兩個情境只用識別碼互相參照 |
| 流程欄位識別碼 | FlowColumnId | 值物件 | 用來指認看板流程裡的一個流程欄位 |
| 工作項目識別碼 | WorkItemId | 值物件 | 用來指認看板流程裡的一個工作項目 |
| 階段 | Stage | 列舉 | 未分類、未開始、進行中、完成 |
| 卡住門檻 | StuckThreshold | 值物件 | 在進行中的同一欄待幾天算卡住；1～365 天 |
| 看板動態位置 | ActivityPosition | 值物件 | 一筆看板動態在看板管理公布紀錄裡的位置，從 1 起算；用來認出重送的動態 |
| 工作項目標題 | WorkItemTitle | 值物件 | 顯示用的工作項目標題 |
| 看板流程名稱 | FlowBoardName | 值物件 | 顯示用的看板名稱 |
| 流程欄位名稱 | FlowColumnName | 值物件 | 顯示用的欄位名稱 |
| 流程欄位位置 | FlowColumnPosition | 值物件 | 流程欄位在現有欄位中的位置，從 0 起算 |
| 停留時間 | DwellTime | 值物件 | 在某一欄從進入到離開的時間；被刪除不算離開 |
| 交付時間 | LeadTime | 值物件 | 完成時間－開始時間 |
| 開始時間 | — | 判斷結果 | 第一次進入「進行中」或「完成」欄的時間 |
| 完成時間 | — | 判斷結果 | 最近一次從非完成的欄進入「完成」欄的時間 |
| 卡住 | — | 判斷結果 | 停在進行中的同一欄達到卡住門檻 |
| 看板變化 | BoardChange | 列舉 | 防腐層翻譯過來、看板上發生的是哪一種事（用流程分析的用語） |
| 看板流程事件 | BoardFlowEvent | 領域事件（共同型別） | 看板流程裡已經發生的一件事 |
| 拒絕原因 | RejectionReason | 列舉 | 流程分析的指令被拒絕時，違反的是哪一條規則 |
| 違反業務規則 | BusinessRuleViolation | 例外 | 流程分析的指令違反業務規則而被拒絕 |
| 記錄看板動態 | RecordBoardActivity | 指令 | 把看板上發生的一件事交給看板流程；看板流程還不存在就先建立 |
| 看板流程建立 | BoardFlowCreated | 事件 | 流程分析開始記錄一個看板 |
| 看板流程改名 | BoardFlowRenamed | 事件 | 看板改了名字 |
| 看板流程封存 | BoardFlowArchived | 事件 | 看板封存了；流程數字照常顯示 |
| 流程欄位新增 | FlowColumnAdded | 事件 | 看板加了一欄，階段一律從「未分類」開始 |
| 流程欄位改名 | FlowColumnRenamed | 事件 | 欄位改了名字 |
| 流程欄位調整順序 | FlowColumnMoved | 事件 | 欄位換了左右位置 |
| 流程欄位刪除 | FlowColumnRemoved | 事件 | 欄位被刪掉了；階段仍保留 |
| 工作項目新增 | WorkItemAdded | 事件 | 看板上新增了一張卡片 |
| 工作項目標題修改 | WorkItemTitleChanged | 事件 | 卡片標題改了 |
| 工作項目換欄 | WorkItemMoved | 事件 | 卡片換到另一欄，附在來源欄的停留時間 |
| 工作項目刪除 | WorkItemRemoved | 事件 | 卡片被刪掉了 |
| 指定欄位階段 | SetColumnStage／ColumnStageSet | 指令／事件 | 改一欄的階段，並照新設定重算整段歷史 |
| 設定卡住門檻 | SetStuckThreshold／StuckThresholdSet | 指令／事件 | 設定在進行中的同一欄待幾天算卡住 |
| 取消卡住門檻 | RemoveStuckThreshold／StuckThresholdRemoved | 指令／事件 | 不再判斷卡住 |
| 流程結果更新 | WorkItemFlowResultChanged | 事件 | 工作項目的開始時間、完成時間、交付時間有了新的判斷結果 |
| 卡住狀態改變 | WorkItemStuckStatusChanged | 事件 | 工作項目變成卡住，或不再卡住 |
| 檢查卡住的工作項目 | CheckStuckWorkItems | 指令 | 定時帶入現在的時間，判斷哪些工作項目卡住 |
| 查看流程數字 | ViewFlowMetrics | 查詢 | 一個看板的階段設定、各欄數字、最近 8 週、卡住清單 |
| 看板流程清單 | ListBoardFlows | 查詢 | 列出全部看板流程的識別碼，給定時檢查逐一送出 |
