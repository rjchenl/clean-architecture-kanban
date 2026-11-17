# 情境關係（Context Map）

規則 DDD-3：跨情境整合只能透過已發布的事件或明確的介面，接收端用防腐層轉成自己的用語。

## 第一階段

只有一個業務情境：

| 業務情境 | 模組 | 負責什麼 |
|---|---|---|
| 看板管理（board-management） | `board-management/` | 看板、欄位、卡片的建立與變更；在製品上限；卡片移動歷程 |

沒有上游，也沒有下游。**DDD-3 在第一階段尚未實作**，因為沒有第二個情境可以整合。

## 第二階段

加入「流程分析」情境（設計文件 `docs/design/phase-two-flow-analysis.md`）。

| 業務情境 | 模組 | 角色 | 提供／使用 |
|---|---|---|---|
| 看板管理（board-management） | `board-management/` | 上游 | 公布「看板動態」（查詢 `ReadBoardActivities`，套件 `kanban.boardmanagement.usecases.queries.readboardactivities`），格式只增不改；不知道下游是誰 |
| 流程分析（flow-analysis） | `flow-analysis/` | 下游 | 防腐層（模組 `flow-analysis/board-activity-translation`，套件 `kanban.flowanalysis.adapters.boardactivity`）在背景定時讀看板動態，翻成流程分析的用語；只用識別碼參照看板 |

關係：看板管理以「已發布的格式」（Published Language）對外開放，流程分析以防腐層（Anti-Corruption Layer）接收。

**這張表的程式版**是 `architecture-tests` 的 `DddRules.CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE` 的兩張對照表：

- `publishedLanguages`：每個上游公布給別人讀的套件——目前只有看板管理的 `usecases.queries.readboardactivities`；
- `translators`：下游的哪個套件負責翻譯哪個上游——目前只有流程分析的 `adapters.boardactivity` 翻譯看板管理。

情境之間的任何其他引用（例如上游引用下游、下游的防腐層以外的程式引用上游、防腐層引用上游公布格式以外的東西）一律讓架構測試失敗。改這張表時，兩邊要一起改。

另外 `CONTEXTS_DO_NOT_SHARE_DOMAIN`（DDD-2）擋住任何情境引用另一個情境的領域模型；前端的畫面由根目錄 `eslint.config.js` 擋住互相引用。
