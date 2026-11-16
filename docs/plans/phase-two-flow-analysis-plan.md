# 第二階段「流程分析」實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal：** 加入第二個業務情境「流程分析」，並透過看板管理公布的「看板動態」與防腐層整合。完成後 `CLAUDE.md` 的 39 條規則全部到達「已驗證」，DDD-3 也包含在內。

**Architecture：**
- `flow-analysis` 有四圈各自的 Maven 模組，另有防腐層模組 `board-activity-translation`；內三圈與防腐層都有 `module-info`。
- 寫入端：每個看板一個事件溯源的「看板流程」聚合，業務判斷都在它裡面。
- 查詢端：Projection 維護 `flow_analysis` schema 裡的讀取模型。
- 看板管理只多一份看板動態：一個 Projection 寫入、一個查詢 Use Case 讀出。第 4 圈的排程每 2 秒同步一次、每 10 分鐘檢查一次卡住。
- 前端進入點搬到 `main/web-ui/`，三個 web-ui 用 npm workspaces 串起來。

**Tech Stack：** 同第一階段：Java 25、Maven wrapper、Spring Boot 4.1.2、ArchUnit 1.5.1、Jackson 3（`tools.jackson`）、PostgreSQL 17（Testcontainers）、Flyway、Vue 3.5＋Vite 8＋TypeScript＋Vitest、GitHub Actions。另外用 Spring 的 `@Scheduled`。

**Spec：** `docs/design/phase-two-flow-analysis.md`（已核可，2026-11-17）。執行者必須連同專案根目錄的 `CLAUDE.md` 與第一階段設計 `docs/design/phase-one-board-management.md` 一起讀。

## Global Constraints

**規則與流程**
- **規則守則**：
  - `CLAUDE.md` 的 39 條都是硬規則。
  - 任何一條做不到，就停下來回報使用者，NEVER 自行放寬。
  - 刪除、停用或放寬任何架構測試、咬人測試或違規樣本都不准。
  - 允許清單只能照設計第 0 節 4d「加名字」。
  - 本計畫說「補強」的規則，一律用「原本的名稱條件，或新的形狀條件，任一成立就算」，只會更嚴。
- **每個 Task 開始時**，先用一兩句話說明改動屬於哪一圈、哪個業務情境、指令端還是查詢端（`CLAUDE.md` 第 6 節第 3 點）。
- **每次 `git commit` 之前**，先用白話講解決什麼、影響誰、使用者感覺到什麼差異。
- **規則跟著結構走**：
  - 引進新結構的 Task，要在同一個 commit 內加上守住它的規則。
  - 每條規則四件一組：規則本身、對正式程式碼的測試、違規樣本、咬人測試（`Imported.assertBites`，訊息必須含 `was violated`）。
  - 新規則的內容見文末「規則目錄」。
- **新的領域名詞**：先進 `docs/glossary.md` 的流程分析一節再寫程式（Task 0 一次列齊）。
- **對照表**：每個 Task 結束前更新 `docs/traceability.md` 對應的列。狀態只有三種：已驗證（附測試名稱）、已實作未驗證、尚未實作。

**套件與模組**
- **套件根**：
  - 流程分析是 `kanban.flowanalysis`。
  - 防腐層是 `kanban.flowanalysis.adapters.boardactivity`。
  - 看板管理新增的查詢是 `kanban.boardmanagement.usecases.queries.readboardactivities`。
- **模組**：

  | 目錄 | artifactId | JPMS 模組名 |
  |---|---|---|
  | `flow-analysis/entities` | `flow-analysis-entities` | `kanban.flowanalysis.entities` |
  | `flow-analysis/use-cases` | `flow-analysis-use-cases` | `kanban.flowanalysis.usecases` |
  | `flow-analysis/interface-adapters` | `flow-analysis-interface-adapters` | `kanban.flowanalysis.adapters` |
  | `flow-analysis/board-activity-translation` | `flow-analysis-board-activity-translation` | `kanban.flowanalysis.boardactivity` |
  | `flow-analysis/frameworks-drivers` | `flow-analysis-frameworks-drivers` | 不用 `module-info` |

  groupId 是 `kanban`，parent 是 `kanban-parent`。

**資料型別**
- **邊界資料只用這些型別**（`BOUNDARY_TYPES_ARE_SIMPLE`）：primitive、`String`、`Integer`、`Long`、`Boolean`、`Instant`、`List`、Use Cases 圈的 record 與 enum。
  - NEVER 用 `Duration`、`LocalDate`、`Map`、`Optional`、`Double`。
  - 時間長度一律是 `Long …Seconds`，週的起點用 `Instant weekStartsAt`。
- **指令套件**（`commands.<業務名>`）只能放 `…Input`／`…Output` 介面與 `…Request` record。
  - 指令端共用的列舉 `BoardChange` 放在 `kanban.flowanalysis.usecases.commands`，和 `CommandResponse` 同一層。
- **值物件的錯誤分兩種**：
  - 來自使用者輸入的（識別碼、階段、門檻）丟 `BusinessRuleViolation`，Use Case 把它轉成失敗碼。
  - 來自看板管理的事實（位置、名稱、時間長度）不合法，代表程式有錯，丟 `IllegalArgumentException` 或 `IllegalStateException`，NEVER 默默略過（設計第 7.5 節）。
- **聚合不讀時鐘、不產生亂數**：看板上的事實用動態帶來的時間；使用者的決定與定時檢查，用 Use Case 從 `TimeSource` 取得的時間。

**資料庫**
- **流程分析的 SQL**：
  - 執行期的 SQL 一律寫 `flow_analysis.` 前綴，因為兩個情境共用一個 DataSource，連線預設的 schema 是 `public`。
  - Flyway 遷移檔不寫前綴：Flyway 遷移時會把 `flow_analysis` 設成預設 schema。
- **advisory lock 編號**：
  - 第一階段已用 `7_238_114`（看板事件表）、`7_238_115`（看板讀取模型）。
  - 新的：`7_238_116`（看板動態紀錄）、`7_238_214`（流程分析事件表）、`7_238_215`（流程分析讀取模型）。
- **PostgreSQL 測試**：NEVER 停用或繞過事件表的唯讀觸發程序。要乾淨的資料庫時，整個 schema 砍掉重跑 Flyway。

**Main 與測試**
- **Main 的 Bean 方法名稱**：流程分析一律加 `flow` 前綴，例如 `flowEventStore`、`flowReadModelStore`，避免和看板管理同名、被當成覆寫而啟動失敗。
- **背景排程在測試裡一律關掉**：`main/src/test/resources/application.properties` 設 `kanban.flow-analysis.background=false`。
  - 開關的值一律用 `true`／`false`，條件寫 `havingValue = "true"`。不用 `on`／`off`：沒加引號的 `on` 在 YAML 會被讀成布林，正式環境可能對不上。
  - 測試要同步時，直接呼叫第 4 圈介面 `FlowAnalysisBackgroundWork`。
  - 只有 `BackgroundScheduleIntegrationTest` 打開排程，證明 `@Scheduled` 真的接上了；它標 `@DirtiesContext`，跑完就關掉，不會在背景繼續同步、干擾之後的測試。
  - 會讓同步永久停住的測試（Review Focus 1）一律在獨立的記憶體模式 context 跑，並標 `@DirtiesContext`，NEVER 用共用的 PostgreSQL：讀取位置全系統只有一個，停住會拖垮之後每一支要同步的測試。

**執行方式**
- 採 inline 執行（superpowers:executing-plans）：由主 session 逐 Task 實作，最後派一位全新的審查者看整個分支。
- 不採逐 Task 派 agent：18 個 Task 每個都派實作者加審查者，會超過全域規則的 agent 上限 10 個。

**驗證與 Git**
- **每個 Task 的最後驗證**：
  - 一律是 `./mvnw -q verify`（全部模組）。
  - 動到前端的 Task 再加跑專案根目錄的 `npm ci && npm run lint && npm test && npm run build`。
- **Git**：
  - 在分支 `phase-two-flow-analysis` 上工作；每個 Task 結束做一次本機 commit。
  - commit 訊息用繁體中文，結尾附 session 指定的 attribution。
  - **不推送、不開 PR**，除非使用者另外說。

## Review Focus

設計文件沒明講、但使用的人很可能碰到的五種情況。每一條都在對應 Task 加了測試。

1. **看板動態引用流程分析不認得的欄位或工作項目**（程式錯誤才會發生）：
   - 同步停在那一筆並寫日誌；
   - 之前的數字照常顯示，看板管理照常運作；
   - NEVER 默默略過。
   
   對應 Task 5、12、15。
2. **看板上的時間比同一個工作項目的前一筆還早**（時鐘回撥）：停留時間與交付時間以 0 計，不可以丟例外讓同步停住。對應 Task 4、5、6。
3. **看板剛建立、還沒同步過來時就打開流程分析頁**：顯示「流程分析還沒準備好，請稍後重新整理」，HTTP 404，不是 500 或空白頁。對應 Task 13、15、16。
4. **第一階段的資料庫第一次升級到第二階段**：
   - 看板動態紀錄原本是空的，啟動後自動從頭補齊；
   - 流程分析接著同步到第一階段以來的全部歷史。
   
   對應 Task 15。
5. **流程分析的請求內容有問題**：例如缺欄位、階段字串不認得、門檻是 0、366 或不是數字、網址裡的識別碼含控制字元。一律回覆 422 或 404 並附中文訊息，不可以是 500。對應 Task 8、13、15。

---

### Task 0：流程分析模組骨架、跨情境整合規則、防腐層的讀取位置

**這個 Task 是關卡：** module-info、Enforcer、Flyway 多 schema 任一項不能照計畫運作、又找不到合規解法 → 停止並回報使用者。

**Files：**
- Modify：根 `pom.xml`。
  - `<modules>` 在 `board-management/frameworks-drivers` 之後依序加入五個 `flow-analysis/*` 模組。
  - `dependencyManagement` 加這五個 artifact。
- Create：五個模組的 `pom.xml`。
  - entities：test-jar；Enforcer 用 `inner-ring-has-no-dependencies`，內容照抄看板管理。
  - use-cases：Enforcer 只允許 `kanban:flow-analysis-entities`。
  - interface-adapters：相依 use-cases 與 `tools.jackson.core:jackson-databind`；test-jar。
  - board-activity-translation：相依 `flow-analysis-use-cases` 與 `board-management-use-cases`；test-jar。
    - Enforcer 允許四個：這兩個，加上它們各自帶進來的 `flow-analysis-entities`、`board-management-entities`。
    - Enforcer 的 `searchTransitive` 預設是 true，只列兩個會把自己擋下（已查 maven-enforcer 文件）。
  - frameworks-drivers：相依 interface-adapters、board-activity-translation、`spring-boot-starter-webmvc`、`postgresql`（runtime）、`flyway-core`、`flyway-database-postgresql`（runtime）；test 相依兩個 test-jar、junit、assertj、`testcontainers-postgresql`、`testcontainers-junit-jupiter`。
- Modify：`main/pom.xml`，加 `flow-analysis-frameworks-drivers`。
- Modify：`architecture-tests/pom.xml`，加 `flow-analysis-entities` 的 test-jar（test scope）。
- Create：四個 `module-info.java`：
  ```java
  module kanban.flowanalysis.entities { exports kanban.flowanalysis.entities.boardflow; }
  module kanban.flowanalysis.usecases { requires transitive kanban.flowanalysis.entities;
      exports kanban.flowanalysis.usecases.commands; exports kanban.flowanalysis.usecases.commands.gateway; }
  module kanban.flowanalysis.adapters { requires transitive kanban.flowanalysis.usecases; requires java.sql;
      requires tools.jackson.databind; exports kanban.flowanalysis.adapters.system; }
  module kanban.flowanalysis.boardactivity { requires kanban.flowanalysis.usecases; requires kanban.boardmanagement.usecases;
      requires java.sql; exports kanban.flowanalysis.adapters.boardactivity; }
  ```
  之後每個 Task 新增套件時，同一個 commit 補 `exports`。`impl`、`support` 套件 NEVER export。
- Create（第一批正式類別，取代探針）：
  - entities：`BoardFlowId`、`FlowColumnId`、`WorkItemId`、`RejectionReason`、`BusinessRuleViolation`、package-private 的 `Identifiers`。
  - use-cases：`commands/CommandResponse`、`commands/gateway/TimeSource`。
  - interface-adapters：`system/SystemTimeSource`。
  - 防腐層：`ActivityPositionStore`、`InMemoryActivityPositionStore`、`PostgresActivityPositionStore`。
  - frameworks-drivers：`kanban.flowanalysis.frameworks.persistence.FlowAnalysisSchemaMigration`，以及 `src/main/resources/db/flow-analysis/V1__board_activity_position.sql`。
