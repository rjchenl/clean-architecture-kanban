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

第一階段「看板管理」開發中。每一條規則目前到哪裡，以 [`docs/traceability.md`](docs/traceability.md) 為準——那張表每個規則編號一列，寫明落在哪個模組、由哪一支測試或檢查守住、目前狀態。

「跨情境整合」（DDD-3）要到第二階段加入第二個業務情境才會實作，所以目前不宣稱整個專案「符合 DDD」。

## 文件

| 文件 | 內容 |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | 專案規範：四套方法共 39 條規則，每條附驗證方式 |
| [`docs/design/phase-one-board-management.md`](docs/design/phase-one-board-management.md) | 第一階段設計：範圍、做法的取捨、業務規則、一致性說明 |
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
main/                        Main：建立所有物件並接起來
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
