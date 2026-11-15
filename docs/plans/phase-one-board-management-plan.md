# 第一階段「看板管理」實作計畫

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal：** 做出一個可在瀏覽器操作的看板系統（看板、欄位、卡片、在製品上限、卡片移動歷程），並讓 `CLAUDE.md` 的 39 條規則中 38 條到達「已驗證」（DDD-3 留到第二階段）。

**Architecture：** 一個業務情境 `board-management`，四圈各是獨立 Maven 模組，內三圈加 `module-info`。寫入端是事件溯源的單一「看板」聚合；查詢端是 Projection 維護的讀取模型；Spring 只在最外圈，Main 負責組裝。

**Tech Stack：** Java 25、Maven（wrapper）、Spring Boot 4.1.2、ArchUnit 1.5.1、JUnit（Spring Boot 管理的版本）、AssertJ、Jackson（Spring Boot 管理的版本）、PostgreSQL（Docker）、Testcontainers 2.0.5、Flyway、Vue 3.5＋Vite 8＋TypeScript＋Vitest、GitHub Actions。

**Spec：** `docs/design/phase-one-board-management.md`（執行者必須連同專案根目錄 `CLAUDE.md` 一起讀）。

## Global Constraints

- `CLAUDE.md` 的 39 條規則全部是硬規則。任何一條覺得做不到 → 停下來回報，NEVER 自行放寬、NEVER 刪除或停用架構測試。
- 套件根：`kanban.boardmanagement`。模組目錄：`board-management/{entities,use-cases,interface-adapters,frameworks-drivers,web-ui}`、`main`、`architecture-tests`。
- 各圈可用的東西：`entities` 只有 JDK；`use-cases` 只有 JDK＋`entities`；`interface-adapters` 只有 JDK（含 `java.sql`）＋內兩圈＋Jackson；Spring、Jakarta、資料庫驅動程式、連線池、Flyway 只能在 `frameworks-drivers` 與 `main`。
- 內三圈都有 `module-info.java`；`frameworks-drivers`、`main`、`architecture-tests` 沒有。
- 聚合不讀時鐘、不產生亂數；時間與識別碼由 Use Case 傳入。
- 聚合測試一律 Given 事件／When 指令／Then 事件或拒絕，使用 Task 4 的 `BoardScenario`。
- 每條 ArchUnit 規則都要有一組故意違規的樣本類別，並有測試證明規則遇到它會失敗。
- **規則跟著結構走（`CLAUDE.md` 第 6 節第 4 點）**：每個 Task 引進新結構時，同一個 Task、同一次 commit 內就要加入守住它的架構規則（規則＋對正式程式碼的測試＋違規樣本＋咬人測試）。每個 Task 的「本 Task 加入的架構規則」列出名稱，規則內容查文末「規則目錄 A、B」。Task 編號 6 與 13 從缺（原內容已改成規則目錄）。
- **動手改程式前先講三件事（`CLAUDE.md` 第 6 節第 3 點）**：每個 Task 開始時，先用一兩句話說明這次改動屬於哪一圈、哪個業務情境、指令端還是查詢端。
- **每次 `git commit` 之前**，先用白話講這次改動解決什麼、影響誰、使用者會感覺到什麼差異，再執行 commit。
- Use Cases 圈的套件名稱採允許清單（CA-10）：`usecases` 之下只有 `commands`、`queries`、`eventfeed`；`commands` 之下只有 14 個 Use Case 業務名稱加上 `gateway`、`support`；`queries` 之下只有 3 個 Use Case 業務名稱；每個 Use Case 套件之下只允許 `impl`。`gateway`、`support`、`eventfeed`、`impl` 這四個結構性名稱是計畫作者的判斷，已請使用者過目。
- PostgreSQL 測試 NEVER 停用或繞過事件表的唯讀觸發程序。需要乾淨資料庫時，整個 schema 砍掉後重跑 Flyway。
- 每個 Task 結束前更新 `docs/traceability.md` 對應的列（狀態只有：已驗證（附測試名稱）、已實作未驗證、尚未實作）。
- 新的領域名詞先加進 `docs/glossary.md` 再寫程式。
- 字數上限以 Unicode code point 計：名稱與標題 100、卡片說明 2000。
- Git：在分支 `phase-one-board-management` 上工作；每個 Task 結束做一次本機 commit（訊息用繁體中文，結尾附 session 指定的 attribution）。**不推送、不開 PR**，除非使用者另外說。
- 每個 Task 的最後驗證指令都是 `./mvnw -q verify`（全部模組），不是只跑單一測試。

## Review Focus

設計文件沒明講、但使用者很可能碰到的五種情況。每一條都已在對應 Task 加了測試。

1. 請求內容缺欄位、型別不對、位置是負數（例如送出空的 JSON）→ 回覆 422 與中文訊息，不可以是 500。（Task 15）
2. 對不存在的看板下指令或查詢 → 回覆「找不到看板」，不可以丟例外。（Task 8、10、15）
3. 名稱只有空白字元、剛好 100 字、101 字、含 emoji（一個 emoji 算一個字）。（Task 3）
4. 系統啟動時事件表有資料但讀取模型是空的或落後 → 啟動後自動補齊。（Task 15）
5. 事件表出現這一版程式不認得的事件類型 → 明確報錯並停止投影，不可以默默略過。（Task 9、11）

---

### Task 0：分支與既有文件入庫

**Files：** 既有的 `CLAUDE.md`、`docs/design/phase-one-board-management.md`、`docs/plans/phase-one-board-management-plan.md`

- [ ] **Step 1：** `git switch -c phase-one-board-management`
- [ ] **Step 2：** 把上述三個檔案 commit（訊息：「加入專案規範、第一階段設計文件與實作計畫」）。
- [ ] **Step 3：** `git status --short` 輸出為空。

---

### Task 1：骨架驗證（`module-info` 能不能和 Maven、ArchUnit、Spring Boot 一起運作）

這個 Task 是設計文件 3.2 節指定的關卡。**任何一步無法通過且找不到合規解法 → 停止整個計畫並回報使用者**，NEVER 自行拿掉 `module-info`。

**Files：**
- Create：`pom.xml`（root，parent 為 `spring-boot-starter-parent:4.1.2`，`java.version=25`，列出全部模組）、`mvnw`、`.mvn/`
- Create：`board-management/entities/pom.xml`、`board-management/use-cases/pom.xml`、`board-management/interface-adapters/pom.xml`、`board-management/frameworks-drivers/pom.xml`、`main/pom.xml`、`architecture-tests/pom.xml`
- Create：三個 `module-info.java`
- Create：每個模組一個暫時的探針類別（Task 3 起陸續取代）
- Create：`architecture-tests/src/test/java/kanban/architecture/rules/CleanArchitectureRules.java`、`DddRules.java`、對應的 `*RulesTest`（對正式程式碼）、`*RulesBiteTest`（對違規樣本）、樣本 `kanban/fixtures/…`
- Create：`board-management/frameworks-drivers/src/test/java/.../DockerProbeTest.java`
- Modify：`.gitignore`（加 `target/`、`node_modules/`、`dist/`）

**Interfaces：**
- Produces：
  - 模組名稱：`kanban.boardmanagement.entities`、`kanban.boardmanagement.usecases`、`kanban.boardmanagement.adapters`
  - Maven artifactId：`board-management-entities`、`board-management-use-cases`、`board-management-interface-adapters`、`board-management-frameworks-drivers`、`kanban-main`、`architecture-tests`；groupId `kanban`
  - `entities`、`use-cases` 的 pom 內有 Maven Enforcer `bannedDependencies`：禁止任何 compile／runtime scope 的相依（`use-cases` 只允許 `kanban:board-management-entities`）
  - `main` 啟動類別 `kanban.main.KanbanApplication`

- [ ] **Step 1：** 建立 root pom、六個模組 pom、wrapper（`mvn wrapper:wrapper`）。
- [ ] **Step 2：** 每一圈放一個探針：`entities` 一個公開 record；`use-cases` 在已公開套件放一個介面、在 `…usecases.probe.impl`（不公開）放它的實作；`interface-adapters` 放一個只引用該介面的類別；`main` 用 `new` 建立實作並接起來。
- [ ] **Step 3：** `module-info`：`entities` 公開自己的套件；`use-cases` `requires transitive kanban.boardmanagement.entities`，只公開介面所在套件；`interface-adapters` `requires` 內兩圈、`java.sql` 與 Jackson databind 的模組（先跑 `./mvnw -q dependency:tree -pl main` 確認 Spring Boot 4.1.2 管理的 Jackson 座標與模組名稱，再寫進 pom 與 `module-info`）。
- [ ] **Step 4：** 每個內圈模組各寫一支 JUnit 測試（確認測試在有 `module-info` 的模組裡跑得起來）。
- [ ] **Step 5：本 Task 加入的架構規則**（內容見規則目錄）：`RINGS_DEPEND_INWARD`、`ENTITIES_USE_ONLY_JDK`、`USECASES_USE_ONLY_JDK_AND_ENTITIES`、`ADAPTERS_FREE_OF_FRAMEWORKS`、`SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING`、`NO_PACKAGE_CYCLES`、`TOP_LEVEL_PACKAGES_SCREAM`、`CONTEXTS_DO_NOT_SHARE_DOMAIN`。四件式寫法在這裡定下來，之後每個 Task 沿用：規則是 `rules` 套件內的 `public static final ArchRule`；`*RulesTest` 對 `kanban.boardmanagement..` 與 `kanban.main..` 跑；`*RulesBiteTest` 對 `kanban.fixtures..` 的違規樣本跑，每條規則一支，斷言 `RULE.check(...)` 丟 `AssertionError`。另加一支測試斷言 ArchUnit 讀到的正式類別數大於 0（確認讀得到模組化 jar）。
- [ ] **Step 5b：** `DockerProbeTest`：用 Testcontainers 啟動一個 PostgreSQL 容器並執行 `SELECT 1`（提早確認 Testcontainers 2.0.5 與本機 Docker 搭得起來，不要等到 Task 14 才發現）。
- [ ] **Step 6：** `main` 加 `spring-boot-starter-web`；寫一支 `@SpringBootTest` 確認應用程式能啟動。
- [ ] **Step 7：** 跑 `./mvnw -q verify`。Expected：BUILD SUCCESS。
- [ ] **Step 8：反向證明（三件，各做一次後還原）**
  - 在 `interface-adapters` 的探針裡 `import` `…usecases.probe.impl` 的實作類別 → Expected：編譯失敗，訊息含 `is not visible` 或 `does not export`。
  - 在 `entities` 的 pom 加一個 compile 相依（例如 `org.slf4j:slf4j-api`）→ Expected：Enforcer 讓建置失敗。
  - 在 `entities` 的探針加 `import org.springframework.stereotype.Component;` → Expected：編譯失敗（找不到套件）。
  把三次失敗訊息各擷取一行，記進 commit 訊息本文。