- Test：
  - entities：`IdentifiersTest`。
  - 防腐層：`ActivityPositionStoreContract`（在 test-jar 裡）、`InMemoryActivityPositionStoreTest`。
  - frameworks-drivers：`FlowAnalysisTestDatabase`、`FlowAnalysisSchemaMigrationTest`、`PostgresActivityPositionStoreTest`。
- Modify：`docs/glossary.md`，新增「## 業務情境：流程分析（flow-analysis）」一節，見 Step 1。
- Modify（architecture-tests）：
  - `CleanArchitectureRules.topLevelPackagesScream`：加 `kanban.flowanalysis` 的四圈。
  - `ADAPTER_PACKAGES_ON_ALLOW_LIST`：加 `"..adapters.boardactivity"`。
  - `EventSourcingRules.JDBC_USERS`：加 `"PostgresActivityPositionStore"`。
  - `DddRules`：新規則 `CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE`，見規則目錄 B1。
  - `CleanArchitectureRulesTest.讀得到各圈的正式類別`：加 `kanban.flowanalysis.entities`、`.usecases`、`.adapters`、`.adapters.boardactivity`、`.frameworks`。
  - `UseCaseRulesTest`：照現有三支模組宣告測試的寫法，替流程分析四個模組各加一支（Expected 見 Step 6）。
  - `GlossaryTest.用語表讀得到聚合內部物件_DDD4規則靠它`：改成 `containsExactlyInAnyOrder("Column", "Card", "FlowColumn", "WorkItem", "ColumnVisit", "FlowResult")`。
  - `GlossaryTest.用語表讀得到名稱`：加 `"BoardFlow", "WorkItemMoved", "SetColumnStage"`。

**Interfaces：**
- Produces：
  - `public record BoardFlowId(String value)`、`FlowColumnId(String value)`、`WorkItemId(String value)`：建構時用 `Identifiers.check(value)` 驗證。null 或空白丟 `IDENTIFIER_BLANK`；含控制字元（`Character.isISOControl`）丟 `INVALID_CHARACTERS`。
  - `public enum RejectionReason { IDENTIFIER_BLANK, INVALID_CHARACTERS, UNKNOWN_STAGE, STUCK_THRESHOLD_OUT_OF_RANGE, FLOW_COLUMN_NOT_FOUND, FLOW_COLUMN_REMOVED }`（完整清單在這裡一次定好）。
  - `public final class BusinessRuleViolation extends RuntimeException`：建構子 package-private `(RejectionReason)`，提供 `public RejectionReason reason()`。
  - `public record CommandResponse(boolean succeeded, String failureCode, String createdId)`：
    - 常數 `BOARD_FLOW_NOT_FOUND`、`CONCURRENT_UPDATE`、`INVALID_REQUEST`；
    - 靜態方法 `success(String)`、`failure(String)`。
  - `public interface TimeSource { Instant now(); }`；`public final class SystemTimeSource implements TimeSource`。
  - `public interface ActivityPositionStore { long lastPosition(); void advanceTo(long position); }`：
    - `lastPosition` 還沒處理過時是 0；
    - `advanceTo` 比目前小丟 `IllegalArgumentException`，相等時不變。
  - `PostgresActivityPositionStore(DataSource)`：
    - 表是 `flow_analysis.board_activity_position(name TEXT PRIMARY KEY, position BIGINT NOT NULL)`，name 固定 `'board-management'`；
    - 寫入用 `INSERT … ON CONFLICT (name) DO UPDATE`。
  - `FlowAnalysisSchemaMigration.migrate(DataSource)`：`Flyway.configure().dataSource(ds).schemas("flow_analysis").locations("classpath:db/flow-analysis").load().migrate()`。
  - `FlowAnalysisTestDatabase`：
    - `dataSource()`：容器與看板管理相同，是 `postgres:17-alpine`。
    - `freshDatabase()`：先 `DROP SCHEMA IF EXISTS flow_analysis CASCADE`，再 `migrate`。

- [ ] **Step 1：用語表。** 新增「## 業務情境：流程分析（flow-analysis）」一節，欄位與看板管理那一節相同，名稱如下：

  | 種類 | 程式名稱 |
  |---|---|
  | 聚合 | `BoardFlow` |
  | 聚合內部物件 | `FlowColumn`、`WorkItem`、`ColumnVisit`、`FlowResult` |
  | 值物件 | `BoardFlowId`、`FlowColumnId`、`WorkItemId`、`StuckThreshold`、`ActivityPosition`、`WorkItemTitle`、`FlowBoardName`、`FlowColumnName`、`FlowColumnPosition`、`DwellTime`、`LeadTime` |
  | 列舉 | `Stage`、`RejectionReason`、`BoardChange` |
  | 例外 | `BusinessRuleViolation` |
  | 事件介面 | `BoardFlowEvent` |
  | 事件 | 設計第 5.3 節的 16 個 |
  | 指令 | `RecordBoardActivity`、`SetColumnStage`、`SetStuckThreshold`、`RemoveStuckThreshold`、`CheckStuckWorkItems` |
  | 查詢 | `ViewFlowMetrics`、`ListBoardFlows` |
  
  - 中文用語與「意思」照設計附錄 A；附錄 A 沒有的，補上一句話的意思。
  - 看板管理那一節加三列：`BoardActivity`（對外公布的資料）、`BoardActivityKind`（列舉）、`ReadBoardActivities`（查詢）。
  - 註記：`ColumnVisit`、`FlowResult` 是本計畫新增的內部物件名稱，屬於 4d 同一類擴充（裁決）。
- [ ] **Step 2：寫失敗的測試。**
  - `IdentifiersTest`：
    - `new BoardFlowId("  ")` → `IDENTIFIER_BLANK`；
    - `new WorkItemId("a\u0000b")` → `INVALID_CHARACTERS`；
    - 相同值的兩個 `FlowColumnId` 相等。
  - `ActivityPositionStoreContract`：
    - `一開始是0`
    - `前進後讀得回來`
    - `不能倒退_丟IllegalArgumentException`
    - `前進到同一個位置_不變`
  - `FlowAnalysisSchemaMigrationTest`：
    - `建立自己的schema與版本紀錄`：`information_schema.tables WHERE table_schema='flow_analysis'` 包含 `board_activity_position`、`flyway_schema_history`。只斷言包含；「恰好哪幾張」由 Task 14 的 `FlowAnalysisSchemaTest` 斷言；
    - `不碰public`：`public` 沒有 `flyway_schema_history`。
- [ ] **Step 3：** 跑測試確認失敗（編譯失敗也算）。
- [ ] **Step 4：** 建立模組、pom、module-info，實作上述類別與遷移檔。
- [ ] **Step 5：本 Task 加入的架構規則：** `CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE`（規則目錄 B1）。違規樣本放在 `kanban.fixtures.contextmap.{upstream,downstream}` 底下，共三組：
  - 上游引用下游；
  - 下游的非防腐層引用上游；
  - 防腐層引用上游公布格式以外的套件。
  
  咬人測試三支，各用工廠方法傳入樣本根與樣本用的情境對照表。
- [ ] **Step 6：** 跑 `./mvnw -q verify`。Expected：BUILD SUCCESS。另確認新的模組宣告測試各自斷言：
  - flow entities `isEmpty()`；
  - flow use-cases `containsExactly("kanban.flowanalysis.entities")`；
  - flow adapters `containsExactlyInAnyOrder("kanban.flowanalysis.usecases", "java.sql", "tools.jackson.databind")`；
  - 防腐層 `containsExactlyInAnyOrder("kanban.flowanalysis.usecases", "kanban.boardmanagement.usecases", "java.sql")`；
  - flow use-cases 的 exports 不以 `.impl`、`.support` 結尾。
- [ ] **Step 7：反向證明（三件，各做一次後還原）：**
  - 在 flow use-cases 的類別裡 `import kanban.boardmanagement.entities.board.Board;` → Expected：編譯失敗。原因是 flow use-cases 沒有 `requires` 看板管理。
  - 在 flow entities 的 pom 加一個 compile 相依 → Expected：Enforcer 失敗。
  - 在防腐層放一個引用 `kanban.boardmanagement.usecases.commands.CommandResponse` 的類別 → Expected：編譯通過，但 `CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE` 失敗。這就是設計第 0 節 4a 那一處由架構測試擋。
  
  三次的失敗訊息各擷取一行，記進 commit 訊息本文。
- [ ] **Step 8：** 更新對照表：
  - CA-1、CA-2、CA-3、CA-16 的「落在哪個模組」加上流程分析。
  - DDD-3 改成「已實作未驗證」，附規則名稱。
- [ ] **Step 9：** Commit。

---

### Task 1：前端進入點搬到 `main/web-ui`（npm workspaces）

**這個 Task 是關卡：** 搬完後第一階段的畫面要一模一樣能用。Vite、vue-tsc、Vitest、ESLint 任一個在 workspaces 底下跑不起來、又找不到合規解法 → 停止並回報使用者。

**Files：**
- Create：根目錄 `package.json`：
  ```json
  { "name": "kanban-web", "private": true,
    "workspaces": ["main/web-ui", "board-management/web-ui"],
    "scripts": { "lint": "eslint board-management/web-ui/src main/web-ui/src",
                 "test": "npm run test --workspaces --if-present",
                 "build": "npm run build --workspaces --if-present" } }
  ```
  共用的開發工具都放在這裡的 devDependencies：vite、vitest、`@vitejs/plugin-vue`、`@vue/test-utils`、jsdom、typescript、vue-tsc、eslint、`eslint-plugin-vue`、typescript-eslint，版本沿用第一階段。各 workspace 不重複宣告。
- Move：`board-management/web-ui/.nvmrc` → 根目錄 `.nvmrc`；`board-management/web-ui/eslint.config.js` → 根目錄 `eslint.config.js`。CA-9 的格式化限制改成套用在 `*/web-ui/src/views/**`、`*/web-ui/src/components/**`。
- Move：`board-management/web-ui/{index.html, src/main.ts, src/router.ts, src/App.vue}` → `main/web-ui/` 底下相同位置。`router.ts` 改成從 `@kanban/board-management-web-ui` import 三個畫面。
- Create：`main/web-ui/{package.json, vite.config.ts, tsconfig.json, src/env.d.ts}`。
  - `package.json`：name `@kanban/main-web-ui`；相依 `"@kanban/board-management-web-ui": "0.1.0"`、vue、vue-router；scripts 有 `dev`、`build`（`vue-tsc --noEmit && vite build`）、`test`（`vitest run`）。
  - `vite.config.ts`：沿用原本的 proxy 與 jsdom 設定。
- Modify：`board-management/web-ui/package.json`。
  - name 改成 `@kanban/board-management-web-ui`；`exports: { ".": "./src/index.ts" }`。
  - vue、vue-router 改成 `peerDependencies`。
  - scripts 只留 `test`（`vitest run`）與 `build`（`vue-tsc --noEmit`，只做型別檢查）。
- Create：
  - `board-management/web-ui/src/index.ts`：export `BoardListView`、`BoardView`、`CardHistoryView`。
  - `board-management/web-ui/vitest.config.ts`：plugin vue、jsdom。
- Delete：`board-management/web-ui/vite.config.ts`、`board-management/web-ui/package-lock.json`；改用根目錄的 `package-lock.json`。
- Test：`main/web-ui/src/__tests__/contextBoundaries.spec.ts`：用 ESLint Node API 的 `lintText` 檢查各情境畫面互不引用。
- Modify：`.github/workflows/ci.yml` 的 frontend job：`working-directory` 改成專案根目錄；`node-version-file: .nvmrc`；`cache-dependency-path: package-lock.json`。
- Modify：`README.md` 的前端操作說明（`npm install`、`npm run dev -w main/web-ui`）。

**Interfaces：**
- Produces：
  - 畫面路由與第一階段相同：`/`、`/boards/:boardId`、`/boards/:boardId/cards/:cardId/history`。
  - 根目錄 `eslint.config.js` 有一條 `no-restricted-imports`：
    - `board-management/web-ui/src/**` 不得 import `@kanban/flow-analysis-web-ui` 或路徑含 `flow-analysis/` 的模組；
    - `flow-analysis/web-ui/src/**` 反過來同樣限制；
    - `main/web-ui` 不在此限。

- [ ] **Step 1：寫失敗的測試** `contextBoundaries.spec.ts`：
  - `看板管理的畫面引用流程分析_被擋下`：`lintText("import X from '@kanban/flow-analysis-web-ui'", { filePath: 'board-management/web-ui/src/views/Bad.vue' })` 有 `no-restricted-imports` 錯誤。
  - `流程分析的畫面引用看板管理_被擋下`：反過來同樣有錯誤。
  - `進入點可以引用兩者`：`filePath` 在 `main/web-ui/src/` 時沒有錯誤。
