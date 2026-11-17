# clean-architecture-kanban

用一個看板系統（Kanban Board），完整實踐下列四套方法：

- Robert C. Martin《Clean Architecture》（無瑕的程式碼：整潔的軟體設計與架構篇）
- Domain-Driven Design（DDD）
- CQRS（指令與查詢責任分離）
- Event Sourcing（事件溯源）

## 目標

這是一個示範作品。功能本身不是重點，重點是四套方法都真的在程式裡起作用，而且每一條原則都有自動檢查守住，讀者可以逐條查核。

## 四套方法各管什麼

| 方法 | 它管什麼 | 在看板系統裡的樣子 |
|---|---|---|
| Clean Architecture | 程式碼誰可以依賴誰 | 相依方向只能由外向內；框架、資料庫、前端都在最外圈 |
| DDD | 最內圈的業務怎麼建模 | 聚合守住業務規則（在製品上限、卡片移動規則等）；程式用語與業務用語表一致 |
| Event Sourcing | 寫入端怎麼存資料 | 只存發生過的事件，現況由重播事件算出 |
| CQRS | 寫入和查詢怎麼分開 | 寫入端處理指令並產生事件；查詢端由事件推算出給畫面看的資料 |

## 技術選型

- 後端：Java + Spring Boot（Spring 只出現在最外圈）
- 前端：Vue（只當很薄的畫面，顯示後端整理好的 ViewModel）

## 狀態

- 第一階段：業務情境「看板管理」（看板、欄位、卡片、在製品上限、卡片移動歷程）。
- 第二階段：第二個業務情境「流程分析」（每一欄算哪個階段、交付時間、每週完成數、卡住的工作項目），由看板管理公布的「看板動態」經防腐層接過來，實作了跨情境整合（DDD-3）。

每一條規則目前到哪裡，以 [`docs/traceability.md`](docs/traceability.md) 為準——那張表每個規則編號一列，寫明落在哪個模組、由哪一支測試或檢查守住、目前狀態。

## 文件

| 文件 | 內容 |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | 專案規範：四套方法共 39 條規則，每條附驗證方式 |
| [`docs/design/phase-one-board-management.md`](docs/design/phase-one-board-management.md) | 第一階段設計：範圍、做法的取捨、業務規則、一致性說明 |
| [`docs/design/phase-two-flow-analysis.md`](docs/design/phase-two-flow-analysis.md) | 第二階段設計：流程分析的業務規則、兩個情境怎麼接、統計方式、一致性說明 |
| [`docs/plans/phase-two-flow-analysis-plan.md`](docs/plans/phase-two-flow-analysis-plan.md) | 第二階段實作計畫：逐步的工作包與新增的架構規則 |
| [`docs/traceability.md`](docs/traceability.md) | 規則對照表 |
| [`docs/glossary.md`](docs/glossary.md) | 業務用語表（程式裡的名稱必須出現在這裡） |
| [`docs/context-map.md`](docs/context-map.md) | 業務情境之間的關係 |
| [`docs/review-checklist.md`](docs/review-checklist.md) | 只能靠人審查的規則與每次審查紀錄 |

## 模組

```
board-management/            業務情境：看板管理
├── entities/                第 1 圈：看板聚合、值物件、領域事件（純 Java，零相依）
├── use-cases/               第 2 圈：14 個指令與 3 個查詢（只相依 entities）
├── interface-adapters/      第 3 圈：Controller、Presenter、事件庫與讀取模型的轉接程式、Projection
├── frameworks-drivers/      第 4 圈：Spring 的 HTTP 薄殼、資料庫結構
└── web-ui/                  第 4 圈：看板管理的 Vue 畫面（由 main/web-ui 的路由接起來）
flow-analysis/               業務情境：流程分析（資料庫 schema flow_analysis）
├── entities/                第 1 圈：看板流程聚合、值物件、領域事件（純 Java，零相依）
├── use-cases/               第 2 圈：5 個指令與 2 個查詢（只相依 entities）
├── interface-adapters/      第 3 圈：Controller、Presenter、事件庫與讀取模型的轉接程式、Projection
├── board-activity-translation/  第 3 圈：防腐層——把看板管理公布的看板動態翻成流程分析的用語
├── frameworks-drivers/      第 4 圈：Spring 的 HTTP 薄殼、背景排程、資料庫結構
└── web-ui/                  第 4 圈：流程分析的 Vue 畫面
main/                        Main：建立所有物件並接起來（兩個情境的跨情境接線只在這裡）
└── web-ui/                  前端的 Main：進入點、路由、最外層版面
architecture-tests/          架構測試（ArchUnit）；每條規則都附一個故意違規的樣本
```