- [ ] **Step 9：** Commit。

---

### Task 2：用語表、對照表、情境關係、審查清單

**Files：**
- Create：`docs/glossary.md`、`docs/traceability.md`、`docs/context-map.md`、`docs/review-checklist.md`
- Create：`architecture-tests/src/test/java/kanban/architecture/GlossaryTest.java`

**Interfaces：**
- Produces：
  - `docs/glossary.md` 是一張 Markdown 表格，欄位固定為 `| 中文用語 | 程式名稱 | 種類 | 意思 |`；「程式名稱」欄一格可放多個名稱，用 `／` 分隔。
  - `GlossaryTest` 的規則：`kanban.boardmanagement.entities..` 所有公開頂層型別的簡名，以及 `kanban.boardmanagement.usecases.commands..` 所有以 `Request` 結尾的型別去掉 `Request` 後的名稱，都必須出現在「程式名稱」欄。
  - `docs/traceability.md`：39 列，欄位 `| 規則編號 | 落在哪個模組或類別 | 由哪一支測試或哪一項檢查守住 | 目前狀態 |`，初始全部「尚未實作」；CA-14 填「設計文件第 10 節」並標「已驗證」。
  - `docs/review-checklist.md`：每一項對應一個規則編號（CA-9 前端、CA-11 的 SRP／OCP／LSP、CA-13、CA-15、DDD-1 用詞、DDD-5、DDD-9），文件末尾有「審查紀錄」表：`| 日期 | 審查範圍（commit）| 項目 | 結果 | 備註 |`。

- [ ] **Step 1：** 寫四份文件。用語表先放設計文件第 5、6 節與附錄 A 的全部名稱（9 個值物件、`Board`、`Column`、`Card`、14 個事件、14 個指令、`BoardEvent`、`RejectionReason`、`BusinessRuleViolation`、`Archive`）。
- [ ] **Step 2：** 寫 `GlossaryTest`，另加一支測試：對一個刻意不在用語表裡的樣本名稱（`kanban.fixtures.glossary.UnlistedThing`），檢查函式回報違規。
- [ ] **Step 3：** `./mvnw -q verify` 通過（此時 entities 只有探針；把探針名稱暫列入用語表，Task 3 移除探針時一併拿掉）。
- [ ] **Step 4：** Commit。

---

### Task 3：值物件與拒絕原因

**Files：**
- Create：`board-management/entities/src/main/java/kanban/boardmanagement/entities/board/` 下：`BoardId`、`ColumnId`、`CardId`、`BoardName`、`ColumnName`、`CardTitle`、`CardDescription`、`WipLimit`、`Position`、`RejectionReason`、`BusinessRuleViolation`
- Test：同模組 `src/test/java/.../board/ValueObjectsTest.java`
- Delete：Task 1 的 entities 探針

**Interfaces：**
- Produces（全部是 `public record`，在建構時驗證，失敗丟 `BusinessRuleViolation`）：
  - `BoardId(String value)`、`ColumnId(String value)`、`CardId(String value)`：不可 null 或空白 → `IDENTIFIER_BLANK`
  - `BoardName(String value)`、`ColumnName(String value)`、`CardTitle(String value)`：去頭尾空白後存；空白 → `NAME_BLANK`；超過 100 個 code point → `NAME_TOO_LONG`
  - `CardDescription(String value)`：null 視為空字串；超過 2000 個 code point → `DESCRIPTION_TOO_LONG`
  - `WipLimit(int value)`：小於 1 → `WIP_LIMIT_NOT_POSITIVE`
  - `Position(int index)`：從 0 起算；小於 0 → `POSITION_OUT_OF_RANGE`
  - `enum RejectionReason { IDENTIFIER_BLANK, NAME_BLANK, NAME_TOO_LONG, DESCRIPTION_TOO_LONG, WIP_LIMIT_NOT_POSITIVE, POSITION_OUT_OF_RANGE, BOARD_ARCHIVED, COLUMN_NOT_FOUND, CARD_NOT_FOUND, COLUMN_NAME_DUPLICATED, COLUMN_NOT_EMPTY, WIP_LIMIT_EXCEEDED, WIP_LIMIT_BELOW_CARD_COUNT }`
  - `final class BusinessRuleViolation extends RuntimeException { public RejectionReason reason(); }`

- [ ] **Step 1：寫失敗的測試**（`ValueObjectsTest`），至少包含：
  - `new BoardName("   ")` → `NAME_BLANK`
  - `new BoardName("  產品開發 ").value()` 等於 `"產品開發"`
  - 100 個「字」可建立；101 個 → `NAME_TOO_LONG`
  - 100 個 `"😀"` 可建立（Review Focus 3）；101 個 → `NAME_TOO_LONG`
  - `new CardDescription(null).value()` 等於 `""`；2001 字 → `DESCRIPTION_TOO_LONG`
  - `new WipLimit(0)` → `WIP_LIMIT_NOT_POSITIVE`；`new Position(-1)` → `POSITION_OUT_OF_RANGE`
  - 兩個相同值的 `ColumnId` 相等
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 3b：本 Task 加入的架構規則：** `VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS`（此時只涵蓋值物件）、`NO_CLOCK_OR_RANDOM_IN_ENTITIES`。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表 DDD-6、CA-2（已驗證並附測試與規則名稱）。
- [ ] **Step 5：** Commit。

---

### Task 4：領域事件、看板聚合（看板與欄位的行為）、聚合測試工具

**Files：**
- Create：`…/entities/board/events/` 下 `BoardEvent`（sealed interface）與 14 個事件 record
- Create：`…/entities/board/Board.java`（`Column`、`Card` 是它的非公開內部類別或同套件非公開類別）
- Test：`…/entities/src/test/java/.../board/BoardScenario.java`、`BoardLifecycleTest.java`、`ColumnRulesTest.java`

**Interfaces：**
- Produces：
  - `sealed interface BoardEvent { BoardId boardId(); Instant occurredAt(); }`
  - 事件 record（欄位順序即建構子順序，`occurredAt` 一律最後）：
    `BoardCreated(boardId, BoardName name, occurredAt)`、`BoardRenamed(boardId, BoardName name, occurredAt)`、`BoardArchived(boardId, occurredAt)`、`ColumnAdded(boardId, ColumnId columnId, ColumnName name, occurredAt)`、`ColumnRenamed(boardId, columnId, ColumnName name, occurredAt)`、`ColumnMoved(boardId, columnId, Position toPosition, occurredAt)`、`ColumnWipLimitSet(boardId, columnId, WipLimit wipLimit, occurredAt)`、`ColumnWipLimitRemoved(boardId, columnId, occurredAt)`、`ColumnRemoved(boardId, columnId, occurredAt)`、`CardAdded(boardId, CardId cardId, columnId, CardTitle title, CardDescription description, occurredAt)`、`CardContentChanged(boardId, cardId, title, description, occurredAt)`、`CardMoved(boardId, cardId, ColumnId fromColumnId, ColumnId toColumnId, Position toPosition, occurredAt)`、`CardRepositioned(boardId, cardId, columnId, Position toPosition, occurredAt)`、`CardRemoved(boardId, cardId, columnId, occurredAt)`
  - `Board`（`public final class`，沒有公開建構子、沒有 setter、不外露內部集合）：
    - `static Board create(BoardId id, BoardName name, Instant at)`
    - `static Board rehydrate(List<BoardEvent> history)`（`history` 不可為空）
    - `BoardId id()`、`long persistedVersion()`（＝`rehydrate` 時的事件數；`create` 後為 0）、`List<BoardEvent> uncommittedEvents()`（不可變副本）、`void markCommitted()`
    - 本 Task 的指令方法：`rename(BoardName, Instant)`、`archive(Instant)`、`addColumn(ColumnId, ColumnName, Instant)`、`renameColumn(ColumnId, ColumnName, Instant)`、`moveColumn(ColumnId, Position, Instant)`、`setColumnWipLimit(ColumnId, WipLimit, Instant)`、`removeColumnWipLimit(ColumnId, Instant)`、`removeColumn(ColumnId, Instant)`
    - 結構：每個指令方法只做「檢查 → 建立事件 → `record(event)`」；`record` 把事件加進未提交清單並呼叫私有的 `apply(BoardEvent)`；`apply` 用 `switch` 分派到各事件的私有套用方法，套用方法只改欄位。`rehydrate` 只呼叫 `apply`。
  - 測試工具 `BoardScenario`：
    ```java
    BoardScenario.given(BoardEvent... history)
        .when(board -> board.addColumn(...))      // Consumer<Board>
        .thenEvents(BoardEvent... expected);      // 與 uncommittedEvents() 完全相等
        // 或 .thenRejected(RejectionReason reason)  // 丟 BusinessRuleViolation 且沒有新事件
        // 或 .thenNoEvents()
    BoardScenario.whenCreating(() -> Board.create(...)).thenEvents(...)
    ```
    測試共用常數：`BOARD = new BoardId("board-1")`、`T0 = Instant.parse("2026-11-14T01:00:00Z")`、`T1 = T0.plusSeconds(60)`。