- [ ] **Step 2：** 搬家前，先在 `board-management/web-ui` 跑一次 `npm ci && npm test`，記下實際的 Vitest 案例數 N。
- [ ] **Step 2b：** 搬檔、建立 workspaces，在根目錄執行 `npm install`，產生根目錄的 `package-lock.json`。
- [ ] **Step 3：** 在根目錄跑 `npm run lint && npm test && npm run build`。Expected：
  - 全部通過；
  - 第一階段原有的 N 個 Vitest 案例都還在、都通過；
  - 上面 3 個新案例通過。
- [ ] **Step 4：反向證明：** 在 `board-management/web-ui/src/views/BoardView.vue` 暫時加 `import '@kanban/flow-analysis-web-ui'` → Expected：`npm run lint` 失敗並指出 `no-restricted-imports`。還原。
- [ ] **Step 5：快速冒煙（不是 L4 定案）：** 照 `README.md` 的方式用記憶體模式（`--kanban.storage=memory`）啟動後端，再用 `npm run dev -w main/web-ui` 啟動前端。用 chrome-devtools 打開 `http://localhost:5173/`，建一個看板、加一欄。Expected：畫面與第一階段相同，console 沒有錯誤。
- [ ] **Step 6：** 更新對照表 CA-8、CA-9 的備註（前端的 Main 在 `main/web-ui`）。Commit。

---

### Task 2：用語表分情境檢查、情境關係、審查清單

**Files：**
- Modify：`architecture-tests/.../support/Glossary.java`。
  - 依 `## 業務情境：…（<模組目錄>）` 分節解析，跳過表頭列。
  - 新增 `Set<String> codeNamesIn(String context)`：`context` 是去掉 `-` 的模組目錄名，例如 `flowanalysis`。
  - 新增 `Set<String> missingPerContext(JavaClasses classes, String root)`：類別的情境＝`root` 之後的第一段。`main` 與不屬於任何情境的類別略過；其餘名稱必須出現在自己那一節。
  - 原本的 `missing` 保留不動。
- Modify：`GlossaryTest`：加 `DDD1_每個情境的名稱都在自己那一節`（`missingPerContext(production(), "kanban")` 為空）。
- Create：違規樣本 `kanban.fixtures.glossarycontext.flowanalysis.entities.boardflow.ColumnName`。`ColumnName` 只在看板管理那一節。咬人測試 `用語表只列在別的情境_被抓出來`。
- Modify：`docs/context-map.md`，用設計附錄 B 取代「第二階段（規劃，尚未實作）」一節，並寫明情境對照表在 `DddRules` 的哪個常數。
- Modify：`docs/review-checklist.md`，新增三項：
  - DDD-9：Projection 只加總、平均、計數已判斷好的結果，統計方式只限 S1、S2（設計第 0 節 4c）；
  - DDD-3：防腐層只翻譯、不判斷；
  - CA-9：流程分析的畫面。

- [ ] **Step 1：** 寫失敗的咬人測試與 `DDD1_每個情境的名稱都在自己那一節`。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作 `Glossary` 的分節解析。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表 DDD-1，加上新的測試名稱。Commit。

---

### Task 3：看板管理公布看板動態（查詢端＋Projection＋記憶體版儲存）

先講三件事：這次在看板管理的 Use Cases 圈與 Interface Adapters 圈，屬於查詢端。看板管理的既有行為不變。

**Files：**
- Create（`board-management/use-cases/.../usecases/queries/readboardactivities/`）：`BoardActivity`、`BoardActivityKind`、`BoardActivityReader`、`ReadBoardActivitiesInput`、`ReadBoardActivitiesOutput`、`ReadBoardActivitiesRequest`、`ReadBoardActivitiesResponse`、`impl/ReadBoardActivitiesUseCase`。
- Modify：看板管理 use-cases 的 `module-info`：`exports …queries.readboardactivities;`。
- Create（`board-management/interface-adapters/.../adapters/projection/`）：`BoardActivityFormat`（package-private）、`BoardActivityProjection`、`BoardActivityLogStore`、`InMemoryBoardActivityLog`。
- Test：
  - use-cases：`ReadBoardActivitiesUseCaseTest`、`PublishedActivityContractTest`。
  - interface-adapters：`BoardActivityFormatTest`、`BoardActivityProjectionTest`、`BoardActivityLogStoreContract`（在 test-jar 裡）、`InMemoryBoardActivityLogTest`。
- Modify（architecture-tests）：
  - `UseCaseRules.QUERY_USE_CASES` 加 `readboardactivities`。
  - `CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL` 補強（規則目錄 B2），新增違規樣本 `readmodelbyshape`。

**Interfaces：**
- Produces：
  - 公布格式：
    ```java
    public enum BoardActivityKind { BOARD_CREATED, BOARD_RENAMED, BOARD_ARCHIVED, COLUMN_ADDED, COLUMN_RENAMED,
        COLUMN_MOVED, COLUMN_REMOVED, CARD_ADDED, CARD_CONTENT_CHANGED, CARD_MOVED, CARD_REMOVED }
    public record BoardActivity(long position, BoardActivityKind kind, String boardId, Instant occurredAt,
        String boardName, String columnId, String columnName, Integer columnPosition,
        String cardId, String cardTitle, String fromColumnId, String toColumnId) { }
    ```
  - 每一種動態用到的欄位如下；沒用到的一律是 null。`position` 是來源事件在事件表的全域位置。

    | 種類 | 來源事件 | 欄位 |
    |---|---|---|
    | BOARD_CREATED | BoardCreated | boardName |
    | BOARD_RENAMED | BoardRenamed | boardName |
    | BOARD_ARCHIVED | BoardArchived | — |
    | COLUMN_ADDED | ColumnAdded | columnId、columnName（加在最右邊） |
    | COLUMN_RENAMED | ColumnRenamed | columnId、columnName |
    | COLUMN_MOVED | ColumnMoved | columnId、columnPosition（在現有欄位中的新位置） |
    | COLUMN_REMOVED | ColumnRemoved | columnId |
    | CARD_ADDED | CardAdded | cardId、columnId、cardTitle |
    | CARD_CONTENT_CHANGED | CardContentChanged | cardId、cardTitle（不帶說明） |
    | CARD_MOVED | CardMoved | cardId、fromColumnId、toColumnId |
    | CARD_REMOVED | CardRemoved | cardId、columnId |
    | 不公布 | CardRepositioned、ColumnWipLimitSet、ColumnWipLimitRemoved | — |

  - 查詢邊界：
    - `ReadBoardActivitiesRequest(Long afterPosition, Integer maxCount)`
    - `ReadBoardActivitiesResponse(boolean accepted, List<BoardActivity> activities)`
    - `interface ReadBoardActivitiesInput { void execute(ReadBoardActivitiesRequest request); }`
    - `interface ReadBoardActivitiesOutput { void present(ReadBoardActivitiesResponse response); }`
    - `interface BoardActivityReader { List<BoardActivity> activitiesAfter(long position, int maxCount); }`
    - `afterPosition` 是 null 或負數、`maxCount` 是 null 或不在 1～500 → `accepted=false`，清單為空。
  - `final class BoardActivityFormat { static Optional<BoardActivity> of(RecordedEvent recorded) }`：對 sealed `BoardEvent` 用 switch，不寫 default。
  - `public interface BoardActivityLogStore extends BoardActivityReader`：
    - `long checkpoint()`
    - `void inExclusiveTransaction(Runnable work)`
    - `void append(BoardActivity activity)`：同一個位置再寫一次會覆蓋，結果不變。
    - `void advanceCheckpoint(long position)`
    - `void reset()`
  - `public final class BoardActivityProjection(EventFeed feed, BoardActivityLogStore store)`：
    - `void catchUp()`：照 `BoardProjection.catchUp` 的寫法，批次 200 筆，每筆一個交易；位置不比 checkpoint 新的略過。
    - `void rebuild()`

- [ ] **Step 1：寫失敗的測試。**
  - `BoardActivityFormatTest`：14 種事件各一支，斷言上表的欄位，不公布的三種回 `Optional.empty()`。
  - `ReadBoardActivitiesUseCaseTest`：
    - `回傳位置之後的動態`
    - `afterPosition為null_不受理`
    - `maxCount超過500_不受理`
    - `Request本身為null_不受理`
  - `BoardActivityProjectionTest`：
    - `從頭補齊_位置等於事件的全域位置`
    - `已處理過的事件再送一次_被略過_結果不變`
    - `重建後與重建前完全相同_位置不變`（ES-8）
    - `遇到處理不了的事件_丟例外且checkpoint停在前一筆`
    - `不公布的事件_checkpoint照樣前進`
  - `BoardActivityLogStoreContract`：
    - `一開始是空的_checkpoint為0`
    - `寫入的動態依位置讀得回來`
    - `同一個位置再寫一次_覆蓋而不是多一筆`
    - `activitiesAfter只回傳位置之後_最多maxCount筆`
    - `reset後全部清空`
    - `交易內丟例外_寫入與checkpoint都不保留`
    - `寫入方法只能在交易內呼叫`
  - `PublishedActivityContractTest`（設計第 4.4 節）：
    - `公布格式的欄位只能新增`：`BoardActivity` 的 record components 依序包含上面 12 個（名稱＋型別）。
    - `公布的種類只能新增`：`BoardActivityKind.values()` 的名稱包含上面 11 個。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：本 Task 補強的架構規則：** `ONLY_PROJECTION_WRITES_READ_MODEL`（規則目錄 B2）。
  - 原有的 `readmodelwrite`、`readmodelsidewrite`、`readmodelimpl` 三個咬人測試照舊要咬到。
  - 新樣本 `readmodelbyshape`：
    - 儲存 `ActivityLog`（`adapters.projection`）實作 `usecases.queries.viewthing.ThingReader`，並宣告 `append`；
    - `adapters.controllers` 的類別呼叫它的 `append`。
    - 補強前不會被擋，補強後會擋。
  - 補強前先跑一次新的咬人測試，確認它失敗，記進 commit 訊息。
- [ ] **Step 5：** `./mvnw -q verify` 通過。更新對照表：
  - CQRS-5：補強與新的測試名稱。
  - ES-8。
  - DDD-3：公布格式契約測試。
  - CA-5：跨情境的邊界只傳簡單資料（設計 4b）。`readboardactivities` 裡的型別由既有的 `BOUNDARY_TYPES_ARE_SIMPLE` 檢查；看板管理的型別只有防腐層碰得到，由 B1 保證。

  Commit。

---

### Task 4：流程分析的其餘值物件與「階段」

**Files：**
- Create（`flow-analysis/entities/.../entities/boardflow/`）：`Stage`、`StuckThreshold`、`ActivityPosition`、`WorkItemTitle`、`FlowBoardName`、`FlowColumnName`、`FlowColumnPosition`、`DwellTime`、`LeadTime`。
- Test：`ValueObjectsTest`。

**Interfaces：**
- Produces：
  - `public enum Stage { UNCLASSIFIED, NOT_STARTED, IN_PROGRESS, DONE; public static Stage named(String name) }`：null 或不認得的名稱丟 `UNKNOWN_STAGE`。
  - `StuckThreshold(int days)`：不在 1～365 丟 `STUCK_THRESHOLD_OUT_OF_RANGE`；`Duration length()`。
  - `ActivityPosition(long value)`：小於 1 丟 `IllegalArgumentException`；`boolean isAfter(ActivityPosition other)`。
  - `WorkItemTitle(String value)`、`FlowBoardName`、`FlowColumnName`：null 丟 `IllegalArgumentException`；不驗長度（F14：上游已經驗過，一律接受）。
  - `FlowColumnPosition(int index)`：小於 0 丟 `IllegalArgumentException`。
  - `DwellTime(Duration value)`、`LeadTime(Duration value)`：null 或負數丟 `IllegalArgumentException`。
    - 另提供 `static DwellTime between(Instant from, Instant to)`、`static LeadTime between(Instant from, Instant to)`：`to` 早於 `from` 時回傳 0（F15 補充，Review Focus 2）。

- [ ] **Step 1：寫失敗的測試**（`ValueObjectsTest`）：
  - `Stage.named("IN_PROGRESS")` 是 `IN_PROGRESS`；`Stage.named("doing")` 丟 `UNKNOWN_STAGE`；`Stage.named(null)` 丟 `UNKNOWN_STAGE`。
  - 門檻 0 與 366 丟 `STUCK_THRESHOLD_OUT_OF_RANGE`；1 與 365 可以建立。
  - `DwellTime.between(T1, T0)` 等於 `Duration.ZERO`。
  - `new LeadTime(Duration.ofSeconds(-1))` 丟 `IllegalArgumentException`。
  - 標題是 101 個字也可以建立。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表 DDD-6。Commit。

