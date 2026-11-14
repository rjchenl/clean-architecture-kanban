# 規則對照表

依專案 `CLAUDE.md` 第 6 節第 2 點：每一個規則編號一列。狀態只有三種——**已驗證**（附測試或規則名稱）、**已實作未驗證**、**尚未實作**。

規則名稱（全大寫）是 `architecture-tests` 模組 `kanban.architecture.rules` 套件內的常數；每條都有「對正式程式碼」的測試與「對違規樣本」的咬人測試。

| 規則編號 | 落在哪個模組或類別 | 由哪一支測試或哪一項檢查守住 | 目前狀態 |
|---|---|---|---|
| CA-1 | 四個圈模組的相依設定 | 編譯（Maven 模組）＋`CleanArchitectureRules.RINGS_DEPEND_INWARD`（`CleanArchitectureRulesTest`、`CleanArchitectureRulesBiteTest`） | 已驗證 |
| CA-2 | `board-management/entities`（`module-info` 不 requires 任何模組） | 編譯＋Maven Enforcer `inner-ring-has-no-dependencies`＋`CleanArchitectureRules.ENTITIES_USE_ONLY_JDK` | 已驗證 |
| CA-3 | `board-management/use-cases` | Maven Enforcer＋`USECASES_USE_ONLY_JDK_AND_ENTITIES` | 已實作未驗證（目前只有探針類別） |
| CA-4 | — | — | 尚未實作 |
| CA-5 | — | — | 尚未實作 |
| CA-6 | 介面：`usecases.commands.gateway`（`BoardRepository`、`EventStore`）、`usecases.eventfeed.EventFeed`；實作：`adapters.eventstore` | 編譯＋`DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE`＋契約測試 `EventStoreContract`（目前只對 `InMemoryEventStore` 跑） | 已實作未驗證（PostgreSQL 版與讀取模型的契約測試待 Task 11、14） |
| CA-7 | 內三圈 | `ADAPTERS_FREE_OF_FRAMEWORKS`、`SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING` | 已實作未驗證（目前只有探針類別） |
| CA-8 | `main` 模組 | `CleanArchitectureRules.ONLY_MAIN_KNOWS_IMPLEMENTATIONS`＋`module-info`（impl 套件不公開） | 已實作未驗證（Main 的組裝程式待 Task 15） |
| CA-9 | — | — | 尚未實作 |
| CA-10 | 模組與最上層套件命名 | `TOP_LEVEL_PACKAGES_SCREAM` | 已實作未驗證（Use Cases 圈內的套件命名規則待 Task 8） |
| CA-11 | — | DIP：編譯＋`ONLY_MAIN_KNOWS_IMPLEMENTATIONS`；ISP：`EVENT_ACCESS_IS_SEGREGATED` | 已實作未驗證（其餘 ISP 規則待 Task 8、11、12；SRP、OCP、LSP 審查待 Task 18） |
| CA-12 | 模組與各圈套件相依圖 | 編譯（Maven 模組不允許循環）＋`NO_PACKAGE_CYCLES` | 已驗證 |
| CA-13 | — | — | 尚未實作 |
| CA-14 | 設計文件 `docs/design/phase-one-board-management.md` 第 10 節「邊界清單」 | 設計文件＋本表 | 已驗證 |
| CA-15 | — | — | 尚未實作 |
| CA-16 | 內三圈的 `module-info.java` | 編譯（Task 1 反向證明：引用未公開的 impl 套件編譯失敗） | 已實作未驗證（`NO_UNNECESSARY_PUBLIC` 待 Task 8） |
| DDD-1 | `docs/glossary.md` | `GlossaryTest` | 已實作未驗證（用詞審查待 Task 18） |
| DDD-2 | `kanban.<情境>` 套件 | `DddRules.CONTEXTS_DO_NOT_SHARE_DOMAIN`（`DddRulesTest`、`DddRulesBiteTest`） | 已驗證（備註：目前只有一個業務情境，規則還沒被真實的第二個情境考驗過） |
| DDD-3 | — | — | 尚未實作（第二階段） |
| DDD-4 | `Board` 聚合（`Column`、`Card` 是非公開的內部 record） | `EntitiesRules.AGGREGATE_HAS_NO_SETTERS`、`AGGREGATE_INTERNALS_NOT_PUBLIC`＋聚合測試：R1、R5 `CardRulesTest`（欄內有卡片不能刪欄、上限設得比現有張數低）；R2、R3、R10 `CardRulesTest`；R4、R7、R9、R11、R12 `ColumnRulesTest`、`CardRulesTest`；R6 `ValueObjectsTest`；R8 `BoardLifecycleTest`、`CardRulesTest`（已封存的看板拒絕所有變更） | 已驗證 |
| DDD-5 | `Board` 聚合（目前唯一的聚合） | `EntitiesRules.AGGREGATE_REFERENCES_BY_ID`（附違規樣本） | 已實作未驗證（「一個指令只改一個聚合」的規則待 Task 8；備註：只有一個聚合，「聚合之間」那一半還沒有真實案例） |
| DDD-6 | 9 個值物件：`BoardId`、`ColumnId`、`CardId`、`BoardName`、`ColumnName`、`CardTitle`、`CardDescription`、`WipLimit`、`Position` | `ValueObjectsTest`＋`EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` | 已驗證 |
| DDD-7 | 14 個領域事件 record，共同型別 `BoardEvent` | `EntitiesRules.EVENTS_NAMED_IN_PAST_TENSE`、`VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` | 已驗證 |
| DDD-8 | `BoardRepository`（一個聚合一個）；實作 `EventSourcedBoardRepository` | `DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE`＋`EventSourcedBoardRepositoryTest` | 已驗證 |
| DDD-9 | — | — | 尚未實作 |
| ES-1 | `EventStore` 是寫入端唯一的儲存途徑；`EventSourcedBoardRepository` 只靠它 | `EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE` | 已實作未驗證（資料庫結構檢查待 Task 14） |
| ES-2 | `EventStore`、`EventFeed` 介面沒有更新與刪除 | `EventStoreContract.介面沒有更新與刪除` | 已實作未驗證（資料庫層的唯讀觸發程序待 Task 14） |
| ES-3 | `Board`：指令方法只做檢查與產生事件；`apply`／`on…` 方法只改狀態 | `EntitiesRules.APPLY_METHODS_DO_NOT_REJECT`、`STATE_CHANGES_ONLY_IN_APPLY`＋聚合測試 | 已驗證 |
| ES-4 | `Board.rehydrate`；時間由指令方法的參數傳入 | `ReplayTest`（同一串事件重播兩次_後續指令的結果相同 等 7 支）＋`EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES` | 已驗證 |
| ES-5 | `BoardScenario`（Given／When／Then） | `EntitiesRules.AGGREGATE_TESTS_USE_SCENARIO` | 已驗證 |
| ES-6 | `EventStore.append` 的預期版本號 | `EventStoreContract.預期版本不符_丟ConcurrentUpdateException且不寫入任何事件`、`InMemoryEventStoreTest.十條執行緒同時寫入同一個預期版本_恰好一條成功` | 已實作未驗證（PostgreSQL 版待 Task 14） |
| ES-7 | — | — | 尚未實作 |
| ES-8 | — | — | 尚未實作 |
| CQRS-1 | — | — | 尚未實作 |
| CQRS-2 | — | — | 尚未實作 |
| CQRS-3 | — | — | 尚未實作 |
| CQRS-4 | — | — | 尚未實作 |
| CQRS-5 | — | — | 尚未實作 |
| CQRS-6 | — | — | 尚未實作 |