- [ ] **Step 1：寫失敗的測試。** 每條規則至少一支，名稱與斷言如下（Given 一律從 `BoardCreated(BOARD, "產品開發", T0)` 開始）：
  - `建立看板_產生BoardCreated`
  - `改名_產生BoardRenamed`；`改成相同名稱_不產生事件`（R11）
  - `封存_產生BoardArchived`；`已封存再封存_拒絕BOARD_ARCHIVED`（R8）
  - `已封存的看板_拒絕所有變更`（R8；參數化，對本 Task 全部 8 個指令方法各跑一次，都要 `BOARD_ARCHIVED`）
  - `新增欄位_加在最右邊`（R9；Given 已有「待辦」，When 新增「進行中」，Then `ColumnAdded`；再 `moveColumn` 驗證它在位置 1）
  - `欄位名稱重複_拒絕COLUMN_NAME_DUPLICATED`（R7）；`欄位改成別欄已用的名稱_拒絕`（R7）
  - `調整欄位順序_產生ColumnMoved`；`移到原位置_不產生事件`（R11）；`位置超出範圍_拒絕POSITION_OUT_OF_RANGE`（R10；3 欄時位置 3）
  - `設定在製品上限_產生ColumnWipLimitSet`；`設成相同上限_不產生事件`；`取消上限_產生ColumnWipLimitRemoved`；`本來就沒上限又取消_不產生事件`（R4、R11）
  - `刪除空欄位_產生ColumnRemoved`
  - `操作不存在的欄位_拒絕COLUMN_NOT_FOUND`（R12；參數化，對 5 個欄位指令各跑一次）
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作事件、`Board`、`BoardScenario`。14 種事件的套用方法在本 Task 就全部寫好（卡片相關的指令方法留到 Task 5）。
- [ ] **Step 3b：本 Task 加入的架構規則：** `EVENTS_NAMED_IN_PAST_TENSE`、`AGGREGATE_HAS_NO_SETTERS`、`AGGREGATE_INTERNALS_NOT_PUBLIC`、`APPLY_METHODS_DO_NOT_REJECT`、`STATE_CHANGES_ONLY_IN_APPLY`、`AGGREGATE_REFERENCES_BY_ID`、`AGGREGATE_TESTS_USE_SCENARIO`（`architecture-tests` 以 test scope 相依 entities 的 test-jar）。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新用語表與對照表（DDD-7、ES-3、ES-5：已驗證；DDD-4：規則已驗證，R1～R12 測試於 Task 5 補齊後定稿；DDD-5 備註「尚無第二個聚合」）。
- [ ] **Step 5：** Commit。

---

### Task 5：看板聚合（卡片的行為）與重播測試

**Files：**
- Modify：`…/entities/board/Board.java`
- Test：`…/board/CardRulesTest.java`、`…/board/ReplayTest.java`

**Interfaces：**
- Produces（`Board` 新增）：`addCard(CardId, ColumnId, CardTitle, CardDescription, Instant)`、`changeCardContent(CardId, CardTitle, CardDescription, Instant)`、`moveCard(CardId, ColumnId toColumn, Position toPosition, Instant)`、`repositionCard(CardId, Position toPosition, Instant)`、`removeCard(CardId, Instant)`
  - `moveCard` 的 `toColumn` 等於卡片目前所在欄 → 行為等同 `repositionCard`（產生 `CardRepositioned` 或不產生事件）。
  - `toPosition` 的合法範圍：換欄時 `0..目標欄張數`；欄內排序時 `0..張數-1`。

- [ ] **Step 1：寫失敗的測試**（共用 Given：看板有「待辦」`col-todo`、「進行中」`col-doing`，視需要加卡片與上限）：
  - `新增卡片_加在該欄最下面`（R9）
  - `新增卡片會超過上限_拒絕WIP_LIMIT_EXCEEDED`（R2；「進行中」上限 1 且已有 1 張）
  - `移入卡片會超過上限_拒絕WIP_LIMIT_EXCEEDED`（R2）
  - `移入後剛好等於上限_成功`（R2 邊界；上限 2、已有 1 張）
  - `移動卡片_產生CardMoved並帶來源欄與目標欄`
  - `欄內排序_產生CardRepositioned`；`已達上限的欄內仍可排序`（R3）
  - `移到原位置_不產生事件`（R11）
  - `移到不存在的欄_拒絕COLUMN_NOT_FOUND`（R10）；`目標位置超出範圍_拒絕POSITION_OUT_OF_RANGE`（R10）
  - `上限設得比現有張數低_拒絕WIP_LIMIT_BELOW_CARD_COUNT`（R5；2 張卡、上限設 1）；`上限等於現有張數_成功`
  - `欄內有卡片不能刪欄_拒絕COLUMN_NOT_EMPTY`（R1）；`卡片移走後可以刪欄`（R1）
  - `改卡片內容_產生CardContentChanged`；`內容沒變_不產生事件`
  - `刪除卡片_產生CardRemoved並帶所在欄`；`刪除後該欄張數減少_原本放不進的卡片現在放得進`
  - `操作不存在的卡片_拒絕CARD_NOT_FOUND`（R12；參數化 4 個卡片指令）
  - `已封存的看板_拒絕所有卡片變更`（R8；參數化 5 個）
  - `ReplayTest.同一串事件重播兩次_後續指令的結果相同`（ES-4）：準備一串至少含每種事件各一筆的歷史，`rehydrate` 兩次，對兩個實例各下同一個會被拒絕的指令與同一個會成功的指令，比對結果相等。
  - `ReplayTest.重播不產生未提交事件`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表（ES-4 已驗證並附測試名稱；DDD-4 列出 R1～R12 對應的測試名稱）。
- [ ] **Step 5：** Commit。

---

### Task 7：指令端 Gateway、記憶體版 Event Store、看板 Repository

**Files：**
- Create：`board-management/use-cases/src/main/java/kanban/boardmanagement/usecases/commands/gateway/`：`BoardRepository`、`EventStore`、`ConcurrentUpdateException`、`TimeSource`、`IdentityGenerator`
- Create：`…/usecases/eventfeed/`：`EventFeed`、`RecordedEvent`
- Create：`board-management/interface-adapters/src/main/java/kanban/boardmanagement/adapters/eventstore/`：`InMemoryEventStore`、`EventSourcedBoardRepository`、`NotifyingEventStore`
- Create：`…/adapters/system/`：`SystemTimeSource`、`UuidIdentityGenerator`
- Test：`interface-adapters/src/test/java/.../eventstore/EventStoreContract.java`（abstract）、`InMemoryEventStoreTest.java`、`EventSourcedBoardRepositoryTest.java`
- Modify：`interface-adapters/pom.xml`（用 `maven-jar-plugin` 產出 test-jar，讓 `frameworks-drivers` 的測試能重用契約）；兩個 `module-info.java`
- Delete：use-cases 與 adapters 的探針

**Interfaces：**
- Produces：
  ```java
  // usecases.commands.gateway
  public interface BoardRepository { Optional<Board> load(BoardId id); void save(Board board); } // save 可丟 ConcurrentUpdateException；成功後呼叫 board.markCommitted()
  public interface EventStore {
      List<BoardEvent> readStream(String streamId);                       // 不存在回空清單
      void append(String streamId, long expectedVersion, List<BoardEvent> events); // 目前事件數 != expectedVersion → ConcurrentUpdateException
  }
  public final class ConcurrentUpdateException extends RuntimeException {}
  public interface TimeSource { Instant now(); }
  public interface IdentityGenerator { String nextId(); }
  // usecases.eventfeed
  public interface EventFeed { List<RecordedEvent> readAfter(long position, int maxCount); } // 依 position 遞增；position 從 1 起算，傳 0 代表從頭
  public record RecordedEvent(long position, BoardEvent event) {}
  // adapters.eventstore
  public final class InMemoryEventStore implements EventStore, EventFeed { public InMemoryEventStore() }
  public final class EventSourcedBoardRepository implements BoardRepository { public EventSourcedBoardRepository(EventStore store) } // streamId = "board-" + id.value()
  public final class NotifyingEventStore implements EventStore { public NotifyingEventStore(EventStore delegate, Runnable afterAppend) } // append 成功後呼叫 afterAppend；afterAppend 丟出的例外吞掉並寫到 System.Logger，不影響 append 的結果
  ```
  - `EventStoreContract`：`protected abstract EventStore store(); protected abstract EventFeed feed();`
  - **契約測試不得假設事件庫是空的**（PostgreSQL 版的事件表不能清）：每支測試的 streamId 用隨機值；`EventFeed` 的測試先讀出目前最後的位置當基準，只斷言基準之後的事件。

- [ ] **Step 1：寫失敗的測試。** `EventStoreContract` 的測試方法：
  - `新增後讀得回來_順序不變`
  - `不存在的串流_回空清單`
  - `預期版本不符_丟ConcurrentUpdateException且不寫入任何事件`（ES-6；包含「一次寫兩筆、版本不符」時兩筆都沒寫入）
  - `兩條串流互不影響`
  - `EventFeed依寫入順序給出遞增的位置`；`readAfter只回傳位置之後的事件`；`maxCount限制回傳筆數`
  - `介面沒有更新與刪除`（ES-2）：用反射斷言 `EventStore` 與 `EventFeed` 的方法名稱集合恰為 `{readStream, append}` 與 `{readAfter}`
  - `EventSourcedBoardRepositoryTest`：`存入再載入_行為與原聚合相同`、`載入不存在的看板_回empty`、`兩個人載入同一版本各自修改_後存的丟ConcurrentUpdateException`、`儲存成功後未提交事件被清空`
  - `NotifyingEventStore`：`append成功後呼叫通知`、`通知丟例外_append仍算成功`、`append失敗_不呼叫通知`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。`InMemoryEventStore` 的 `append` 必須是執行緒安全的（檢查版本與寫入是同一個臨界區）。