---

### Task 5：事件、看板流程聚合（記錄看板上的事實）、聚合測試工具

**Files：**
- Create（`…/entities/boardflow/`）：
  - `BoardFlowEvent`（sealed interface）與設計第 5.3 節的 16 個事件 record；
  - `BoardFlow`；
  - package-private 的 `FlowColumn`、`WorkItem`、`ColumnVisit`、`FlowResult`、`FlowRules`。
- Test：`BoardFlowScenario`、`BoardActivityFactsTest`。
- Modify（architecture-tests）：
  - `EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` 加 `.doNotHaveSimpleName("BoardFlow")`。
  - `EntitiesRules.STATE_CHANGES_ONLY_IN_APPLY` 的建構子例外加欄位 `"workItems"`。
  - `EntitiesRules.SCENARIO_TOOLS_AND_REPLAY_TESTS` 加 `"kanban.flowanalysis.entities.boardflow.BoardFlowScenario"`、`"kanban.flowanalysis.entities.boardflow.ReplayTest"`。
  - `EntitiesRulesTest.ES5_檢查對象是Entities圈的全部測試程式_不看檔名也不含正式程式碼`：加斷言，含 `…boardflow.BoardFlowScenario`、不含 `…boardflow.BoardFlow`。
  - `EntitiesRulesTest.DDD5_聚合之間不互相持有物件`：拿掉 `allowEmptyShould(true)`，改寫註解，說明現在有 `board`、`boardflow` 兩個聚合。

**Interfaces：**
- Produces：
  - `public sealed interface BoardFlowEvent { BoardFlowId boardFlowId(); Instant occurredAt(); }`
  - 事件欄位如下。欄位順序就是建構子順序，`occurredAt` 一律最後。看板上的事實帶 `ActivityPosition activityPosition`，排在第二個。
    - `BoardFlowCreated(boardFlowId, activityPosition, FlowBoardName name, occurredAt)`
    - `BoardFlowRenamed(boardFlowId, activityPosition, FlowBoardName name, occurredAt)`
    - `BoardFlowArchived(boardFlowId, activityPosition, occurredAt)`
    - `FlowColumnAdded(boardFlowId, activityPosition, FlowColumnId flowColumnId, FlowColumnName name, Stage stage, occurredAt)`：stage 一律是 `UNCLASSIFIED`（F2），由聚合寫進事件，Projection 不自己假設。
    - `FlowColumnRenamed(boardFlowId, activityPosition, flowColumnId, FlowColumnName name, occurredAt)`
    - `FlowColumnMoved(boardFlowId, activityPosition, flowColumnId, FlowColumnPosition toPosition, occurredAt)`
    - `FlowColumnRemoved(boardFlowId, activityPosition, flowColumnId, occurredAt)`
    - `WorkItemAdded(boardFlowId, activityPosition, WorkItemId workItemId, FlowColumnId flowColumnId, WorkItemTitle title, occurredAt)`
    - `WorkItemTitleChanged(boardFlowId, activityPosition, workItemId, WorkItemTitle title, occurredAt)`
    - `WorkItemMoved(boardFlowId, activityPosition, workItemId, FlowColumnId fromFlowColumnId, FlowColumnId toFlowColumnId, DwellTime dwellTimeInFromColumn, occurredAt)`
    - `WorkItemRemoved(boardFlowId, activityPosition, workItemId, FlowColumnId flowColumnId, occurredAt)`
    - `ColumnStageSet(boardFlowId, flowColumnId, Stage stage, occurredAt)`
    - `StuckThresholdSet(boardFlowId, StuckThreshold threshold, occurredAt)`
    - `StuckThresholdRemoved(boardFlowId, occurredAt)`
    - `WorkItemFlowResultChanged(boardFlowId, workItemId, Instant startedAt, Instant completedAt, LeadTime leadTime, occurredAt)`：後三個可以是 null。
    - `WorkItemStuckStatusChanged(boardFlowId, workItemId, boolean stuck, FlowColumnId flowColumnId, Instant stuckSince, occurredAt)`：`stuck=false` 時後兩個是 null。
  - `BoardFlow`（`public final class`）：
    - 欄位：`id`、`name`、`archived`、`lastActivityPosition`、`stuckThreshold`（null＝未設定）、`List<FlowColumn> columns = List.of()`、`List<WorkItem> workItems = List.of()`（依第一次出現的順序）、`persistedVersion`、`pendingEvents`。
    - 簿記方法照抄 `Board`：`static create(BoardFlowId, ActivityPosition, FlowBoardName, Instant)`、`static rehydrate(List<BoardFlowEvent>)`、`id()`、`persistedVersion()`、`uncommittedEvents()`、`markCommitted()`。內部是 private 的 `record` 與 `apply`（不寫 default 的 switch），以及各事件的 `onXxx`。
    - 本 Task 的指令方法（看板上的事實），全部是 `public void`：
      - `acknowledgeCreation(ActivityPosition)`
      - `renameBoard(ActivityPosition, FlowBoardName, Instant)`
      - `archiveBoard(ActivityPosition, Instant)`
      - `addColumn(ActivityPosition, FlowColumnId, FlowColumnName, Instant)`
      - `renameColumn(ActivityPosition, FlowColumnId, FlowColumnName, Instant)`
      - `moveColumn(ActivityPosition, FlowColumnId, FlowColumnPosition, Instant)`
      - `removeColumn(ActivityPosition, FlowColumnId, Instant)`
      - `addWorkItem(ActivityPosition, WorkItemId, FlowColumnId, WorkItemTitle, Instant)`
      - `changeWorkItemTitle(ActivityPosition, WorkItemId, WorkItemTitle, Instant)`
      - `moveWorkItem(ActivityPosition, WorkItemId, FlowColumnId from, FlowColumnId to, Instant)`
      - `removeWorkItem(ActivityPosition, WorkItemId, FlowColumnId from, Instant)`
    - 每個事實方法都依序做：
      1. 位置不比 `lastActivityPosition` 新，直接 return，不產生事件（F14）。
      2. 檢查引用的欄位或工作項目存在、`from` 和聚合記錄的所在欄相同；不符就丟 `IllegalStateException`（Review Focus 1）。
      3. 記錄事實事件。
      4. 交給 `FlowRules` 判斷，判斷結果有變才記錄 `WorkItemFlowResultChanged`、`WorkItemStuckStatusChanged`（Task 6、7）。
    - `acknowledgeCreation` 遇到更新的位置時，丟 `IllegalStateException`：同一個看板不會建立兩次。
    - 看板流程本身不記欄位的順序：`FlowColumnMoved` 只是記下的事實，給 Projection 用。
  - package-private 的內部物件：
    - `record FlowColumn(FlowColumnId id, FlowColumnName name, Stage stage, boolean removed)`
    - `record WorkItem(WorkItemId id, WorkItemTitle title, List<ColumnVisit> visits, boolean removed, FlowResult result, boolean stuck)`
    - `record ColumnVisit(FlowColumnId flowColumnId, Instant enteredAt)`
    - `record FlowResult(Instant startedAt, Instant completedAt, LeadTime leadTime)`，另有 `static FlowResult NONE`
  - `final class FlowRules`（package-private）：判斷全寫在這裡，聚合根本身不呼叫集合的修改方法（`STATE_CHANGES_ONLY_IN_APPLY`）。
  - 測試工具 `BoardFlowScenario`：
    - 用法比照 `BoardScenario`：`given(BoardFlowEvent...)`、`whenCreating(Supplier<BoardFlow>)`、`.when(Consumer<BoardFlow>)`。
    - 斷言：`.thenEvents(...)`、`.thenNoEvents()`、`.thenRejected(RejectionReason)`、`.thenFails(Class<? extends RuntimeException>)`，最後一種要求沒有新事件。
    - `.events()` 回傳產生的事件，給重算比較用。
    - 常數：`FLOW = new BoardFlowId("board-1")`、`T0 = Instant.parse("2026-11-16T01:00:00Z")`、`at(long position)` 回傳 `new ActivityPosition(position)`、`days(n)` 回傳 `T0.plus(Duration.ofDays(n))`。

- [ ] **Step 1：寫失敗的測試**（`BoardActivityFactsTest`。共用 Given：`BoardFlowCreated(FLOW, at(1), "產品開發", T0)`，加上欄位 `col-todo`（at 2）、`col-doing`（at 3））：
  - `建立看板流程_產生BoardFlowCreated`
  - `同一筆動態收到兩次_只記一次`（F14；參數化，11 個事實方法各跑一次，第二次 `thenNoEvents`）
  - `位置比已處理的舊_略過`（F14）
  - `新增欄位_階段是未分類`（F2）
  - `改欄名_產生FlowColumnRenamed`、`調整欄位順序_產生FlowColumnMoved`
  - `刪除欄位_產生FlowColumnRemoved`
  - `新增工作項目_產生WorkItemAdded_所在欄未分類時沒有流程結果`（F2）
  - `改標題_產生WorkItemTitleChanged`；`標題沒變_不產生事件`
  - `換欄_產生WorkItemMoved並帶在來源欄的停留時間`（F7；`days(2)` 移動，停留 2 天）
  - `看板上的時間倒退_停留時間以0計`（F15、Review Focus 2）
  - `刪除工作項目_產生WorkItemRemoved_沒有停留時間`（F7：被刪除不算離開）
  - `事件時間用看板上發生的時間`（F15）
  - `看板改名與封存_照常記錄`
  - `引用不存在的欄位或工作項目_丟IllegalStateException且沒有新事件`（Review Focus 1；參數化，6 種事實）
  - `來源欄和記錄的所在欄不一致_丟IllegalStateException`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作事件、`BoardFlow`、內部物件、`BoardFlowScenario`。16 種事件的 `onXxx` 在本 Task 全部寫好；判斷結果的事件留到 Task 6、7 才會產生。
- [ ] **Step 4：** `./mvnw -q verify` 通過。更新對照表：DDD-4、DDD-7、ES-3、ES-5 加上流程分析；DDD-5 改成「有兩個聚合」。Commit。

---

### Task 6：階段設定與流程結果（含重算）

**Files：** Modify `BoardFlow`、`FlowRules`；Test：`StageRulesTest`、`RecalculationTest`。

**Interfaces：**
- Produces：`BoardFlow.setColumnStage(FlowColumnId, Stage, Instant at)`。
  - 欄位不存在 → `FLOW_COLUMN_NOT_FOUND`；欄位已刪除 → `FLOW_COLUMN_REMOVED`（F12）；階段相同 → 不產生事件。
  - 產生事件的順序：`ColumnStageSet` → 結果有變的工作項目各一筆 `WorkItemFlowResultChanged`（依 `workItems` 順序）→ 卡住狀態有變的各一筆 `WorkItemStuckStatusChanged`（Task 7）。
- `FlowRules.resultOf(WorkItem, Function<FlowColumnId, Stage>)` 的演算法（F2～F5、F11）：
  1. 去掉所在欄是「未分類」的進出紀錄。
  2. 開始時間＝剩下的紀錄中，第一筆階段是進行中或完成的進入時間。
  3. 最後一筆是完成 → 完成時間＝結尾那一段連續完成紀錄的第一筆進入時間；否則沒有完成時間。
  4. 交付時間＝`LeadTime.between(開始, 完成)`，兩者都有時才算。
  
  被刪除的工作項目照同一個演算法算，所以在完成欄被刪除的保留完成紀錄（F11）。
- 事實方法（Task 5）在記錄事實之後，對受影響的工作項目重算一次，結果有變才記錄 `WorkItemFlowResultChanged`。

- [ ] **Step 1：寫失敗的測試**（`StageRulesTest`。共用 Given：`待辦`、`進行中`、`完成`三欄，以及至少一個工作項目）：
  - `指定階段_產生ColumnStageSet`（F1）；`指定相同階段_不產生事件`
  - `不存在的欄位_拒絕FLOW_COLUMN_NOT_FOUND`
  - `已刪除的欄位不能改階段_拒絕FLOW_COLUMN_REMOVED`（F12）
  - `第一次進入進行中_記開始時間`（F3）；`移回未開始再回進行中_開始時間不變`（F3）
  - `進入完成_記完成時間與交付時間`（F4、F5）
  - `直接從未開始進入完成_開始與完成同時_交付時間為0`（F3、F5）
  - `移回進行中_不再算完成_再進入完成重新記完成時間`（F4）
  - `在兩個完成欄之間移動_完成時間不變`（F4）
  - `從完成欄移到未分類欄_仍算完成`（F2、F4 裁決）
  - `進出未分類欄_不影響開始時間`（F2）
  - `改階段_所有工作項目從頭重算_只替結果有變的產生事件`（F6）
  - `在完成欄被刪除_保留完成紀錄`、`在進行中欄被刪除_不算完成`（F11）
  - `欄位刪除後_經過它的歷史照它原本的階段判斷`（F12）
  - `看板封存後_仍可以改階段`（F13）
  - `時間倒退時_交付時間以0計`（Review Focus 2）
