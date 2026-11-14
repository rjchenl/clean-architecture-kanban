# 規則對照表

依專案 `CLAUDE.md` 第 6 節第 2 點：每一個規則編號一列。狀態只有三種——**已驗證**（附測試或規則名稱）、**已實作未驗證**、**尚未實作**。

規則名稱（全大寫）是 `architecture-tests` 模組 `kanban.architecture.rules` 套件內的常數；每條都有「對正式程式碼」的測試與「對違規樣本」的咬人測試。

| 規則編號 | 落在哪個模組或類別 | 由哪一支測試或哪一項檢查守住 | 目前狀態 |
|---|---|---|---|
| CA-1 | 四個圈模組的相依設定 | 編譯（Maven 模組）＋`CleanArchitectureRules.RINGS_DEPEND_INWARD`（`CleanArchitectureRulesTest`、`CleanArchitectureRulesBiteTest`） | 已驗證 |
| CA-2 | `board-management/entities`（`module-info` 不 requires 任何模組） | 編譯＋Maven Enforcer `inner-ring-has-no-dependencies`＋`CleanArchitectureRules.ENTITIES_USE_ONLY_JDK` | 已驗證 |
| CA-3 | `board-management/use-cases`（`module-info` 只 requires Entities） | 編譯＋Maven Enforcer＋`CleanArchitectureRules.USECASES_USE_ONLY_JDK_AND_ENTITIES`；14 支 `…UseCaseTest` 用 `FakeBoardRepository`，不啟動 Spring | 已驗證 |
| CA-4 | 17 個 Use Case 套件的 `…Input`、`…Output` 與 `impl/…UseCase`；`adapters.controllers` 呼叫 Input、`adapters.presenters` 實作 Output | `UseCaseRules.EVERY_USECASE_HAS_BOTH_BOUNDARIES`、`AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES`＋`ControllersTest` | 已驗證 |
| CA-5 | `…Request`、`…Response`、`CommandResponse`、查詢端的讀取模型 record（解讀見設計文件 4.3） | `UseCaseRules.BOUNDARY_TYPES_ARE_SIMPLE` | 已驗證 |
| CA-6 | 介面：`usecases.commands.gateway`、`usecases.eventfeed`、查詢端三個 `…Reader`；實作：`adapters.eventstore`、`adapters.projection`（記憶體版與 PostgreSQL 版） | 編譯＋`DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE`＋契約測試各跑兩次：`EventStoreContract`（`InMemoryEventStoreTest`、`PostgresEventStoreTest`）、`ReadModelStoreContract`（`InMemoryReadModelStoreTest`、`PostgresReadModelStoreTest`） | 已驗證 |
| CA-7 | Spring 只出現在 `frameworks-drivers` 與 `main`；內三圈無任何框架 | `CleanArchitectureRules.ADAPTERS_FREE_OF_FRAMEWORKS`、`SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING`、`ENTITIES_USE_ONLY_JDK`、`USECASES_USE_ONLY_JDK_AND_ENTITIES`＋`FrameworkRules.ENDPOINTS_ONLY_DELEGATE`＋編譯（內兩圈零相依） | 已驗證 |
| CA-8 | `main` 模組：`BoardManagementWiring`（建立所有實作）、`BoardManagementRequestAssembly`（每次請求接起 Controller、Use Case、Presenter） | `CleanArchitectureRules.ONLY_MAIN_KNOWS_IMPLEMENTATIONS`＋`module-info`（impl 套件不公開，Task 1 反向證明）＋`BoardHttpIntegrationTest` | 已驗證 |
| CA-9 | `adapters.presenters`（Presenter 與 ViewModel）；`board-management/web-ui`（Vue 畫面只顯示 ViewModel） | `CommandPresenterTest`、`BoardPresenterTest`、`BoardListPresenterTest`、`CardHistoryPresenterTest`＋`AdapterRules.VIEWMODELS_ARE_DISPLAY_READY`＋前端 `ColumnPanel.spec.ts`、`BoardView.spec.ts`＋ESLint 規則（畫面元件禁用日期／數字格式化 API）＋審查清單 R-01～R-03（2026-11-14）＋瀏覽器實測（2026-11-14） | 已驗證 |
| CA-10 | 模組、最上層套件、Use Cases 圈套件命名 | `CleanArchitectureRules.TOP_LEVEL_PACKAGES_SCREAM`、`UseCaseRules.USECASE_PACKAGES_ON_ALLOW_LIST` | 已驗證 |
| CA-11 | 全部 | DIP：編譯＋`module-info`＋`CleanArchitectureRules.ONLY_MAIN_KNOWS_IMPLEMENTATIONS`、`AdapterRules.CONTROLLERS_USE_ONLY_INPUT_BOUNDARIES`；ISP：`UseCaseRules.INPUT_BOUNDARY_HAS_ONE_METHOD`、`CleanArchitectureRules.EVENT_ACCESS_IS_SEGREGATED`、`CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL`；OCP：`AdapterRules.ONE_PRESENTER_PER_OUTPUT`；SRP、OCP、LSP：審查清單 R-04～R-06（2026-11-14，R-05 發現一處不符合並已修正） | 已驗證 |
| CA-12 | 模組與各圈套件相依圖 | 編譯（Maven 模組不允許循環）＋`NO_PACKAGE_CYCLES` | 已驗證 |
| CA-13 | 模組與套件切分（每圈一個模組；Use Case 各自一個套件） | 審查清單 R-07（2026-11-14） | 已驗證 |
| CA-14 | 設計文件 `docs/design/phase-one-board-management.md` 第 10 節「邊界清單」 | 設計文件＋本表 | 已驗證 |
| CA-15 | 所有測試 | 審查清單 R-08（2026-11-14）＋`EntitiesRules.AGGREGATE_TESTS_USE_SCENARIO` | 已驗證 |
| CA-16 | 內三圈的 `module-info.java`；impl 套件不公開 | 編譯（Task 1 反向證明）＋`UseCaseRules.NO_UNNECESSARY_PUBLIC`＋`UseCaseRulesTest.CA16_模組宣告不公開impl與support套件`＋`EntitiesRules.AGGREGATE_INTERNALS_NOT_PUBLIC` | 已驗證 |
| DDD-1 | `docs/glossary.md` | `GlossaryTest`＋審查清單 R-09（2026-11-14） | 已驗證 |
| DDD-2 | `kanban.<情境>` 套件 | `DddRules.CONTEXTS_DO_NOT_SHARE_DOMAIN`（`DddRulesTest`、`DddRulesBiteTest`） | 已驗證（備註：目前只有一個業務情境，規則還沒被真實的第二個情境考驗過） |
| DDD-3 | — | — | 尚未實作（第二階段） |
| DDD-4 | `Board` 聚合（`Column`、`Card` 是非公開的內部 record） | `EntitiesRules.AGGREGATE_HAS_NO_SETTERS`、`AGGREGATE_INTERNALS_NOT_PUBLIC`＋聚合測試：R1、R5 `CardRulesTest`（欄內有卡片不能刪欄、上限設得比現有張數低）；R2、R3、R10 `CardRulesTest`；R4、R7、R9、R11、R12 `ColumnRulesTest`、`CardRulesTest`；R6 `ValueObjectsTest`；R8 `BoardLifecycleTest`、`CardRulesTest`（已封存的看板拒絕所有變更） | 已驗證 |
| DDD-5 | `Board` 聚合；每個指令 Use Case 只持有 `BoardRepository` | `EntitiesRules.AGGREGATE_REFERENCES_BY_ID`、`UseCaseRules.ONE_AGGREGATE_PER_COMMAND`＋審查清單 R-10（2026-11-14） | 已驗證（備註：只有一個聚合，「聚合之間只用識別碼互相參照」那一半還沒有真實案例，規則僅由違規樣本證明會擋） |
| DDD-6 | 9 個值物件：`BoardId`、`ColumnId`、`CardId`、`BoardName`、`ColumnName`、`CardTitle`、`CardDescription`、`WipLimit`、`Position` | `ValueObjectsTest`＋`EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` | 已驗證 |
| DDD-7 | 14 個領域事件 record，共同型別 `BoardEvent` | `EntitiesRules.EVENTS_NAMED_IN_PAST_TENSE`、`VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` | 已驗證 |
| DDD-8 | `BoardRepository`（一個聚合一個）；實作 `EventSourcedBoardRepository` | `DddRules.REPOSITORY_INTERFACE_INSIDE_IMPL_OUTSIDE`＋`EventSourcedBoardRepositoryTest` | 已驗證 |
| DDD-9 | 業務規則只在 `Board` 聚合與值物件；指令 Use Case 只協調（`commands.support.BoardCommand`） | 聚合測試涵蓋 R1～R12（見 DDD-4）＋審查清單 R-11（2026-11-14） | 已驗證 |
| ES-1 | 資料表 `board_events` 是寫入端唯一的表；`EventSourcedBoardRepository` 只靠 `EventStore` | `EventSourcingRules.NO_STATE_TABLE_ABSTRACTION_ON_WRITE_SIDE`＋資料庫結構檢查 `WriteSideSchemaTest.寫入端只有事件表` | 已驗證 |
| ES-2 | `EventStore`、`EventFeed` 介面沒有更新與刪除；`board_events` 的唯讀觸發程序（`V1__event_store.sql`） | `EventStoreContract.介面沒有更新與刪除`＋`EventTableIsAppendOnlyTest`（修改、刪除、清空都被資料庫拒絕） | 已驗證 |
| ES-3 | `Board`：指令方法只做檢查與產生事件；`apply`／`on…` 方法只改狀態 | `EntitiesRules.APPLY_METHODS_DO_NOT_REJECT`、`STATE_CHANGES_ONLY_IN_APPLY`＋聚合測試 | 已驗證 |
| ES-4 | `Board.rehydrate`；時間由指令方法的參數傳入 | `ReplayTest`（同一串事件重播兩次_後續指令的結果相同 等 7 支）＋`EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES` | 已驗證 |
| ES-5 | `BoardScenario`（Given／When／Then） | `EntitiesRules.AGGREGATE_TESTS_USE_SCENARIO` | 已驗證 |
| ES-6 | `EventStore.append` 的預期版本號；`board_events` 的 `(stream_id, stream_version)` 唯一鍵 | `EventStoreContract.預期版本不符_丟ConcurrentUpdateException且不寫入任何事件`（兩種實作）＋`PostgresEventStoreTest.十條執行緒同時對同一條串流寫入同一個預期版本_恰好一條成功`＋`InMemoryEventStoreTest` 同名測試 | 已驗證 |
| ES-7 | `adapters.eventstore.BoardEventJson`（事件類型名稱、格式版本、upcaster）；樣本檔 `interface-adapters/src/test/resources/event-samples/` | `BoardEventJsonTest`（樣本檔都讀得回來、每種事件的每個版本都有樣本檔、CardAdded_v1_讀回來時說明是空字串） | 已驗證 |
| ES-8 | `BoardProjection.rebuild`、`ReadModelStore.reset` | `BoardProjectionTest.重建後與重建前完全相同`、`PostgresProjectionTest.重建後與重建前完全相同` | 已驗證 |
| CQRS-1 | `usecases.commands`、`usecases.queries` 兩個套件 | `CqrsRules.COMMANDS_AND_QUERIES_SEPARATE`、`QUERIES_NEVER_TOUCH_WRITE_SIDE` | 已驗證 |
| CQRS-2 | `CommandResponse`；查詢的 Response Model 來自讀取模型 | `UseCaseRules.COMMAND_RESPONSE_CARRIES_NO_VIEW_DATA`、`CqrsRules.QUERIES_NEVER_TOUCH_WRITE_SIDE`＋查詢測試 `ViewBoardUseCaseTest`、`ListBoardsUseCaseTest`、`ViewCardHistoryUseCaseTest` | 已驗證 |
| CQRS-3 | 查詢 Use Case 只讀 `BoardSnapshotReader`、`BoardSummaryReader`、`CardHistoryReader` | `CqrsRules.QUERIES_NEVER_TOUCH_WRITE_SIDE` | 已驗證 |
| CQRS-4 | 指令端只靠聚合自己的狀態做決定 | `CqrsRules.COMMANDS_NEVER_READ_READ_MODEL` | 已驗證 |
| CQRS-5 | `adapters.projection.BoardProjection`（讀取模型唯一的寫入途徑） | `CqrsRules.ONLY_PROJECTION_WRITES_READ_MODEL`＋`BoardProjectionTest`（已處理過的事件再送一次_被略過_結果不變、catchUp連續呼叫兩次_結果不變） | 已驗證 |
| CQRS-6 | 設計文件 7.5 節；`NotifyingEventStore`＋`BoardProjection.catchUp`＋啟動時補齊 | `ConsistencyIntegrationTest`（情況1、情況2、情況3 各一支）＋`StartupCatchUpIntegrationTest` | 已驗證 |

## CI

`.github/workflows/ci.yml` 每次推送都跑後端 `./mvnw -B verify`（含全部架構測試與接 PostgreSQL 的測試）與前端的 lint、測試、打包。
**CI 設定已入庫，但還沒有在 GitHub 上實際執行過**——首次執行要等分支推送之後（推送需要使用者同意）。在那之前，「所有檢查都要在 CI 上每次推送自動執行」（專案規範第 6 節第 6 點）這一點只算設定完成、尚未驗證。