- [ ] **Step 3b：本 Task 加入的架構規則：** `REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE`、`NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE`、`EVENT_ACCESS_IS_SEGREGATED`、`ONLY_MAIN_KNOWS_IMPLEMENTATIONS`。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表（DDD-8：已驗證；ES-1、ES-2、ES-6、CA-6：記憶體版已驗證，資料庫版待 Task 14，狀態先寫「已實作未驗證」）。
- [ ] **Step 5：** Commit。

---

### Task 8：14 個指令 Use Case

**Files：**
- Create：`…/usecases/commands/CommandResponse.java`
- Create：每個 Use Case 一個套件 `…/usecases/commands/<name>/`（公開）放 `<Name>Input`、`<Name>Request`、`<Name>Output`；`…/<name>/impl/`（不公開）放 `<Name>UseCase`。`<name>` 依序為：`createboard`、`renameboard`、`archiveboard`、`addcolumn`、`renamecolumn`、`movecolumn`、`setcolumnwiplimit`、`removecolumnwiplimit`、`removecolumn`、`addcard`、`changecardcontent`、`movecard`、`repositioncard`、`removecard`
- Test：`use-cases/src/test/java/.../commands/` 下每個 Use Case 一支測試，加上共用的 `FakeBoardRepository`（記憶體，含樂觀鎖）、`FixedTimeSource`、`SequentialIdentityGenerator`、`CapturingOutput`
- Modify：`use-cases/module-info.java`（公開 14 個邊界套件與 `commands`、`commands.gateway`、`eventfeed`；`impl` 套件不公開）

**Interfaces：**
- Consumes：Task 7 的 Gateway；Task 4、5 的 `Board`
- Produces：
  ```java
  public record CommandResponse(boolean succeeded, String failureCode, String createdId) {} // failureCode：RejectionReason 的名稱，或 "BOARD_NOT_FOUND"、"CONCURRENT_UPDATE"、"INVALID_REQUEST"；成功時為 null
  // 每個 Use Case（以 MoveCard 為例）
  public interface MoveCardInput  { void execute(MoveCardRequest request); }
  public interface MoveCardOutput { void present(CommandResponse response); }
  public final class MoveCardUseCase implements MoveCardInput {
      public MoveCardUseCase(BoardRepository boards, TimeSource time, MoveCardOutput output)
  }
  ```
  Request record（欄位全是 `String`／`int`／`Integer`）：
  `CreateBoardRequest(String name)`、`RenameBoardRequest(String boardId, String name)`、`ArchiveBoardRequest(String boardId)`、`AddColumnRequest(String boardId, String name)`、`RenameColumnRequest(String boardId, String columnId, String name)`、`MoveColumnRequest(String boardId, String columnId, int toPosition)`、`SetColumnWipLimitRequest(String boardId, String columnId, int limit)`、`RemoveColumnWipLimitRequest(String boardId, String columnId)`、`RemoveColumnRequest(String boardId, String columnId)`、`AddCardRequest(String boardId, String columnId, String title, String description)`、`ChangeCardContentRequest(String boardId, String cardId, String title, String description)`、`MoveCardRequest(String boardId, String cardId, String toColumnId, int toPosition)`、`RepositionCardRequest(String boardId, String cardId, int toPosition)`、`RemoveCardRequest(String boardId, String cardId)`
  - `CreateBoardUseCase`、`AddColumnUseCase`、`AddCardUseCase` 的建構子多一個 `IdentityGenerator`；成功時 `createdId` 分別是新的看板、欄位、卡片識別碼，其餘 Use Case 的 `createdId` 為 null。
  - Use Case 本體的固定形狀（DDD-9：不含業務判斷）：把 Request 的字串包成值物件 → `boards.load` → 呼叫聚合的**一個**方法 → `boards.save` → `output.present(...)`。`BusinessRuleViolation` → `failureCode = reason().name()`；找不到看板 → `"BOARD_NOT_FOUND"`；`ConcurrentUpdateException` → `"CONCURRENT_UPDATE"`；Request 本身或欄位為 null → `"INVALID_REQUEST"`；唯一例外是 `AddCardRequest`、`ChangeCardContentRequest` 的 `description`，null 視為沒有說明（交給 `CardDescription` 轉成空字串）。`execute` 永遠不丟例外、恰好呼叫 `present` 一次。

- [ ] **Step 1：寫失敗的測試。** 每個 Use Case 至少四支（測試不啟動 Spring，CA-3）：
  - `成功_聚合被存回且回覆成功`（用 `FakeBoardRepository` 重新載入後，再下一個指令驗證狀態確實改變）
  - `看板不存在_回覆BOARD_NOT_FOUND`（Review Focus 2；`CreateBoard` 除外）
  - `違反業務規則_回覆對應的原因且沒有存入任何事件`（各挑一條該指令會碰到的規則）
  - `Request欄位為null_回覆INVALID_REQUEST`
  另外對 `MoveCardUseCase` 寫 `儲存時版本衝突_回覆CONCURRENT_UPDATE`；對三個會產生識別碼的 Use Case 驗證 `createdId`；對每個 Use Case 驗證事件的 `occurredAt` 等於 `FixedTimeSource` 給的時間。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作 14 個 Use Case。重複的「載入→執行→儲存→回覆」流程抽成 `commands` 套件內一個非公開的輔助類別，放在不公開的 `…usecases.commands.support` 套件。
- [ ] **Step 3b：本 Task 加入的架構規則：** `EVERY_USECASE_HAS_BOTH_BOUNDARIES`、`INPUT_BOUNDARY_HAS_ONE_METHOD`、`BOUNDARY_TYPES_ARE_SIMPLE`、`ONE_AGGREGATE_PER_COMMAND`、`COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA`、`NO_UNNECESSARY_PUBLIC`、`USECASE_PACKAGES_ON_ALLOW_LIST`。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新用語表、對照表（CA-3、CA-4、CA-5、CA-16：已驗證；CQRS-2：指令端已驗證，查詢端待 Task 10；DDD-9：待 Task 18 審查）。
- [ ] **Step 5：** Commit。

---

### Task 9：事件轉 JSON 與格式演進

**Files：**
- Create：`…/adapters/eventstore/BoardEventJson.java`、`StoredEvent.java`、`UnknownEventTypeException.java`
- Test：`…/eventstore/BoardEventJsonTest.java`；樣本檔 `interface-adapters/src/test/resources/event-samples/<event-type>.v<N>.json`

**Interfaces：**
- Produces：
  ```java
  public record StoredEvent(String eventType, int schemaVersion, String payloadJson) {}
  public final class BoardEventJson {
      public StoredEvent toStored(BoardEvent event);       // 一律寫出該事件的最新 schemaVersion
      public BoardEvent fromStored(StoredEvent stored);    // 舊版本先經 upcaster 轉成最新格式；不認得的 eventType 或比程式新的 schemaVersion → UnknownEventTypeException
  }
  ```
  - `eventType` 是寫死的字串常數（例如 `"CardAdded"`），不是由類別名稱反射取得——類別改名不可以讓舊事件讀不回來。
  - JSON 欄位名稱採小駝峰，值物件攤平成基本型別（例如 `"columnId": "col-1"`、`"wipLimit": 3`、`"toPosition": 0`、`"occurredAt": "2026-11-14T01:00:00Z"`）。
  - **刻意做一次格式升級來證明 ES-7**：`CardAdded` 有 v1（沒有 `description`）與 v2（有 `description`）。upcaster 把 v1 補上 `"description": ""`。其餘 13 種事件只有 v1。

- [ ] **Step 1：寫失敗的測試：**
  - `每種事件_轉成JSON再轉回來_與原事件相等`（參數化 14 種）
  - `樣本檔都讀得回來`：掃描 `event-samples/` 全部檔案，檔名解析出 `eventType` 與版本，`fromStored` 不丟例外且結果型別正確
  - `每種事件的每個版本都有樣本檔`：`BoardEventJson` 支援的 (eventType, version) 組合必須都有對應檔案（共 15 個）
  - `CardAdded_v1_讀回來時說明是空字串`
  - `不認得的事件類型_丟UnknownEventTypeException`（Review Focus 5）；`比程式新的版本_丟UnknownEventTypeException`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作並建立 15 個樣本檔。樣本檔檔頭不加註解（JSON 不支援），改在 `event-samples/README.md` 寫明「樣本檔只增不改」。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表 ES-7（已驗證）。
- [ ] **Step 5：** Commit。

---

### Task 10：查詢端——讀取模型資料與 3 個查詢 Use Case

**Files：**
- Create：`…/usecases/queries/viewboard/`：`ViewBoardInput`、`ViewBoardRequest`、`ViewBoardOutput`、`ViewBoardResponse`、`BoardSnapshot`、`ColumnSnapshot`、`CardSnapshot`、`BoardSnapshotReader`；`impl/ViewBoardUseCase`
- Create：`…/usecases/queries/listboards/`：`ListBoardsInput`、`ListBoardsOutput`、`ListBoardsResponse`、`BoardSummary`、`BoardSummaryReader`；`impl/ListBoardsUseCase`
- Create：`…/usecases/queries/viewcardhistory/`：`ViewCardHistoryInput`、`ViewCardHistoryRequest`、`ViewCardHistoryOutput`、`ViewCardHistoryResponse`、`CardHistory`、`CardHistoryEntry`、`CardHistoryEntryKind`、`CardHistoryReader`；`impl/ViewCardHistoryUseCase`
- Test：`use-cases/src/test/java/.../queries/` 三支
- Modify：`use-cases/module-info.java`