- [ ] **Step 2：寫失敗的測試**（`RecalculationTest`）：`先設定A再改成B_結果等於一開始就設定B`（F6，設計第 9 節第 4 項）。
  - 準備同一串事實：3 個工作項目、會進出每一種階段的欄位。
  - 情境一：先設定 A 再改成 B；情境二：一開始就設定 B。
  - 兩邊各收集「每個工作項目最後一筆 `WorkItemFlowResultChanged`」，用 `.events()` 取，兩邊的 Map 必須相等。
- [ ] **Step 3：** 跑測試確認失敗。
- [ ] **Step 4：** 實作。
- [ ] **Step 5：** `./mvnw -q verify` 通過；更新對照表 DDD-4，列出 F1～F6、F11～F13 對應的測試名稱。Commit。

---

### Task 7：卡住的判斷與重播測試

**Files：** Modify `BoardFlow`、`FlowRules`；Test：`StuckRulesTest`、`ReplayTest`。

**Interfaces：**
- Produces：
  - `setStuckThreshold(StuckThreshold, Instant at)`：相同門檻不產生事件。
  - `removeStuckThreshold(Instant at)`：本來就沒有門檻時不產生事件。
  - `checkStuckWorkItems(Instant now)`
- `FlowRules.isStuck(WorkItem, stages, StuckThreshold, Instant now)` 成立的條件（F8、F9）：
  - 工作項目沒有被刪除；
  - 有門檻；
  - 所在欄是進行中；
  - `now - 進入所在欄的時間 >= 門檻`。
- 重新判斷的時機（F10）：
  - 定時檢查：用 `now`。
  - 換欄或被刪除：原本卡住的變成不卡住。
  - 改階段、設定門檻、取消門檻：用指令的 `at`。
  
  狀態有變才產生 `WorkItemStuckStatusChanged`：變成卡住時帶所在欄與 `stuckSince`（＝進入所在欄的時間）。

- [ ] **Step 1：寫失敗的測試**（`StuckRulesTest`）：
  - `設定門檻_產生StuckThresholdSet`；`設定相同門檻_不產生事件`；`取消門檻_產生StuckThresholdRemoved`；`沒有門檻又取消_不產生事件`（F8）
  - `沒有門檻_定時檢查不產生事件`（F8）
  - `在進行中欄待滿門檻_定時檢查後變成卡住`；`還差一秒_不算卡住`（F9）
  - `在未開始或完成欄待再久_都不算卡住`（F9）
  - `已經卡住_再檢查一次_不重複產生事件`
  - `換欄_不再卡住`；`被刪除_不再卡住`（F10、F11）
  - `改階段讓所在欄變成進行中_立刻判斷`；`改門檻_立刻重新判斷`；`取消門檻_全部不再卡住`（F10）
- [ ] **Step 2：寫失敗的測試**（`ReplayTest`，ES-4）：
  - `同一串事件重播兩次_後續指令的結果相同`：歷史裡 16 種事件各至少一筆，對兩個實例下同一個會被拒絕的指令、同一個會成功的指令。
  - `重播不產生未提交事件`
- [ ] **Step 3：** 跑測試確認失敗。
- [ ] **Step 4：** 實作。
- [ ] **Step 5：** `./mvnw -q verify` 通過。更新對照表：DDD-4 補齊 F7～F15；ES-4 加上流程分析。Commit。

---

### Task 8：流程分析的指令 Use Case 與指令端 Gateway

**Files：**
- Create（`flow-analysis/use-cases/.../usecases/commands/`）：
  - `BoardChange`；
  - `gateway/{BoardFlowRepository, EventStore, ConcurrentUpdateException}`；
  - `support/BoardFlowCommand`；
  - 五個指令套件 `recordboardactivity`、`setcolumnstage`、`setstuckthreshold`、`removestuckthreshold`、`checkstuckworkitems`，每個都有 `…Input`、`…Output`、`…Request`、`impl/…UseCase`。
- Modify：flow use-cases 的 `module-info`，exports 五個指令套件。
- Test：`FlowCommandFixture`、`FakeBoardFlowRepository`，以及五支 `…UseCaseTest`。
- Modify（architecture-tests）：
  - `UseCaseRules.COMMAND_USE_CASES` 加這五個名稱。
  - `EventSourcingRules.WRITE_SIDE_GATEWAY_TYPES` 加 `"BoardFlowRepository"`。

**Interfaces：**
- Produces：
  - `public enum BoardChange`：`BOARD_FLOW_CREATED`、`BOARD_FLOW_RENAMED`、`BOARD_FLOW_ARCHIVED`、`FLOW_COLUMN_ADDED`、`FLOW_COLUMN_RENAMED`、`FLOW_COLUMN_MOVED`、`FLOW_COLUMN_REMOVED`、`WORK_ITEM_ADDED`、`WORK_ITEM_TITLE_CHANGED`、`WORK_ITEM_MOVED`、`WORK_ITEM_REMOVED`。
  - `RecordBoardActivityRequest(Long activityPosition, BoardChange change, String boardFlowId, Instant occurredAt, String boardName, String flowColumnId, String flowColumnName, Integer flowColumnPosition, String workItemId, String workItemTitle, String fromFlowColumnId, String toFlowColumnId)`：每種變化需要哪些欄位，對應 Task 3 的表；名稱改成流程分析的用語。
  - `SetColumnStageRequest(String boardFlowId, String flowColumnId, String stage)`、`SetStuckThresholdRequest(String boardFlowId, Integer days)`、`RemoveStuckThresholdRequest(String boardFlowId)`、`CheckStuckWorkItemsRequest(String boardFlowId)`。
  - 每個 `…Input { void execute(…Request request); }`、每個 `…Output { void present(CommandResponse response); }`。
  - Gateway：
    - `interface BoardFlowRepository { Optional<BoardFlow> load(BoardFlowId id); void save(BoardFlow flow); }`
    - `interface EventStore { List<BoardFlowEvent> readStream(String streamId); void append(String streamId, long expectedVersion, List<BoardFlowEvent> events); }`
    - `ConcurrentUpdateException(String streamId, long expectedVersion)`
  - `BoardFlowCommand`（照 `BoardCommand` 的寫法：`required`、`readRequest`、`guarded`；只有 `readRequest` 把 `MissingField` 或 NPE 當成請求不完整）：
    - `onExistingBoardFlow(BoardFlowRepository, Supplier<String> boardFlowId, Action)`
    - `onBoardFlow(BoardFlowRepository, Supplier<String> boardFlowId, Supplier<BoardFlow> whenMissing, Action whenExisting)`：`whenMissing` 是 null 時，找不到就回 `BOARD_FLOW_NOT_FOUND`。
    - 每個 Use Case 只呼叫它一次（`ONE_AGGREGATE_PER_COMMAND`）。
  - 各 Use Case 的建構子：
    - `RecordBoardActivityUseCase(BoardFlowRepository, RecordBoardActivityOutput)`
    - `SetColumnStageUseCase(BoardFlowRepository, TimeSource, SetColumnStageOutput)`；`SetStuckThresholdUseCase`、`RemoveStuckThresholdUseCase`、`CheckStuckWorkItemsUseCase` 同形。
  - `RecordBoardActivityUseCase`：
    - `BOARD_FLOW_CREATED` 在看板流程不存在時呼叫 `BoardFlow.create`，存在時呼叫 `acknowledgeCreation`。
    - 其他變化在看板流程不存在時回 `BOARD_FLOW_NOT_FOUND`。
    - 聚合丟的 `IllegalStateException` 不吞，往外丟。

- [ ] **Step 1：寫失敗的測試。**
  - `FlowCommandFixture`：照 `CommandFixture`，`@AfterEach 一個指令只修改一個看板流程`。
  - `RecordBoardActivityUseCaseTest`：
    - `每一種變化_呼叫聚合並存回`（參數化 11 種，斷言事實事件）
    - `看板流程不存在且不是建立_回覆BOARD_FLOW_NOT_FOUND`
    - `重送建立看板_成功且沒有新事件`
    - `缺必要欄位_回覆INVALID_REQUEST`
    - `Request本身為null_回覆INVALID_REQUEST`
    - `引用不存在的工作項目_例外往外丟`
    - `儲存時版本衝突_回覆CONCURRENT_UPDATE`
  - `SetColumnStageUseCaseTest`：
    - `成功_產生ColumnStageSet與重算結果`
    - `不認得的階段_回覆UNKNOWN_STAGE`
    - `已刪除的欄位_回覆FLOW_COLUMN_REMOVED`
    - `看板流程不存在_回覆BOARD_FLOW_NOT_FOUND`
    - `識別碼含控制字元_回覆INVALID_CHARACTERS`
    - `Request欄位為null_回覆INVALID_REQUEST`
    - `儲存時版本衝突_回覆CONCURRENT_UPDATE`
  - 另外三支同形：
    - 門檻 0 → `STUCK_THRESHOLD_OUT_OF_RANGE`；
    - `CheckStuckWorkItems` 用 `time.now()`，斷言 `WorkItemStuckStatusChanged` 的 `occurredAt` 等於 `NOW`。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過。更新對照表：CA-4、CA-5、CQRS-2、DDD-5（一個指令一個看板流程）、DDD-9 加上流程分析。Commit。

---

### Task 9：事件轉 JSON、記憶體版 Event Store、看板流程 Repository

**Files：**
- Create（`flow-analysis/interface-adapters/.../adapters/eventstore/`）：`BoardFlowEventJson`、`StoredEvent`、`UnknownEventTypeException`、`InMemoryEventStore`、`NotifyingEventStore`、`EventSourcedBoardFlowRepository`。
- Create：`…/usecases/eventfeed/{EventFeed, RecordedEvent}`，記錄的是 `BoardFlowEvent`，並 export。
- Test：`EventStoreContract`（在 test-jar 裡，照看板管理那一份的測試名稱，型別換成流程分析的）、`InMemoryEventStoreTest`、`BoardFlowEventJsonTest`、`EventSourcedBoardFlowRepositoryTest`、`NotifyingEventStoreTest`。
- Create：`src/test/resources/event-samples/<Type>.v1.json`，16 個樣本加一份 README，規矩比照看板管理：只能新增、不能修改。

**Interfaces：**
- Produces：
  - `BoardFlowEventJson`：公開方法和看板管理相同，`latestVersions()`、`toStored(BoardFlowEvent)`、`fromStored(StoredEvent)`。
    - 型別名稱就是事件的簡名，用字串寫死，16 種都是 v1。
    - 可以是 null 的欄位寫成 JSON null。
    - 時間長度存成秒數，例如 `dwellTimeSeconds`、`leadTimeSeconds`。
  - `EventSourcedBoardFlowRepository(EventStore)`：串流識別碼是 `"board-flow-" + id.value()`。
  - `NotifyingEventStore(EventStore delegate, Runnable afterAppend)`：照抄看板管理，只吞 `RuntimeException` 並寫日誌。
  - `InMemoryEventStore implements EventStore, EventFeed`：位置從 1 起算。

- [ ] **Step 1：寫失敗的測試。**
  - 契約測試全部。
  - `BoardFlowEventJsonTest`：
    - `每一種事件都能來回轉換`（16 種）
    - `每種事件的每個版本都有樣本檔`
    - `樣本檔讀得回來`
    - `不認得的類型_丟UnknownEventTypeException`
    - `比最新版還新的格式_丟UnknownEventTypeException`
  - Repository：
    - `找不到_回empty`
    - `存了再載入_狀態相同`
    - `版本衝突_丟ConcurrentUpdateException`
    - `沒有新事件_不寫入`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過。更新對照表：ES-2、ES-6、ES-7、CA-6、DDD-8 加上流程分析。Commit。

---

### Task 10：查詢端——讀取模型資料與兩個查詢 Use Case

**Files：**
- Create（`flow-analysis/use-cases/.../usecases/queries/`）：
  - `viewflowmetrics/`：`ViewFlowMetricsInput`、`ViewFlowMetricsOutput`、`ViewFlowMetricsRequest`、`ViewFlowMetricsResponse`、`FlowMetrics`、`ColumnMetrics`、`WeeklyCompletion`、`Delivery`、`StuckWorkItem`、`FlowMetricsReader`、`impl/ViewFlowMetricsUseCase`。
  - `listboardflows/`：`ListBoardFlowsInput`、`ListBoardFlowsOutput`、`ListBoardFlowsResponse`、`BoardFlowListReader`、`impl/ListBoardFlowsUseCase`。