## 在本機執行

需要：JDK 25、Docker、Node 22。

```bash
# 1. 啟動資料庫
docker compose up -d

# 2. 啟動後端（http://localhost:8080）
./mvnw -q -DskipTests install
./mvnw -q -pl main spring-boot:run

# 3. 啟動前端（http://localhost:5173）：在專案根目錄執行
npm ci
npm run dev -w main/web-ui
```

前端用 npm workspaces：專案根目錄的 `package.json` 只列出 workspaces 與共用的開發工具；
`main/web-ui` 是前端的進入點，各業務情境的 `web-ui` 各自一個 workspace，彼此不互相引用（由 ESLint 擋）。

不想裝資料庫時，後端可以用記憶體儲存啟動（重啟後資料消失）：

```bash
./mvnw -q -pl main spring-boot:run -Dspring-boot.run.arguments=--kanban.storage=memory
```

## 流程分析

看板頁上方的「流程分析」連結進入（`/boards/<看板識別碼>/flow`）。看板上的變化由背景排程同步過來，大約 2 秒內反映在流程分析頁。

| 方法與路徑 | 做什麼 |
|---|---|
| `GET /api/flow-analysis/boards/{boardId}` | 流程分析頁的資料；看板剛建立、還沒同步過來時回 404 與「流程分析還沒準備好，請稍後重新整理」 |
| `PUT /api/flow-analysis/boards/{boardId}/columns/{columnId}/stage`，內容 `{"stage": "IN_PROGRESS"}` | 把一個欄位歸到某個階段（`UNCLASSIFIED`、`NOT_STARTED`、`IN_PROGRESS`、`DONE`）；整段歷史在同一個請求內重算 |
| `PUT /api/flow-analysis/boards/{boardId}/stuck-threshold`，內容 `{"days": 5}` | 設定卡住門檻（1 到 365 天） |
| `DELETE /api/flow-analysis/boards/{boardId}/stuck-threshold` | 取消卡住門檻 |

背景排程的設定（`main/src/main/resources/application.yml`）：

| 設定 | 預設值 | 意思 |
|---|---|---|
| `kanban.flow-analysis.background` | `true` | `false` 時不跑背景排程（只有測試會關掉） |
| `kanban.flow-analysis.sync-interval` | `PT2S` | 多久讀一次看板動態 |
| `kanban.flow-analysis.stuck-check-interval` | `PT10M` | 多久檢查一次卡住的工作項目 |

第一階段的資料庫第一次用第二階段的程式啟動時，會自動建立流程分析的資料表，並把第一階段以來的全部歷史補進流程分析。

## 跑全部檢查

```bash
# 後端：編譯、架構測試、行為測試、接 PostgreSQL 的整合測試（需要 Docker 在執行）
./mvnw verify

# 前端（在專案根目錄執行）
npm ci && npm run lint && npm test && npm run build
```

CI（GitHub Actions，`.github/workflows/ci.yml`）每次推送都會跑這兩組指令。

### 前端的兩個注意事項

- **請用 Node 22**（專案根目錄的 `.nvmrc`）。測試工具 Vitest 4 不支援 Node 23，在 Node 23 下測試會啟動失敗。
- **新增或升級前端套件時，請用 `npx npm@latest install`** 來更新 `package-lock.json`。npm 10.9 在解析 Vitest 4 的相依時會當掉（`Cannot read properties of null (reading 'edgesOut')`）；照 lock 檔安裝的 `npm ci` 不受影響。