**Interfaces：**
- Produces：
  ```java
  public record BoardSnapshot(String boardId, String name, boolean archived, List<ColumnSnapshot> columns) {}
  public record ColumnSnapshot(String columnId, String name, Integer wipLimit, List<CardSnapshot> cards) {} // wipLimit 為 null 代表無上限
  public record CardSnapshot(String cardId, String title, String description) {}
  public interface BoardSnapshotReader { Optional<BoardSnapshot> boardById(String boardId); }
  public record ViewBoardRequest(String boardId) {}
  public record ViewBoardResponse(boolean found, BoardSnapshot board) {}
  public interface ViewBoardInput { void execute(ViewBoardRequest request); }
  public interface ViewBoardOutput { void present(ViewBoardResponse response); }

  public record BoardSummary(String boardId, String name, boolean archived) {}
  public interface BoardSummaryReader { List<BoardSummary> allBoards(); } // 依建立順序
  public record ListBoardsResponse(List<BoardSummary> boards) {}
  public interface ListBoardsInput { void execute(); }
  public interface ListBoardsOutput { void present(ListBoardsResponse response); }

  public enum CardHistoryEntryKind { ADDED, MOVED, REMOVED }
  public record CardHistoryEntry(CardHistoryEntryKind kind, String fromColumnName, String toColumnName, Instant occurredAt) {} // ADDED：from 為 null；REMOVED：to 為 null
  public record CardHistory(String cardId, String title, List<CardHistoryEntry> entries) {}
  public interface CardHistoryReader { Optional<CardHistory> cardHistory(String boardId, String cardId); }
  public record ViewCardHistoryRequest(String boardId, String cardId) {}
  public record ViewCardHistoryResponse(boolean found, CardHistory history) {}
  ```
  Use Case 建構子：`ViewBoardUseCase(BoardSnapshotReader, ViewBoardOutput)`，其餘兩個同形。
  - 查詢端的型別完全不引用 `entities` 的任何型別、不引用 `commands` 與 `eventfeed`。

- [ ] **Step 1：寫失敗的測試：** 每個 Use Case：`找得到_把讀取模型的資料原樣交給Output`、`找不到_found為false`（Review Focus 2；`ListBoards` 改為 `沒有看板_回空清單`）、`request為null或欄位為null_found為false`。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 3b：本 Task 加入的架構規則：** `COMMANDS_AND_QUERIES_SEPARATE`、`QUERIES_NEVER_TOUCH_WRITE_SIDE`、`COMMANDS_NEVER_READ_READ_MODEL`；Task 8 的 `EVERY_USECASE_HAS_BOTH_BOUNDARIES`、`BOUNDARY_TYPES_ARE_SIMPLE`、`USECASE_PACKAGES_ON_ALLOW_LIST` 自動涵蓋查詢端，確認仍通過。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表（CQRS-1、CQRS-2、CQRS-3、CQRS-4：已驗證）。
- [ ] **Step 5：** Commit。

---

### Task 11：Projection 與記憶體版讀取模型

**Files：**
- Create：`…/adapters/projection/`：`ReadModelStore`、`InMemoryReadModelStore`、`BoardProjection`、`BoardSnapshotProjector`、`CardHistoryProjector`
- Test：`…/projection/ReadModelStoreContract.java`（abstract）、`InMemoryReadModelStoreTest`、`BoardProjectionTest`、`ProjectorsTest`

**Interfaces：**
- Consumes：`EventFeed`、`RecordedEvent`（Task 7）；讀取模型 record 與三個 Reader（Task 10）
- Produces：
  ```java
  public interface ReadModelStore extends BoardSnapshotReader, BoardSummaryReader, CardHistoryReader {
      long checkpoint();                                   // 尚未處理過任何事件時為 0
      void inExclusiveTransaction(Runnable work);          // 同一時間只有一個在執行；work 內的寫入與 checkpoint 更新一起成功或一起失敗
      void saveBoard(BoardSnapshot board);
      void saveCardHistory(String boardId, CardHistory history);
      void advanceCheckpoint(long position);
      void reset();                                        // 清空全部讀取模型並把 checkpoint 設回 0
  }
  public final class InMemoryReadModelStore implements ReadModelStore { public InMemoryReadModelStore() }
  public final class BoardProjection {
      public BoardProjection(EventFeed feed, ReadModelStore store);
      public void catchUp();   // 迴圈：batch = feed.readAfter(store.checkpoint(), 200)，讀不到就結束；batch 內「每一筆事件各自一個」inExclusiveTransaction：進交易後若 event.position <= store.checkpoint() 就略過，否則套用並 advanceCheckpoint(event.position)
      public void rebuild();   // store.reset() 後 catchUp()
  }
  final class BoardSnapshotProjector { static BoardSnapshot apply(BoardSnapshot currentOrNull, BoardEvent event); }   // 純函式
  final class CardHistoryProjector  { static Optional<CardHistory> apply(CardHistory currentOrNull, BoardSnapshot boardBeforeEvent, BoardEvent event); } // 只有 CardAdded、CardMoved、CardRemoved、CardContentChanged（更新標題）會有結果；欄位名稱取事件當下 boardBeforeEvent 裡的名稱
  ```
  - 寫入方法（`saveBoard`、`saveCardHistory`、`advanceCheckpoint`、`reset`、`inExclusiveTransaction`）只有 `BoardProjection` 可以呼叫（本 Task 的 `ONLY_PROJECTION_WRITES_READ_MODEL` 守）。
  - 單一事件處理失敗 → 只有那一筆的交易回復、例外往外丟、`catchUp` 停止；checkpoint 停在最後一筆成功的事件。
  - Projection 內沒有任何業務判斷：只把事件已經說定的事實抄進讀取模型（例如不重新檢查上限）。

- [ ] **Step 1：寫失敗的測試：**
  - `ReadModelStoreContract`：`存入的看板讀得回來`、`allBoards依建立順序`、`存入的卡片歷程讀得回來`、`reset後全部清空且checkpoint為0`、`inExclusiveTransaction內丟例外_寫入與checkpoint都不保留`
  - `ProjectorsTest`：14 種事件各一支，驗證套用後的 `BoardSnapshot`；`卡片歷程_新增後換欄再刪除_三筆且欄名正確`；`欄位改名之後_舊歷程仍顯示當時的名稱`；`欄內排序與改說明_不產生歷程`
  - `BoardProjectionTest`：
    - `catchUp後讀取模型反映全部事件`
    - `catchUp連續呼叫兩次_結果不變`（CQRS-5 冪等）
    - `同一批事件重複送進來_結果不變`：用一個測試用 `EventFeed`，故意讓 `readAfter` 回傳位置不比 checkpoint 新的事件，驗證被略過
    - `重建後與重建前完全相同`（ES-8）：比對 `allBoards()`、每個 `boardById`、每張卡片的 `cardHistory`
    - `遇到處理不了的事件_丟例外且checkpoint停在前一筆`（Review Focus 5；測試用 `EventFeed` 的 `readAfter` 回傳位置 1、2 兩筆正常事件，再次被呼叫（`readAfter(2, …)`）時丟 `UnknownEventTypeException`；斷言 `catchUp` 丟出該例外、`store.checkpoint() == 2`、前兩筆的結果查得到）
    - `套用到一半失敗_那一筆的寫入不保留`：測試用 `ReadModelStore` 包裝在位置 3 的 `saveBoard` 丟例外；斷言 `checkpoint() == 2` 且位置 3 的變更查不到
    - `超過200筆事件_全部補完`
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 3b：本 Task 加入的架構規則：** `ONLY_PROJECTION_WRITES_READ_MODEL`。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表（CQRS-5、ES-8：已驗證並附測試名稱）。
- [ ] **Step 5：** Commit。

---

### Task 12：Controller、Presenter、ViewModel

**Files：**
- Create：`…/adapters/controllers/`：17 個 Controller（`<Name>Controller`，名稱同 Use Case）
- Create：`…/adapters/presenters/`：`CommandPresenter`、`CommandViewModel`、`CommandOutcome`、`BoardPresenter`、`BoardViewModel`、`ColumnViewModel`、`CardViewModel`、`BoardListPresenter`、`BoardListViewModel`、`BoardListItemViewModel`、`CardHistoryPresenter`、`CardHistoryViewModel`、`CardHistoryLineViewModel`
- Test：`…/presenters/` 四支 Presenter 測試、`…/controllers/ControllersTest.java`

**Interfaces：**
- Consumes：Task 8、10 的 Input／Output／Request／Response
- Produces：
  ```java
  // Controller：建構子收自己的 Input；一個 handle 方法，參數是原始輸入，回傳 void
  public final class MoveCardController { public MoveCardController(MoveCardInput input); public void handle(String boardId, String cardId, String toColumnId, Integer toPosition); }
  // 數字參數一律用 Integer 收；為 null 時不呼叫 Input，改丟 IllegalArgumentException("toPosition")（由薄殼統一轉成 422）

  public enum CommandOutcome { SUCCEEDED, REJECTED, NOT_FOUND, CONFLICT }
  public record CommandViewModel(CommandOutcome outcome, String message, String createdId) {}
  public final class CommandPresenter implements /* 14 個 <Name>Output */ { public CommandViewModel viewModel(); }

  public record CardViewModel(String cardId, String title, String description) {}
  public record ColumnViewModel(String columnId, String name, String cardCountLabel, List<CardViewModel> cards) {}
  public record BoardViewModel(boolean found, String boardId, String name, boolean archived, String archivedLabel, List<ColumnViewModel> columns) {}
  public final class BoardPresenter implements ViewBoardOutput { public BoardViewModel viewModel(); }

  public record BoardListItemViewModel(String boardId, String name, boolean archived, String archivedLabel) {}
  public record BoardListViewModel(List<BoardListItemViewModel> boards) {}
  public final class BoardListPresenter implements ListBoardsOutput { public BoardListViewModel viewModel(); }

  public record CardHistoryLineViewModel(String occurredAt, String description) {}
  public record CardHistoryViewModel(boolean found, String cardTitle, List<CardHistoryLineViewModel> lines) {}
  public final class CardHistoryPresenter implements ViewCardHistoryOutput { public CardHistoryPresenter(ZoneId zone); public CardHistoryViewModel viewModel(); }
  ```
  固定文案：
  - `cardCountLabel`：有上限 `"2 / 3"`；無上限 `"2"`。
  - `archivedLabel`：`"已封存"` 或 `""`。
  - 歷程描述：`"新增到「待辦」"`、`"從「待辦」移到「進行中」"`、`"已刪除"`；時間格式 `yyyy-MM-dd HH:mm`（依傳入時區）。
  - `failureCode` → `outcome`／`message`：
    | failureCode | outcome | message |
    |---|---|---|
    | （成功） | SUCCEEDED | `""` |
    | BOARD_NOT_FOUND | NOT_FOUND | 找不到這個看板 |
    | COLUMN_NOT_FOUND | NOT_FOUND | 找不到這個欄位 |
    | CARD_NOT_FOUND | NOT_FOUND | 找不到這張卡片 |
    | CONCURRENT_UPDATE | CONFLICT | 看板已被更新，請重新整理後再試 |
    | BOARD_ARCHIVED | REJECTED | 看板已封存，不能再變更 |
    | WIP_LIMIT_EXCEEDED | REJECTED | 這一欄已達在製品上限，放不進去 |
    | WIP_LIMIT_BELOW_CARD_COUNT | REJECTED | 上限不能低於欄內現有的卡片數，請先移走卡片 |
    | COLUMN_NOT_EMPTY | REJECTED | 欄內還有卡片，不能刪除 |
    | COLUMN_NAME_DUPLICATED | REJECTED | 這個看板已經有同名的欄位 |
    | NAME_BLANK | REJECTED | 名稱不可空白 |
    | NAME_TOO_LONG | REJECTED | 名稱最長 100 字 |
    | DESCRIPTION_TOO_LONG | REJECTED | 說明最長 2000 字 |
    | WIP_LIMIT_NOT_POSITIVE | REJECTED | 在製品上限必須是 1 以上的整數 |
    | POSITION_OUT_OF_RANGE | REJECTED | 位置超出範圍 |
    | IDENTIFIER_BLANK、INVALID_REQUEST、其他未知代碼 | REJECTED | 請求內容不完整或格式不正確 |
  - Presenter 不比較張數與上限、不決定任何操作可不可以做（DDD-9）。