- Modify：flow use-cases 的 `module-info`，exports 兩個查詢套件。
- Test：`ViewFlowMetricsUseCaseTest`、`ListBoardFlowsUseCaseTest`。
- Modify（architecture-tests）：`UseCaseRules.QUERY_USE_CASES` 加 `viewflowmetrics|listboardflows`。

**Interfaces：**
- Produces：
  ```java
  public record ViewFlowMetricsRequest(String boardFlowId, Instant asOf) { }
  public record ViewFlowMetricsResponse(boolean found, FlowMetrics metrics) { }
  public record FlowMetrics(String boardFlowId, String boardName, boolean archived, Integer stuckThresholdDays,
      List<ColumnMetrics> columns, List<WeeklyCompletion> recentWeeks, Delivery delivery, List<StuckWorkItem> stuckWorkItems) { }
  public record ColumnMetrics(String flowColumnId, String name, String stage, int workItemCount, Long averageDwellSeconds) { }
  public record WeeklyCompletion(Instant weekStartsAt, int completedCount) { }
  public record Delivery(int completedCount, Long averageLeadTimeSeconds, Long longestLeadTimeSeconds) { }
  public record StuckWorkItem(String workItemId, String title, String flowColumnName, Instant stuckSince) { }
  public interface FlowMetricsReader { Optional<FlowMetrics> flowMetrics(String boardFlowId, Instant asOf); }
  public record ListBoardFlowsResponse(List<String> boardFlowIds) { }
  public interface BoardFlowListReader { List<String> allBoardFlowIds(); }
  ```
  - `ListBoardFlowsInput { void execute(); }`。
  - 查詢 Use Case 不讀時鐘，`asOf` 由呼叫端帶入。
  - 「最近 8 週」的計算全在讀取模型（Task 11）裡，Use Case 只負責協調。
  - Request 是 null、或欄位是 null → `found=false`。

- [ ] **Step 1：寫失敗的測試**：
  - `找得到_原樣交回`
  - `找不到_found為false`
  - `Request或欄位為null_found為false`
  - `清單依讀取模型的順序交回`
- [ ] **Step 2～4：** 跑測試確認失敗 → 實作 → `./mvnw -q verify` 通過。更新對照表：CQRS-1、CQRS-3 加上流程分析。Commit。

---

### Task 11：Projection 與記憶體版讀取模型（含統計方式 S1、S2）

**Files：**
- Create（`flow-analysis/interface-adapters/.../adapters/projection/`）：
  - `ReadModelStore`；
  - `InMemoryReadModelStore`；
  - `FlowProjection`；
  - package-private 的 `FlowBoardProjector`；
  - `WeekCalendar`；
  - `FlowBoardDocument`，內含 `ColumnEntry`、`WorkItemEntry` 兩個 record。
- Test：`FlowBoardProjectorTest`、`FlowProjectionTest`、`ReadModelStoreContract`（在 test-jar 裡）、`InMemoryReadModelStoreTest`、`WeekCalendarTest`。

**Interfaces：**
- Produces：
  - `public final class WeekCalendar(ZoneId zone)`（統計方式 S1，全系統唯一一份實作）：
    - `Instant weekStartOf(Instant t)`：t 所在那一週的週一 00:00，以 `zone` 的當地時間計。
    - `List<Instant> recentWeekStarts(Instant asOf, int count)`：含本週，由舊到新。
  - `public record FlowBoardDocument(String boardFlowId, String name, boolean archived, Integer stuckThresholdDays, List<ColumnEntry> columns, List<WorkItemEntry> workItems)`：
    - `ColumnEntry(String flowColumnId, String name, String stage, boolean removed, long dwellSecondsTotal, long dwellCount)`：沒被刪除的欄位依清單順序就是畫面順序。
    - `WorkItemEntry(String workItemId, String title, String flowColumnId, boolean stuck, Instant stuckSince)`
  - `public interface ReadModelStore extends FlowMetricsReader, BoardFlowListReader`：
    - `long checkpoint()`
    - `void inExclusiveTransaction(Runnable)`
    - `Optional<FlowBoardDocument> boardFlowDocument(String boardFlowId)`
    - `void saveBoardFlow(FlowBoardDocument)`
    - `void saveCompletion(String boardFlowId, String workItemId, Instant completedAt, long leadTimeSeconds)`：週的起點由儲存用 `WeekCalendar` 算好後存起來。
    - `void removeCompletion(String boardFlowId, String workItemId)`
    - `void advanceCheckpoint(long)`
    - `void reset()`
  - `InMemoryReadModelStore(WeekCalendar)`。
  - `flowMetrics(id, asOf)` 依統計方式 S2 計算：
    - 各欄的 `workItemCount`＝所在欄是這一欄的工作項目數。
    - `averageDwellSeconds`＝`dwellSecondsTotal / dwellCount`，四捨五入到整數秒；次數為 0 時是 null。
    - 平均一律四捨五入到整數秒（Java 用 `Math.round`，資料庫版用 `ROUND(AVG(x))::bigint`）。契約測試要放一個平均不是整數秒的案例，例如 1 秒與 2 秒的平均是 2，讓記憶體版和資料庫版的取整方式一致。
    - `recentWeeks`＝`recentWeekStarts(asOf, 8)` 每一週的完成數。
    - `delivery` 只算這 8 週內完成的工作項目：張數、平均、最長；沒有完成的時候兩個時間都是 null。
    - `stuckWorkItems`＝`stuck` 為 true 的工作項目，附所在欄的名稱。
  - `public final class FlowProjection(EventFeed feed, ReadModelStore store)`：
    - `catchUp()`、`rebuild()`，寫法照 `BoardProjection`。
    - 遇到 `WorkItemFlowResultChanged`：有完成時間就 `saveCompletion`，沒有就 `removeCompletion`。
    - 遇到 `WorkItemMoved`：把停留時間加進來源欄的 `dwellSecondsTotal`，`dwellCount` 加 1。
    - 這裡沒有任何「算不算」的判斷（設計第 0 節 4c）。

- [ ] **Step 1：寫失敗的測試。**
  - `FlowBoardProjectorTest`：16 種事件各一支。
  - `FlowProjectionTest`：
    - `從頭補齊`
    - `已處理過的事件再送一次_被略過_結果不變`
    - `重建後與重建前完全相同`
    - `遇到處理不了的事件_丟例外且checkpoint停在前一筆`
  - `WeekCalendarTest`（S1，時區 `Asia/Taipei`）：
    - `週日23點59分屬於上一週_週一0點屬於這一週`，台北時間的 `2026-11-15T23:59` 與 `2026-11-16T00:00`。
    - `最近8週含本週_由舊到新`
  - `ReadModelStoreContract`（S2）：
    - `平均與最長只算最近8週內完成的`
    - `沒有完成的_平均與最長是null`
    - `移出完成後_那一筆不再算`
    - `停留平均_只算已經離開的`
    - `各欄目前張數`
    - `卡住清單附所在欄名稱`
    - 另照看板管理那一份，加上 checkpoint、交易、reset 各項測試。
- [ ] **Step 2～4：** 跑測試確認失敗 → 實作 → `./mvnw -q verify` 通過。
  - 確認 `ONLY_PROJECTION_WRITES_READ_MODEL`（Task 3 補強版）有認到流程分析的 `ReadModelStore` 與它的寫入方法：在 `CqrsRulesTest` 加一支 `CQRS5_每個讀取模型儲存都被認到`，斷言規則認到的儲存型別包含 `kanban.flowanalysis.adapters.projection.ReadModelStore`。
  - 更新對照表：CQRS-5、ES-8、DDD-9（統計方式 S1、S2）加上流程分析。Commit。

---

### Task 12：防腐層——翻譯與同步

**Files：**
- Create（`flow-analysis/board-activity-translation/.../adapters/boardactivity/`）：`BoardActivityTranslation`、`BoardActivitySync`。
- Test：`BoardActivityTranslationTest`、`BoardActivitySyncTest`。
- Modify（architecture-tests）：`AdapterRules` 新規則 `TRANSLATION_ONLY_TRANSLATES`（規則目錄 B3），違規樣本 `translationleaks`。

**Interfaces：**
- Consumes：
  - Task 3：`ReadBoardActivitiesInput`、`ReadBoardActivitiesOutput`、`BoardActivity`、`BoardActivityKind`。
  - Task 8：`RecordBoardActivityInput`、`RecordBoardActivityOutput`、`RecordBoardActivityRequest`、`BoardChange`、`CommandResponse`。
- Produces：
  - `public final class BoardActivityTranslation`：私有建構子；`public static RecordBoardActivityRequest toRequest(BoardActivity activity)`。
    - 對 `activity.kind()` 用 switch 運算式，不寫 default。
    - 每一種對應到一個 `BoardChange`。欄位對應：`boardId` → `boardFlowId`，`columnId` → `flowColumnId`，`cardId` → `workItemId`，`cardTitle` → `workItemTitle`，`fromColumnId`／`toColumnId` → `fromFlowColumnId`／`toFlowColumnId`。
  - `public final class BoardActivitySync`：
    - 建構子 `(Function<ReadBoardActivitiesOutput, ReadBoardActivitiesInput> readActivities, Function<RecordBoardActivityOutput, RecordBoardActivityInput> recordActivity, ActivityPositionStore positions)`。
    - `public void syncOnce()`：
      1. 從 `positions.lastPosition()` 之後一次讀 100 筆，逐筆翻譯並記錄。
      2. 回覆成功才 `positions.advanceTo(該筆位置)`。
      3. `CONCURRENT_UPDATE` 立即重試，最多 3 次。
      4. 其他失敗、重試用完，或 `RuntimeException`：寫 ERROR 日誌（`System.Logger`），停在這一筆並 return。
      5. 讀不到新的就 return；有讀到就繼續下一批。
    - 兩個 Output 的實作是 `BoardActivitySync` 的 private static 巢狀類別：只有自己的巢狀類別才可以自己 `new`（`ONLY_MAIN_KNOWS_IMPLEMENTATIONS`）。

- [ ] **Step 1：寫失敗的測試。**
  - `BoardActivityTranslationTest`：11 種各一支，斷言 `BoardChange` 與欄位。
  - `BoardActivitySyncTest`：用 lambda 假造兩個 Input，記憶體版當讀取位置。
    - `每一筆成功_讀取位置前進到最後一筆`
    - `某一筆失敗_停在那一筆_之後的不送_位置不前進`（Review Focus 1）
    - `記錄時丟例外_停在那一筆_不往外丟`
    - `版本衝突_重試後成功_位置前進`
    - `版本衝突連續三次_停在那一筆`
    - `超過一批_全部送完`（250 筆）
    - `送出後當掉沒前進位置_下一輪重送同一筆`
- [ ] **Step 2～3：** 跑測試確認失敗 → 實作。
- [ ] **Step 4：本 Task 加入的架構規則：** `TRANSLATION_ONLY_TRANSLATES`（規則目錄 B3）。樣本 `translationleaks`：防腐層的類別引用 `adapters.projection` 的類別，並實作非 Output 的介面。
- [ ] **Step 5：** `./mvnw -q verify` 通過；更新對照表 DDD-3，附防腐層測試與規則名稱。Commit。

---

### Task 13：流程分析的 Controller、Presenter、ViewModel

**Files：**
- Create：
  - `flow-analysis/interface-adapters/.../adapters/controllers/`：`SetColumnStageController`、`SetStuckThresholdController`、`RemoveStuckThresholdController`、`CheckStuckWorkItemsController`、`ViewFlowMetricsController`、`ListBoardFlowsController`。
  - `…/adapters/presenters/`：
    - `CommandOutcome`、`CommandViewModel`、`CommandPresenter`，形狀照看板管理；
    - 四個指令 Presenter；
    - `FlowMetricsPresenter`、`FlowMetricsViewModel`、`FlowColumnViewModel`、`WeekViewModel`、`StuckWorkItemViewModel`、`StageOptionViewModel`；
    - `BoardFlowListPresenter`、`BoardFlowListViewModel`。
- Modify：flow adapters 的 `module-info`，exports `controllers`、`presenters`、`eventstore`、`projection`。
- Test：`ControllersTest`、`CommandPresenterTest`、`FlowMetricsPresenterTest`、`BoardFlowListPresenterTest`。