- [ ] **Step 1：寫失敗的測試：** `CommandPresenter` 對上表每一列各一支（參數化）；`BoardPresenter`：`有上限顯示2斜線3`、`無上限只顯示張數`、`已封存顯示標籤`、`找不到看板_found為false且其他欄位為空`；`CardHistoryPresenter`：三種描述各一支、`時間依台北時區顯示`（`2026-11-14T01:00:00Z` → `"2026-11-14 09:00"`）；`ControllersTest`：每個 Controller 把參數原樣組成 Request 交給 Input（用假的 Input 擷取），數字參數為 null 時丟 `IllegalArgumentException`。
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 3b：本 Task 加入的架構規則：** `VIEWMODELS_ARE_DISPLAY_READY`、`CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES`。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表 CA-9（Presenter 測試與 ViewModel 規則已驗證；前端審查待 Task 18，狀態先寫「已實作未驗證」）、CA-11（DIP、ISP 已驗證；SRP、OCP、LSP 待 Task 18）。
- [ ] **Step 5：** Commit。

---

### Task 14：PostgreSQL——資料庫結構、Event Store、讀取模型

**Files：**
- Create：`board-management/frameworks-drivers/src/main/resources/db/migration/V1__event_store.sql`、`V2__read_model.sql`
- Create：`…/adapters/eventstore/PostgresEventStore.java`、`…/adapters/projection/PostgresReadModelStore.java`
- 測試隔離：`PostgresTestDatabase` 每個測試類別開始前把 `public` schema 整個砍掉重建並重跑 Flyway（`DROP SCHEMA public CASCADE; CREATE SCHEMA public;`）；同一個類別內的測試靠隨機 streamId 與基準位置互不干擾。觸發程序永遠保持啟用。
- Test（放在 `frameworks-drivers`，因為需要驅動程式與 Testcontainers）：`frameworks-drivers/src/test/java/kanban/boardmanagement/frameworks/persistence/`：`PostgresTestDatabase`（啟動容器並跑 Flyway）、`PostgresEventStoreTest extends EventStoreContract`、`PostgresReadModelStoreTest extends ReadModelStoreContract`、`EventTableIsAppendOnlyTest`、`WriteSideSchemaTest`、`PostgresProjectionTest`
- Modify：`frameworks-drivers/pom.xml`（`org.postgresql:postgresql`、`flyway-core`、`flyway-database-postgresql`；test：Testcontainers PostgreSQL、`interface-adapters` 的 test-jar）

**Interfaces：**
- Consumes：`EventStore`、`EventFeed`、`ReadModelStore`、`BoardEventJson`、兩個契約測試基底
- Produces：
  ```java
  public final class PostgresEventStore implements EventStore, EventFeed { public PostgresEventStore(javax.sql.DataSource dataSource, BoardEventJson json) }
  public final class PostgresReadModelStore implements ReadModelStore { public PostgresReadModelStore(javax.sql.DataSource dataSource) }
  ```
  資料表：
  ```sql
  -- V1：寫入端只有這一張表
  CREATE TABLE board_events (
      global_position BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
      stream_id       TEXT        NOT NULL,
      stream_version  BIGINT      NOT NULL,   -- 從 1 起算
      event_type      TEXT        NOT NULL,
      schema_version  INT         NOT NULL,
      payload         JSONB       NOT NULL,
      occurred_at     TIMESTAMPTZ NOT NULL,
      UNIQUE (stream_id, stream_version)
  );
  -- 加上 BEFORE UPDATE OR DELETE 與 BEFORE TRUNCATE 觸發程序，一律 RAISE EXCEPTION 'board_events is append-only'
  -- V2：讀取模型（可整批清掉）
  CREATE TABLE board_view         (board_id TEXT PRIMARY KEY, created_order BIGINT GENERATED ALWAYS AS IDENTITY, doc JSONB NOT NULL);
  CREATE TABLE card_history_view  (board_id TEXT NOT NULL, card_id TEXT NOT NULL, doc JSONB NOT NULL, PRIMARY KEY (board_id, card_id));
  CREATE TABLE projection_checkpoint (name TEXT PRIMARY KEY, position BIGINT NOT NULL);
  ```
  - `append`：一個交易內先 `SELECT pg_advisory_xact_lock(<固定常數>)`（保證 `global_position` 的順序等於提交順序），查該串流目前最大 `stream_version`，不等於 `expectedVersion` → 回復並丟 `ConcurrentUpdateException`；否則逐筆 INSERT 後提交。唯一鍵衝突也轉成 `ConcurrentUpdateException`。
  - `PostgresReadModelStore.inExclusiveTransaction`：開交易、取另一把固定常數的 advisory lock、執行、提交；`work` 丟例外則回復。同一條執行緒在 `work` 內呼叫的其他方法共用這條連線。`reset` 用 `TRUNCATE` 清讀取模型表。

- [ ] **Step 1：寫失敗的測試：**
  - 兩支契約測試（直接繼承，零新增測試方法）
  - `EventTableIsAppendOnlyTest`：對 `board_events` 直接下 `UPDATE`、`DELETE`、`TRUNCATE`，三者都丟 `SQLException` 且訊息含 `append-only`（ES-2）
  - `WriteSideSchemaTest.寫入端只有事件表`（ES-1）：查 `information_schema.tables`，`public` schema 的資料表集合恰為 `{board_events, board_view, card_history_view, projection_checkpoint, flyway_schema_history}`
  - `PostgresEventStoreTest` 另加：`十條執行緒同時對同一條串流寫入同一個預期版本_恰好一條成功`
  - `PostgresProjectionTest`：`重建後與重建前完全相同`、`catchUp兩次結果不變`（Task 11 同名測試接上 PostgreSQL）
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作。
- [ ] **Step 4：** `./mvnw -q verify` 通過（需要 Docker 在執行）；更新對照表：CA-6、ES-1、ES-2、ES-6 改「已驗證」。
- [ ] **Step 5：** Commit。

---

### Task 15：Spring 薄殼、Main 組裝、整合測試

**Files：**
- Create：`…/frameworks-drivers/src/main/java/kanban/boardmanagement/frameworks/web/`：`BoardManagementRequests`（介面）、`BoardHttpEndpoints`（`@RestController`）、`HttpStatusMapping`、`BadRequestHandling`（`@RestControllerAdvice`）、各端點的請求 body record
- Create：`main/src/main/java/kanban/main/`：`KanbanApplication`、`BoardManagementWiring`（`@Configuration`）、`BoardManagementRequestAssembly`（實作 `BoardManagementRequests`）、`StartupCatchUp`
- Create：`main/src/main/resources/application.yml`、`compose.yaml`（PostgreSQL）
- Test：`main/src/test/java/kanban/main/`：`BoardHttpIntegrationTest`、`ConsistencyIntegrationTest`、`StartupCatchUpIntegrationTest`、`InMemoryModeIntegrationTest`
- Create：`architecture-tests/…/rules/FrameworkRules.java` 與其兩支測試