**Interfaces：**
- Produces：
  - `ViewFlowMetricsController.handle(String boardFlowId, Instant asOf)`；其餘 Controller 的 `handle` 參數照 Request 的欄位排。
  - `CommandPresenter` 的文字：

    | 失敗碼 | 結果 | 訊息 |
    |---|---|---|
    | `BOARD_FLOW_NOT_FOUND` | NOT_FOUND | 「流程分析還沒準備好，請稍後重新整理」 |
    | `FLOW_COLUMN_NOT_FOUND` | NOT_FOUND | 「找不到這個欄位」 |
    | `FLOW_COLUMN_REMOVED` | REJECTED | 「這個欄位已經刪除，不能再改階段」 |
    | `STUCK_THRESHOLD_OUT_OF_RANGE` | REJECTED | 「卡住門檻要是 1 到 365 天」 |
    | `UNKNOWN_STAGE` | REJECTED | 「不認得這個階段」 |
    | `INVALID_CHARACTERS` | REJECTED | 同看板管理的文字 |
    | `CONCURRENT_UPDATE` | CONFLICT | 同看板管理的文字 |
    | 其他 | REJECTED | 「請求內容不完整或格式不正確」 |

  - `FlowMetricsViewModel(boolean found, String boardFlowId, String boardName, String archivedLabel, boolean hasStuckThreshold, String stuckThresholdDays, String stuckThresholdLabel, List<StageOptionViewModel> stageOptions, List<FlowColumnViewModel> columns, String deliverySummary, List<WeekViewModel> weeks, List<StuckWorkItemViewModel> stuckWorkItems, String notFoundMessage)`
  - `FlowColumnViewModel(String flowColumnId, String name, String stage, String stageLabel, String workItemCount, String averageDwell)`、`WeekViewModel(String weekLabel, String completedLabel)`、`StuckWorkItemViewModel(String title, String flowColumnName, String since)`、`StageOptionViewModel(String value, String label)`
  - `FlowMetricsPresenter(ZoneId zone)` 的格式：
    - 時間長度：秒數 ÷ 86400，四捨五入到小數一位，加「 天」，例如「3.5 天」；null 顯示「—」。
    - 週：`MM/dd`；張數：「2 張」。
    - 卡住起點：`MM/dd HH:mm`。
    - 交付摘要：「最近 8 週：完成 12 張，交付時間平均 4.2 天、最長 9.0 天」；0 張時是「最近 8 週沒有完成的工作項目」。
    - 階段中文：未分類、未開始、進行中、完成；`stageOptions` 依這個順序列出四個。
    - 找不到時：`found=false`，`notFoundMessage`＝「流程分析還沒準備好，請稍後重新整理」，清單為空（Review Focus 3）。

- [ ] **Step 1：寫失敗的測試**：上面每一種格式至少一支，包含 null、0 張、時區（台北）、找不到。
- [ ] **Step 2～4：** 跑測試確認失敗 → 實作 → `./mvnw -q verify` 通過。`VIEWMODELS_ARE_DISPLAY_READY`、`CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES`、`ONE_PRESENTER_PER_OUTPUT` 會自動檢查新類別。更新對照表 CA-9、CA-11 加上流程分析。Commit。

---

### Task 14：PostgreSQL（兩個情境）

**Files：**
- 看板管理：
  - Create：`board-management/frameworks-drivers/src/main/resources/db/migration/V3__board_activity_log.sql`：`CREATE TABLE board_activity_log (position BIGINT PRIMARY KEY, doc JSONB NOT NULL)`。
  - Create：`board-management/interface-adapters/.../adapters/projection/PostgresBoardActivityLog`：lock 是 `7_238_116L`，checkpoint 名是 `'board-activity-log'`（同一張 `projection_checkpoint` 表），寫入用 `INSERT … ON CONFLICT (position) DO UPDATE`。
  - Test：`PostgresBoardActivityLogTest`（重跑契約測試）。
  - Modify：`WriteSideSchemaTest.寫入端只有事件表` 的清單加 `"board_activity_log"`（4d）。
- 流程分析：
  - Create：`db/flow-analysis/V2__event_store.sql`：`board_flow_events`，欄位與看板管理的事件表相同，有 `UNIQUE (stream_id, stream_version)` 與三個唯讀觸發程序。
  - Create：`db/flow-analysis/V3__read_model.sql`：
    - `board_flow_view(board_flow_id TEXT PRIMARY KEY, created_order BIGINT GENERATED ALWAYS AS IDENTITY, doc JSONB NOT NULL)`
    - `completed_work_item_view(board_flow_id TEXT, work_item_id TEXT, completed_at TIMESTAMPTZ NOT NULL, week_starts_at TIMESTAMPTZ NOT NULL, lead_time_seconds BIGINT NOT NULL, PRIMARY KEY (board_flow_id, work_item_id))`
    - `projection_checkpoint(name TEXT PRIMARY KEY, position BIGINT NOT NULL)`
  - Create：`flow-analysis/interface-adapters/.../adapters/eventstore/PostgresEventStore`（lock `7_238_214L`）。
  - Create：`…/adapters/projection/PostgresReadModelStore(DataSource, WeekCalendar)`（lock `7_238_215L`，checkpoint 名 `'flow-analysis'`；S2 用 SQL 的 `COUNT`、`AVG`、`MAX`，以 `week_starts_at` 篩選）。
  - Test（flow frameworks-drivers）：`PostgresEventStoreTest`、`PostgresReadModelStoreTest`（重跑契約測試）、`PostgresProjectionTest`、`FlowEventTableIsAppendOnlyTest`、`FlowAnalysisSchemaTest`（ES-1）。
- Modify（architecture-tests）：`EventSourcingRules.JDBC_USERS` 加 `"PostgresBoardActivityLog"`。流程分析的 `PostgresEventStore`、`PostgresReadModelStore` 用的是既有名稱，不用加。

- [ ] **Step 1：寫失敗的測試。**
  - `FlowAnalysisSchemaTest`：
    - `寫入端只有事件表`：`flow_analysis` 的表恰好是 `board_flow_events`、`board_flow_view`、`completed_work_item_view`、`projection_checkpoint`、`board_activity_position`、`flyway_schema_history`。
    - `事件表的欄位就是事件本身_沒有現況欄位`
    - `同一條串流的版本號不可重複`
  - `FlowEventTableIsAppendOnlyTest`：修改、刪除、清空都被拒絕。
  - 其餘的契約測試直接重跑。
- [ ] **Step 2～3：** 跑測試確認失敗 → 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過。更新對照表：ES-1、ES-2、ES-6、CA-6 加上兩個情境的資料庫版。Commit。

---

### Task 15：Spring 薄殼、背景排程、Main 組裝、整合測試

**Files：**
- 流程分析的 frameworks-drivers：
  - `kanban.flowanalysis.frameworks.web/`：`FlowAnalysisHttpEndpoints`、`FlowAnalysisRequests`（public interface）、`FlowAnalysisRequestBodies`、`FlowAnalysisBadRequestHandling`（`@RestControllerAdvice(basePackageClasses = FlowAnalysisHttpEndpoints.class)`）、`FlowAnalysisHttpStatusMapping`。
  - `kanban.flowanalysis.frameworks.scheduling/`：`FlowAnalysisBackgroundWork`（public interface）、`FlowAnalysisSchedule`（不加 `@Component`，由 Main 建立）。
- 看板管理：Modify `BadRequestHandling`：`@RestControllerAdvice` 改成 `@RestControllerAdvice(basePackageClasses = BoardHttpEndpoints.class)`（設計第 0 節第 3 點的裁決）。
- main：
  - Create：`DatabaseWiring`：把 `HikariDataSource` 從 `BoardManagementWiring.PostgresStorage` 搬過來，依序執行兩個情境的遷移。
  - Modify：`BoardManagementWiring`。
    - 新增 `BoardActivityLogStore` 的兩種儲存版本與 `BoardActivityProjection`。
    - `BoardActivitySync` 需要的 `output -> new ReadBoardActivitiesUseCase(boardActivityLogStore, output)` 寫在 `FlowAnalysisWiring`。跨情境的組裝只出現在 Main，Main 不受 B1 限制。
    - `boardRepository` 改成 `new EventSourcedBoardRepository(new NotifyingEventStore(new NotifyingEventStore(eventStore, projection::catchUp), activityProjection::catchUp))`。
    - 新增 `ApplicationRunner boardActivityStartupCatchUp`，寫法照 `startupCatchUp`。
  - Create：`FlowAnalysisWiring`：
    - `@EnableScheduling`；
    - Postgres 與記憶體兩種儲存版本，同樣用 `kanban.storage` 切換；
    - Bean 名稱一律加 `flow` 前綴；
    - `flowTimeSource`、`WeekCalendar`（時區用 `kanban.display-zone`）；
    - `FlowProjection` 與它的啟動補讀；
    - `BoardActivitySync`；
    - `FlowAnalysisSchedule`，加 `@ConditionalOnProperty(name = "kanban.flow-analysis.background", havingValue = "true", matchIfMissing = true)`。
  - Create：`FlowAnalysisRequestAssembly implements FlowAnalysisRequests`、`FlowAnalysisBackgroundAssembly implements FlowAnalysisBackgroundWork`。
  - Modify：`KanbanApplication.scanBasePackageClasses` 加 `FlowAnalysisRequests.class`。
  - Modify：`application.yml` 加：
    ```yaml
    flow-analysis:
      background: true
      sync-interval: PT2S
      stuck-check-interval: PT10M
    ```
  - Create：`main/src/test/resources/application.properties`：`kanban.flow-analysis.background=false`。
  - Test：`FlowAnalysisHttpIntegrationTest`、`FlowAnalysisConsistencyIntegrationTest`、`FlowAnalysisStartupIntegrationTest`、`BackgroundScheduleIntegrationTest`、`FlowAnalysisInMemoryModeIntegrationTest`、`FlowAnalysisTestClock`（`@TestConfiguration`，`@Primary` 可調整的流程分析 `TimeSource`）。
- Modify（architecture-tests）：
  - `FrameworkRules` 新規則 `SCHEDULES_ONLY_DELEGATE`（規則目錄 B4），違規樣本 `schedulesmart`。
  - `ENDPOINTS_ONLY_DELEGATE` 的禁止清單加 `"..adapters.boardactivity.."`，違規樣本 `endpointusestranslation`。
  - `DddRules` 補強 Gateway 辨認（規則目錄 B5），違規樣本 `gatewayunnamed`。

**Interfaces：**
- Produces：
  - HTTP：`@RequestMapping("/api/flow-analysis/boards")`

    | 方法與路徑 | 呼叫 | 回覆 |
    |---|---|---|
    | `GET /{boardId}` | `viewFlowMetrics(boardId)` | found 時 200，否則 404 |
    | `PUT /{boardId}/columns/{columnId}/stage`，body `{"stage": "IN_PROGRESS"}` | `setColumnStage` | 依結果 |
    | `PUT /{boardId}/stuck-threshold`，body `{"days": 5}` | `setStuckThreshold` | 依結果 |
    | `DELETE /{boardId}/stuck-threshold` | `removeStuckThreshold` | 依結果 |

    狀態碼對應同看板管理：SUCCEEDED 200、REJECTED 422、NOT_FOUND 404、CONFLICT 409。
  - `FlowAnalysisBackgroundWork`：`void syncBoardActivities()`、`BoardFlowListViewModel listBoardFlows()`、`CommandViewModel checkStuckWorkItems(String boardFlowId)`。
  - `FlowAnalysisSchedule(FlowAnalysisBackgroundWork work)`：
    - `@Scheduled(fixedDelayString = "${kanban.flow-analysis.sync-interval}") void sync()`
    - `@Scheduled(fixedDelayString = "${kanban.flow-analysis.stuck-check-interval}") void checkStuck()`：對清單裡每個識別碼呼叫一次 `checkStuckWorkItems`。
  - `FlowAnalysisRequestAssembly.viewFlowMetrics` 用 `flowTimeSource.now()` 當 `asOf`。

- [ ] **Step 1：寫失敗的測試。**
  - `FlowAnalysisConsistencyIntegrationTest`（設計第 7.6 節，六種情況各一支，加兩支）：
    - `情況1_看板上操作後_看板動態已補到最新_同步一次後流程分析看得到`
    - `情況2_改設定後立刻查詢_看得到重算後的數字`
    - `情況3_待滿門檻_檢查後列入卡住_換欄後下一次同步就移出`：用 `FlowAnalysisTestClock` 把流程分析的時間調到 6 天後。
    - `情況4_背景同步失敗_之前的數字照常_看板管理照常_下一輪補上`：`@MockitoSpyBean` 流程分析的 `EventStore`，丟例外一次。
    - `情況4b_看板動態紀錄沒更新成功_看板指令照樣成功_下一次看板指令後補上`：spy `BoardActivityLogStore.append`。
    - `情況5_背景同步碰到版本衝突_重試後成功_使用者碰到版本衝突_回覆409`
    - `看板動態紀錄清空重建後_位置不變`（ES-8）
  - `FlowAnalysisStuckSyncIntegrationTest`：
    - 設 `properties = "kanban.storage=memory"`，標 `@DirtiesContext`。
    - 測試 `動態引用不認得的工作項目_同步停在那一筆_之前的數字照常_看板管理照常`（Review Focus 1）。
    - 做法：用 `@MockitoSpyBean` 包住 `BoardActivityLogStore`，讓 `activitiesAfter` 在真實結果後面多回一筆引用不存在卡片的動態，位置取真實的最後一筆加 1。
    - 不往共用資料庫寫假資料。
  - `FlowAnalysisHttpIntegrationTest`：
    - `完整流程_設定階段與門檻_數字正確`
    - `還沒同步的看板_404且有中文訊息`（Review Focus 3）
    - `請求內容缺欄位或讀不懂_422且有中文訊息`
    - `階段字串不認得_422`
    - `門檻0與366與非數字_422`
    - `網址裡的識別碼含控制字元_不是500`（Review Focus 5）
    - `看板管理的端點_請求讀不懂時仍回看板管理的訊息`：證明 `BadRequestHandling` 的範圍改了之後行為不變。
  - `FlowAnalysisStartupIntegrationTest`：`第一階段的資料庫第一次升級_看板動態補齊_流程分析同步到全部歷史`（Review Focus 4）。啟動前先寫看板事件，並清空 `board_activity_log` 與名稱 `'board-activity-log'` 的 checkpoint。
  - `BackgroundScheduleIntegrationTest`：
    - 設 `properties = {"kanban.flow-analysis.background=true", "kanban.flow-analysis.sync-interval=PT0.2S"}`，標 `@DirtiesContext`。
    - 測試 `看板上操作後_不手動同步_幾秒內流程分析看得到`：最多等 10 秒，每 100 毫秒查一次。
  - `ApplicationYamlTest`（不啟動 Spring 的單元測試）：`正式設定檔的排程開關是字串true`。
    - 用 `new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))` 讀 main 的正式設定檔。
    - 斷言 `kanban.flow-analysis.background` 轉成字串後等於 `"true"`，證明 YAML 的寫法對得上 `havingValue = "true"`。
    - 上面那支排程測試的開關是行內帶入的，證明不到這件事。
  - `FlowAnalysisInMemoryModeIntegrationTest`：設 `kanban.storage=memory`，跑完整流程。
- [ ] **Step 2～3：** 跑測試確認失敗 → 實作。
- [ ] **Step 4：本 Task 加入的架構規則：**
  - `SCHEDULES_ONLY_DELEGATE`（B4）；
  - `ENDPOINTS_ONLY_DELEGATE` 加禁止 `adapters.boardactivity`；
  - Gateway 辨認補強（B5）。
  
  補強前先跑新的咬人測試，確認它失敗。
- [ ] **Step 5：** `./mvnw -q verify` 通過。第一階段既有的整合測試必須全部照舊通過。更新對照表：CQRS-6（附 8 支測試名稱）、CA-7、CA-8、DDD-3。Commit。

---

### Task 16：流程分析的畫面與看板頁入口

**Files：**
- Create：`flow-analysis/web-ui/`：
  - `package.json`：name `@kanban/flow-analysis-web-ui`，`exports: { ".": "./src/index.ts" }`。
  - `tsconfig.json`、`vitest.config.ts`。
  - `src/index.ts`：export `FlowAnalysisView`。
  - `src/api/flowAnalysisApi.ts`、`src/api/viewModels.ts`、`src/api/serviceUnavailable.ts`。
  - `src/views/FlowAnalysisView.vue`。
  - `src/components/StageTable.vue`、`src/components/StuckList.vue`。
- Create：`main/web-ui/src/BoardPage.vue`（「流程分析」連結加 `BoardView`）、`main/web-ui/src/FlowAnalysisPage.vue`（「回到看板」連結加 `FlowAnalysisView`）。
- Modify：
  - `main/web-ui/src/router.ts`：`/boards/:boardId` 改用 `BoardPage`；新增 `/boards/:boardId/flow` → `FlowAnalysisPage`。
  - `main/web-ui/package.json`：加流程分析 workspace 的相依。
  - 根目錄 `package.json`：workspaces 與 lint 路徑加 `flow-analysis/web-ui`。
- Test：`flowAnalysisApi.spec.ts`、`FlowAnalysisView.spec.ts`、`main/web-ui/src/__tests__/BoardPage.spec.ts`。

**Interfaces：**
- Consumes：Task 13 的 ViewModel 欄位（TypeScript 型別逐欄對應）、Task 15 的 HTTP 端點。
- Produces：
  - 畫面只顯示 ViewModel 的字串，不做格式轉換、不做判斷（CA-9，ESLint 的格式化限制也套用在這裡）。
  - 送出指令後重讀；被拒絕時把 `message` 顯示在訊息列。

- [ ] **Step 1：寫失敗的測試。**
  - `FlowAnalysisView.spec.ts`：
    - `顯示各欄階段張數與平均停留_原樣顯示ViewModel字串`
    - `改階段_送出欄位與階段後重讀`
    - `儲存門檻_送出天數`
    - `取消門檻`
    - `被拒絕_顯示訊息`
    - `找不到_顯示notFoundMessage`（Review Focus 3）
    - `後端出錯_顯示系統錯誤`
  - `BoardPage.spec.ts`：`看板頁有流程分析連結_指向該看板`。
  - `flowAnalysisApi.spec.ts`：照 `boardApi.spec.ts` 的六種情況寫。
- [ ] **Step 2～3：** 跑測試確認失敗 → 實作。
- [ ] **Step 4：** 在根目錄跑 `npm run lint && npm test && npm run build`，全部通過。`contextBoundaries.spec.ts` 照舊通過。
- [ ] **Step 5：** 更新對照表 CA-9（前端），審查清單的 CA-9 項目留到 Task 17 審查。Commit。

---

### Task 17：收尾——審查、對照表定稿、瀏覽器實測

- [ ] **Step 1：** 跑 `./mvnw -B verify` 與根目錄的 `npm ci && npm run lint && npm test && npm run build`，全部通過。數 surefire 報告的 `Tests run:` 行，確認沒有 failure 或 error，不靠 exit code。
- [ ] **Step 2：審查清單**（`docs/review-checklist.md`）。逐項審查並記進「審查紀錄」：
  - CA-9 前端；
  - CA-11 的 SRP／OCP／LSP；
  - CA-13、CA-15、DDD-1 用詞、DDD-5、DDD-9（含 4c 統計方式）；
  - DDD-3 防腐層只翻譯。
- [ ] **Step 3：瀏覽器實測（L4，chrome-devtools）。** 用 `docker compose up -d` 啟動 PostgreSQL，再啟動後端與 `npm run dev -w main/web-ui`。
  - 第一階段的每一項操作重走一遍，因為前端進入點搬家了：
    - 建看板、改名、封存；
    - 欄位的新增、改名、拖曳排序、上限、刪除；
    - 卡片的新增、編輯、拖曳換欄與排序、超過上限被拒絕、刪除；
    - 卡片歷程。
  - 第二階段：
    - 從看板頁進入流程分析；
    - 替每一欄指定階段，數字跟著變；
    - 在看板上移動卡片，約 2 秒內流程分析的數字跟著變；
    - 改階段後整段歷史重算；
    - 設定與取消卡住門檻；
    - 已刪除的欄位改階段被拒絕的訊息；
    - 還沒同步的看板顯示「還沒準備好」。
  - 每一項截圖存在 scratchpad，console 不能有錯誤。
- [ ] **Step 4：** 對照表定稿：39 列全部「已驗證」，沒有「已實作未驗證」或「尚未實作」。
  - CA-14 加上設計第 10 節。
  - 對照表開頭那段「新增業務情境時還要做兩件事」，改寫成第二個情境實際做過的完整清單。
  - `README.md` 補上流程分析：模組樹、端點、背景排程的設定值。
- [ ] **Step 5：** 整個分支交給一位全新的審查者（最強的模型），附本計畫的 Review Focus。Critical／Important 的問題各寫一支先失敗的測試再修，修完重跑 Step 1。L4 之後若有改程式，重做受影響的 L4 項目。
- [ ] **Step 6：** Commit。不推送，回報使用者。

---

## 規則目錄（新增與補強）

本計畫新增的規則都放在 `architecture-tests/src/test/java/kanban/architecture/rules/` 既有的檔案裡，四件一組寫法同第一階段。

**B1 `DddRules.CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE`（DDD-3、CA-16）**
- 工廠方法：`contextsIntegrateOnlyThroughPublishedLanguage(String root, Map<String, Set<String>> publishedLanguages, Map<String, Map<String, String>> translators)`。
- 正式版的參數：
  - `root` 是 `"kanban"`；
  - `publishedLanguages = Map.of("boardmanagement", Set.of("usecases.queries.readboardactivities"))`；
  - `translators = Map.of("flowanalysis", Map.of("boardmanagement", "adapters.boardactivity"))`。
- 判定：情境＝`root` 之後的第一段，`main` 豁免。情境 X 的類別相依情境 Y（X≠Y）的類別時，必須同時滿足兩件事，否則違規：
  1. 自己在 `root.X.<translators[X][Y]>..` 裡；
  2. 目標在 `root.Y.<publishedLanguages[Y] 其中之一>` 這個套件裡（不含子套件）。
- 結果：上游引用下游一律違規，因為對照表沒有這個方向。
- `docs/context-map.md` 要寫明這兩張表是情境關係的程式版。

**B2 補強 `CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL`（CQRS-5）**
- 「讀取模型儲存」＝原本的條件（簡名是 `ReadModelStore`，或實作它），**或者**放在 `..adapters.projection..`、而且可以指派給某個 `..usecases.queries..` 介面的型別。
- 「寫入方法」＝原本的 `READ_MODEL_WRITES`，**或者**讀取模型儲存型別自己宣告、但沒有任何 `..usecases.queries..` 介面宣告過的方法（比對名稱與參數型別）。
- 其餘判定不變：呼叫寫入方法者的簡名要以 `Projection` 結尾；相依讀取模型儲存者要在 `projection` 套件。
- 另加測試 `CQRS5_每個讀取模型儲存都被認到`（Task 11）。

**B3 `AdapterRules.TRANSLATION_ONLY_TRANSLATES`（DDD-3、CA-11）**
- `..adapters.boardactivity..` 的類別只能相依這些套件：
  - `java..`、`javax.sql..`；
  - `..adapters.boardactivity..`；
  - `..usecases.commands.recordboardactivity`；
  - `..usecases.commands`（`CommandResponse`、`BoardChange`）；
  - `..usecases.queries.readboardactivities`。
- 實作的介面只能是：以上 Use Cases 套件裡簡名以 `Output` 結尾的介面、自己套件的 `ActivityPositionStore`。
- 用 JDBC 的限制交給 `JDBC_ONLY_IN_POSTGRES_STORES` 與 `JDBC_USERS`。

**B4 `FrameworkRules.SCHEDULES_ONLY_DELEGATE`（CA-7、CA-9）**
- `..frameworks.scheduling..` 不得相依 `"..usecases.."`、`"..entities.."`、`"..adapters.controllers.."`、`"..adapters.eventstore.."`、`"..adapters.projection.."`、`"..adapters.system.."`、`"..adapters.boardactivity.."`。
- 內容和 `ENDPOINTS_ONLY_DELEGATE` 相同，管的是排程。

**B5 補強 `Shapes.isGatewayInterface`（DDD-8、CA-6）**
- 原本的名稱條件，**或者**同時符合以下全部：
  - 是介面；
  - 在 `usecases` 套件、top-level；
  - 不在 `impl`、`support`；
  - 簡名不以 `Input`、`Output` 結尾。
- `REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE` 跟著變嚴：所有 Gateway 的實作都要在 `adapters`。
- 違規樣本 `gatewayunnamed`：Use Cases 圈的 `ThingStore` 由 `frameworks` 的類別實作。

## 計畫層的裁決（已回寫設計文件，2026-11-17）

1. F2 與 F4 在「從完成欄移到未分類欄」時互相矛盾，取 F2：未分類對判斷是透明的。
2. F15 補充：時間倒退時，停留與交付時間以 0 計。
3. 動態引用不認得的欄位或工作項目時，同步停住並寫日誌，不默默略過。
4. 看板管理的 `BadRequestHandling` 範圍縮到自己的端點。這是唯一修改既有程式的一處，行為不變。
5. 前端用 npm workspaces，根目錄多一個只列 workspaces 的 `package.json`。
6. 內部物件名稱多了 `ColumnVisit`、`FlowResult`，和 4d 同一類擴充。