**Interfaces：**
- Consumes：Task 7～14 的全部公開類別
- Produces：
  - `BoardManagementRequests`：17 個方法加一個 `CommandViewModel invalidRequest()`，名稱同 Use Case 小駝峰，參數是原始輸入，回傳對應的 ViewModel。例：`CommandViewModel moveCard(String boardId, String cardId, String toColumnId, Integer toPosition)`、`BoardViewModel viewBoard(String boardId)`、`BoardListViewModel listBoards()`、`CardHistoryViewModel viewCardHistory(String boardId, String cardId)`。
  - `BoardManagementRequestAssembly`：每個方法內 `new` 一個 Presenter、一個 Use Case（把 Presenter 當 Output 傳入）、一個 Controller，呼叫 `handle`，回傳 `presenter.viewModel()`。共用的單例（Repository、Store、Projection、`TimeSource`、`IdentityGenerator`）由 `BoardManagementWiring` 的 `@Bean` 方法建立並由建構子傳入。
  - `BoardManagementWiring`：`kanban.storage=postgres`（預設）→ `PostgresEventStore`＋`PostgresReadModelStore`，啟動時跑 Flyway；`kanban.storage=memory` → 記憶體版。指令端拿到的 `EventStore` 是 `new NotifyingEventStore(真正的 store, projection::catchUp)`。`kanban.display-zone` 預設 `Asia/Taipei`。
  - `StartupCatchUp`：應用程式啟動完成時呼叫一次 `projection.catchUp()`。
  - 端點（JSON；回應 body 就是 ViewModel）：
    | 方法與路徑 | body | 對應 |
    |---|---|---|
    | `POST /api/boards` | `{name}` | createBoard |
    | `GET /api/boards` | | listBoards |
    | `GET /api/boards/{boardId}` | | viewBoard |
    | `PUT /api/boards/{boardId}/name` | `{name}` | renameBoard |
    | `POST /api/boards/{boardId}/archive` | | archiveBoard |
    | `POST /api/boards/{boardId}/columns` | `{name}` | addColumn |
    | `PUT /api/boards/{boardId}/columns/{columnId}/name` | `{name}` | renameColumn |
    | `PUT /api/boards/{boardId}/columns/{columnId}/position` | `{position}` | moveColumn |
    | `PUT /api/boards/{boardId}/columns/{columnId}/wip-limit` | `{limit}` | setColumnWipLimit |
    | `DELETE /api/boards/{boardId}/columns/{columnId}/wip-limit` | | removeColumnWipLimit |
    | `DELETE /api/boards/{boardId}/columns/{columnId}` | | removeColumn |
    | `POST /api/boards/{boardId}/cards` | `{columnId,title,description}` | addCard |
    | `PUT /api/boards/{boardId}/cards/{cardId}/content` | `{title,description}` | changeCardContent |
    | `PUT /api/boards/{boardId}/cards/{cardId}/column` | `{columnId,position}` | moveCard |
    | `PUT /api/boards/{boardId}/cards/{cardId}/position` | `{position}` | repositionCard |
    | `DELETE /api/boards/{boardId}/cards/{cardId}` | | removeCard |
    | `GET /api/boards/{boardId}/cards/{cardId}/history` | | viewCardHistory |
  - 狀態碼（`HttpStatusMapping`，一對一、無其他判斷）：`SUCCEEDED`→200、`REJECTED`→422、`NOT_FOUND`→404、`CONFLICT`→409；查詢的 `found=false`→404。
  - `BadRequestHandling`：JSON 解析失敗、型別不符、`IllegalArgumentException` → 呼叫 `BoardManagementRequests.invalidRequest()` 取得 ViewModel，照 `HttpStatusMapping` 回 422。薄殼自己不寫任何訊息文字。
  - `BoardManagementRequests.invalidRequest()`：`BoardManagementRequestAssembly` 的實作是 `new CommandPresenter()`，對它 `present(new CommandResponse(false, "INVALID_REQUEST", null))`，回傳 `viewModel()`——訊息文字仍然只出自 Presenter。
  - `FrameworkRules.ENDPOINTS_ONLY_DELEGATE`：`..frameworks.web..` 只能相依 `BoardManagementRequests`、`HttpStatusMapping`、`*ViewModel`、`CommandOutcome`、body record、JDK 與 Spring；不得相依 `..usecases..`、`..entities..`，也不得相依 `..adapters.presenters..` 以外的任何 adapters 套件。

- [ ] **Step 1：寫失敗的測試**（`@SpringBootTest(webEnvironment = RANDOM_PORT)`＋Testcontainers）：
  - `BoardHttpIntegrationTest`：
    - `完整流程`：建看板 → 加「待辦」「進行中」→「進行中」上限設 1 → 加兩張卡到「待辦」→ 第一張移到「進行中」（200）→ 第二張移到「進行中」（422，訊息「這一欄已達在製品上限，放不進去」）→ `GET` 看板，「進行中」的 `cardCountLabel` 為 `"1 / 1"` → 第一張卡的歷程有兩行
    - `刪除還有卡片的欄_422`；`不存在的看板_404`（指令與查詢各一，Review Focus 2）
    - `body是空的JSON_422且有中文訊息`、`position是字串_422`、`position是負數_422`（Review Focus 1）
  - `ConsistencyIntegrationTest`（CQRS-6，對應設計文件 7.5 三種情況）：
    - `指令成功後立刻查詢_看得到新資料`
    - `讀取模型更新失敗_指令仍成功_查詢是舊資料_下一次指令後補上`：測試設定把 `ReadModelStore` 包一層，可開關「下一次 `saveBoard` 丟例外」
    - `兩個請求同時改同一個看板_後者409`：直接用兩個 `BoardRepository.load` 後分別 `save` 來重現；HTTP 層另驗證 `CONCURRENT_UPDATE` 對應 409 與訊息
  - `StartupCatchUpIntegrationTest.啟動時讀取模型是空的_自動補齊`（Review Focus 4）：先直接用 `PostgresEventStore` 寫入事件，清空讀取模型表，再啟動應用程式，`GET` 看得到看板
  - `InMemoryModeIntegrationTest.記憶體模式跑完整流程`（`kanban.storage=memory`，不需要 Docker）
- [ ] **Step 2：** 跑測試確認失敗。
- [ ] **Step 3：** 實作；**本 Task 加入的架構規則：** `ENDPOINTS_ONLY_DELEGATE`（含樣本與咬人測試）。
- [ ] **Step 4：** `./mvnw -q verify` 通過；更新對照表：CQRS-6、CA-7、CA-8 改「已驗證」。
- [ ] **Step 5：** Commit。

---

### Task 16：Vue 畫面

**Files：**
- Create：`board-management/web-ui/`：`package.json`、`vite.config.ts`（`/api` 代理到 `http://localhost:8080`）、`tsconfig.json`、`eslint.config.js`、`index.html`
- Create：`src/api/boardApi.ts`（17 個函式，一對一呼叫端點）、`src/api/viewModels.ts`（與後端 ViewModel 同形的型別）
- Create：`src/views/BoardListView.vue`、`BoardView.vue`、`CardHistoryView.vue`；`src/components/ColumnPanel.vue`、`CardItem.vue`、`MessageBar.vue`；`src/router.ts`、`src/main.ts`、`src/App.vue`
- Test：`src/views/__tests__/BoardView.spec.ts`、`src/components/__tests__/ColumnPanel.spec.ts`

**Interfaces：**
- Consumes：Task 15 的端點與 ViewModel 形狀
- Produces：
  - 路由：`/`（看板清單＋建立看板）、`/boards/:boardId`（看板現況）、`/boards/:boardId/cards/:cardId/history`（卡片移動歷程）。
  - 畫面行為規則（CA-9，全部寫進 `docs/review-checklist.md` 的前端項目）：
    1. 元件只顯示 ViewModel 給的字串、布林、列舉，不做運算、不做格式轉換。
    2. 任何指令送出後，不論成功或失敗，都重新 `GET` 看板現況；不在前端先改畫面。
    3. 指令回覆不是 `SUCCEEDED` → 把 `message` 原樣顯示在 `MessageBar`。
    4. 前端不判斷能不能放、不比較張數與上限、不擋操作（封存後按鈕仍可按，由後端拒絕並顯示訊息）。
    5. 卡片與欄位的移動都用瀏覽器原生的 drag and drop；放下時只送出「目標欄識別碼＋放下位置的索引」（欄位則是放下位置的索引）。不另外加設計文件沒有的操作入口。
  - ESLint：`src/views`、`src/components` 內禁用 `Date`、`Intl`、`toLocaleString`、`toFixed`（`no-restricted-globals`／`no-restricted-properties`），違反即 lint 失敗。

- [ ] **Step 1：寫失敗的測試（Vitest＋@vue/test-utils，API 用 mock）：**
  - `ColumnPanel_把cardCountLabel原樣顯示`
  - `BoardView_指令被拒絕時顯示後端給的訊息並重新讀取看板`
  - `BoardView_指令成功後重新讀取看板`
  - `BoardView_放下卡片時送出目標欄與位置`
- [ ] **Step 2：** 跑 `npm test` 確認失敗。
- [ ] **Step 3：** 實作三個畫面與全部 14 種操作的入口。
- [ ] **Step 4：** `npm run lint && npm test && npm run build` 全部通過。
- [ ] **Step 5：** Commit。

---

### Task 17：CI

**Files：**
- Create：`.github/workflows/ci.yml`
- Modify：`README.md`（「狀態」一節改為：如何在本機啟動、如何跑全部檢查、對照表連結）

**Interfaces：**
- Produces：一個 workflow，`on: [push, pull_request]`，兩個 job：
  - `backend`：`ubuntu-latest`、Temurin 25、`./mvnw -B verify`（含 ArchUnit、Testcontainers）
  - `frontend`：Node 22、在 `board-management/web-ui` 跑 `npm ci && npm run lint && npm test && npm run build`

- [ ] **Step 1：** 寫 workflow 與 README。
- [ ] **Step 2：** 本機跑一次兩個 job 內的指令，全部通過。
- [ ] **Step 3：** Commit。更新對照表備註：「CI 設定已入庫；首次在 GitHub 上執行要等使用者同意推送」。

---

### Task 18：收尾——審查清單、對照表定稿、瀏覽器實測

**Files：**
- Modify：`docs/traceability.md`、`docs/review-checklist.md`

- [ ] **Step 1：** 跑 `./mvnw verify`（不加 `-q`）；從 surefire 報告統計測試總數與失敗數，貼進回報（不可只看 exit code）。
- [ ] **Step 2：** 逐項執行 `docs/review-checklist.md`，把日期、commit、每項結果與備註寫進「審查紀錄」表；發現不符就修到符合，再重跑 Step 1。
- [ ] **Step 3：** 對照表定稿：逐列確認「由哪一支測試守住」欄寫的測試或規則常數真的存在（用名稱搜尋）。預期 38 列「已驗證」、DDD-3「尚未實作」。任何一列對不上 → 照實寫「已實作未驗證」並在回報最前面列出。
- [ ] **Step 4：瀏覽器實測。** `docker compose up -d`、啟動後端、`npm run dev`，在瀏覽器實際操作設計文件第 2 節的每一項：
  - 看板：建立、改名、封存（封存後再操作看到拒絕訊息）
  - 欄位：新增、改名、調整順序、設定上限、取消上限、刪除空欄、刪除有卡片的欄（被拒絕）
  - 卡片：新增、改內容、拖到另一欄、拖到已滿的欄（被拒絕並看到訊息）、欄內排序、刪除
  - 畫面：看板現況、卡片移動歷程（含換欄紀錄與時間）、重新整理頁面後資料仍在、重啟後端後資料仍在
  每一項記錄「通過／不通過」，不通過就修並重跑受影響的測試。
- [ ] **Step 5：** Commit，並向使用者回報：逐條規則狀態、測試數、瀏覽器實測結果、尚未做的事（推送、CI 首次執行、DDD-3）。

---

## 規則目錄（不是 Task；各規則在哪個 Task 加入，見該 Task 的「本 Task 加入的架構規則」）

正式程式碼違反規則 → 改程式碼。認為某條規則的寫法與設計文件矛盾 → 停下來回報，NEVER 自行改鬆。

### 規則目錄 A：Entities 圈（`EntitiesRules`）

  - `ENTITIES_USE_ONLY_JDK`（CA-2、CA-7）：`..entities..` 只能相依 `java..` 與自己
  - `NO_CLOCK_OR_RANDOM_IN_ENTITIES`（ES-4）：不得呼叫 `Instant.now`、`Clock`、`System.currentTimeMillis`、`System.nanoTime`、`java.util.Random`、`UUID.randomUUID`、`Math.random`
  - `VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS`（DDD-6、DDD-7 不可變）：`..entities.board` 內除 `Board`、`BusinessRuleViolation`、`RejectionReason` 以外的公開型別，以及 `..events..` 內所有非介面型別，都必須是 record
  - `EVENTS_NAMED_IN_PAST_TENSE`（DDD-7）：`..events..` 內 record 的簡名必須以 `Created|Renamed|Archived|Added|Moved|Set|Removed|Changed|Repositioned` 結尾
  - `AGGREGATE_HAS_NO_SETTERS`（DDD-4）：`Board` 沒有名稱以 `set` 開頭且參數為 1 個的公開方法（`setColumnWipLimit` 有 3 個參數，不受影響）；公開方法的回傳型別不得是 `Column`、`Card` 或可變集合實作類別
  - `AGGREGATE_INTERNALS_NOT_PUBLIC`（DDD-4、CA-16）：`Column`、`Card` 不是 public
  - `APPLY_METHODS_DO_NOT_REJECT`（ES-3）：`Board` 內名稱以 `apply` 或 `on` 開頭的方法不得建立 `BusinessRuleViolation`、不得呼叫 `record`
  - `STATE_CHANGES_ONLY_IN_APPLY`（ES-3）：`Board` 的欄位只能被名稱以 `apply`／`on` 開頭的方法、建構子寫入（未提交清單與版本號欄位除外，明列名稱）


### 規則目錄 B：其餘各圈（`CleanArchitectureRules`、`DddRules`、`CqrsRules`、`EventSourcingRules`）

  | 規則常數 | 守哪條 | 內容 |
  |---|---|---|
  | `RINGS_DEPEND_INWARD` | CA-1 | `layeredArchitecture`：entities ← usecases ← adapters ← frameworks ← main，只准由外向內 |
  | `USECASES_USE_ONLY_JDK_AND_ENTITIES` | CA-3、CA-7 | `..usecases..` 只能相依 `java..`、`..entities..`、自己 |
  | `ADAPTERS_FREE_OF_FRAMEWORKS` | CA-7 | `..adapters..` 不得相依 `org.springframework..`、`jakarta..`、`org.postgresql..`、`com.zaxxer..`、`org.flywaydb..` |
  | `EVERY_USECASE_HAS_BOTH_BOUNDARIES` | CA-4 | 每個 `..usecases.(commands|queries).<x>` 套件（`gateway`、`support` 除外）恰有一個 `*Input` 介面與一個 `*Output` 介面；`..impl` 內恰有一個實作該 `*Input` 的類別，且它有一個型別為該 `*Output` 的欄位 |
  | `INPUT_BOUNDARY_HAS_ONE_METHOD` | CA-11 ISP | `*Input`、`*Output` 介面各只有一個方法 |
  | `BOUNDARY_TYPES_ARE_SIMPLE` | CA-5 | `*Input`／`*Output` 方法的參數型別，以及 `*Request`／`*Response`／`CommandResponse`／查詢端 record 的欄位型別（遞迴展開泛型參數），只能是基本型別、`String`、`Integer`、`Boolean`、`Instant`、`List`、enum、或同屬 `..usecases..` 的 record；不得出現 `..entities..` 的任何型別 |
  | `CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES` | CA-11 DIP | `..adapters.controllers..` 對 `..usecases..` 的相依只能是 `*Input` 與 `*Request` |
  | `ONLY_MAIN_KNOWS_IMPLEMENTATIONS` | CA-8、CA-11 DIP | `..usecases..impl..` 與 `..usecases.commands.support..` 只能被自己與 `kanban.main..` 相依；`..adapters..` 內的「行為類別」（Gateway 與 Reader 的實作、`*Output` 的實作、`*Controller`、`BoardProjection`、`BoardEventJson`）的建構子只能被 `kanban.main..` 呼叫。資料 record（`*ViewModel`、`StoredEvent`）不在此限 |
  | `VIEWMODELS_ARE_DISPLAY_READY` | CA-9 | `*ViewModel` record 的欄位只能是 `String`、`boolean`、enum、`List<*ViewModel>` |
  | `TOP_LEVEL_PACKAGES_SCREAM` | CA-10 | `kanban` 之下的第一層套件只能是 `boardmanagement`、`main`；情境之下的第一層只能是 `entities`、`usecases`、`adapters`、`frameworks` |
  | `USECASE_PACKAGES_ON_ALLOW_LIST` | CA-10 | Global Constraints 的允許清單：`usecases` 之下只有 `commands`、`queries`、`eventfeed`；`commands` 之下只有 14 個 Use Case 名稱＋`gateway`、`support`；`queries` 之下只有 3 個 Use Case 名稱；Use Case 套件之下只允許 `impl`。清單寫死在規則裡，新增 Use Case 時要同時改清單 |
  | `NO_PACKAGE_CYCLES` | CA-12 | `slices().matching("kanban.(*)..")` 與 `slices().matching("kanban.boardmanagement.(*)..")` 無循環 |
  | `NO_UNNECESSARY_PUBLIC` | CA-16 | `..impl..`、`..support..` 內除了被 Main 建立的 Use Case 類別外，沒有公開型別；`Column`、`Card`、兩個 Projector 不是 public |
  | `CONTEXTS_DO_NOT_SHARE_DOMAIN` | DDD-2 | `kanban.(*)..` 各情境的 `..entities..` 不得被其他情境相依（樣本：`kanban.fixtures.othercontext` 引用看板的值物件） |
  | `AGGREGATE_REFERENCES_BY_ID` | DDD-5 | `..entities.<a>..` 與 `..entities.<b>..` 不同聚合套件之間不得互相相依（樣本：兩個假聚合互相持有） |
  | `ONE_AGGREGATE_PER_COMMAND` | DDD-5 | `..commands..impl..` 的 Use Case 類別只有一個 `*Repository` 型別的欄位 |
  | `REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE` | DDD-8、CA-6 | `*Repository`、`EventStore`、`EventFeed`、`*Reader` 介面在 `..usecases..`；它們的實作在 `..adapters..` |
  | `COMMANDS_AND_QUERIES_SEPARATE` | CQRS-1 | `..usecases.commands..` 與 `..usecases.queries..` 互不相依 |
  | `COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA` | CQRS-2 | 每個指令 `*Output` 的方法參數型別恰為 `CommandResponse`；`CommandResponse` 的欄位恰為 `succeeded`、`failureCode`、`createdId` |
  | `QUERIES_NEVER_TOUCH_WRITE_SIDE` | CQRS-2、CQRS-3 | `..usecases.queries..` 不得相依 `..entities..`、`..commands..`、`..eventfeed..` |
  | `COMMANDS_NEVER_READ_READ_MODEL` | CQRS-4 | `..usecases.commands..` 與 `..entities..` 不得相依 `..queries..`；`..adapters.eventstore..` 不得相依 `..adapters.projection..` |
  | `ONLY_PROJECTION_WRITES_READ_MODEL` | CQRS-5、CA-11 ISP | `ReadModelStore` 的 `saveBoard`、`saveCardHistory`、`advanceCheckpoint`、`reset`、`inExclusiveTransaction` 只能被 `BoardProjection` 呼叫；`ReadModelStore` 型別只能被 `..adapters.projection..` 與 `kanban.main..` 相依 |
  | `EVENT_ACCESS_IS_SEGREGATED` | CA-11 ISP | `EventStore` 只被 `..adapters.eventstore..` 與 `kanban.main..` 相依；`EventFeed` 只被 `..adapters.eventstore..`（實作）、`..adapters.projection..`、`kanban.main..` 相依 |
  | `NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE` | ES-1 | `..usecases.commands.gateway..` 的介面集合恰為 `BoardRepository`、`EventStore`、`TimeSource`、`IdentityGenerator`；`BoardRepository` 的實作恰為 `EventSourcedBoardRepository`，且它唯一的 Gateway 相依是 `EventStore` |
  | `AGGREGATE_TESTS_USE_SCENARIO` | ES-5 | entities 模組測試類別中，名稱以 `RulesTest`／`LifecycleTest` 結尾者，不得直接呼叫 `Board` 的 `rehydrate`、`uncommittedEvents`（只能經 `BoardScenario`） |

  | `SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING` | CA-7 | `org.springframework..` 的任何型別只出現在 `..frameworks..` 與 `kanban.main..` |

