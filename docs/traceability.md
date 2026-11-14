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
| CA-6 | — | — | 尚未實作 |
| CA-7 | 內三圈 | `ADAPTERS_FREE_OF_FRAMEWORKS`、`SPRING_ANNOTATIONS_ONLY_IN_OUTER_RING` | 已實作未驗證（目前只有探針類別） |
| CA-8 | — | — | 尚未實作 |
| CA-9 | — | — | 尚未實作 |
| CA-10 | 模組與最上層套件命名 | `TOP_LEVEL_PACKAGES_SCREAM` | 已實作未驗證（Use Cases 圈內的套件命名規則待 Task 8） |
| CA-11 | — | — | 尚未實作 |
| CA-12 | 模組與各圈套件相依圖 | 編譯（Maven 模組不允許循環）＋`NO_PACKAGE_CYCLES` | 已驗證 |
| CA-13 | — | — | 尚未實作 |
| CA-14 | 設計文件 `docs/design/phase-one-board-management.md` 第 10 節「邊界清單」 | 設計文件＋本表 | 已驗證 |
| CA-15 | — | — | 尚未實作 |
| CA-16 | 內三圈的 `module-info.java` | 編譯（Task 1 反向證明：引用未公開的 impl 套件編譯失敗） | 已實作未驗證（`NO_UNNECESSARY_PUBLIC` 待 Task 8） |
| DDD-1 | `docs/glossary.md` | `GlossaryTest` | 已實作未驗證（用詞審查待 Task 18） |
| DDD-2 | `kanban.<情境>` 套件 | `DddRules.CONTEXTS_DO_NOT_SHARE_DOMAIN`（`DddRulesTest`、`DddRulesBiteTest`） | 已驗證（備註：目前只有一個業務情境，規則還沒被真實的第二個情境考驗過） |
| DDD-3 | — | — | 尚未實作（第二階段） |
| DDD-4 | — | — | 尚未實作 |
| DDD-5 | — | — | 尚未實作 |
| DDD-6 | 9 個值物件：`BoardId`、`ColumnId`、`CardId`、`BoardName`、`ColumnName`、`CardTitle`、`CardDescription`、`WipLimit`、`Position` | `ValueObjectsTest`＋`EntitiesRules.VALUE_OBJECTS_AND_EVENTS_ARE_RECORDS` | 已驗證 |
| DDD-7 | — | — | 尚未實作 |
| DDD-8 | — | — | 尚未實作 |
| DDD-9 | — | — | 尚未實作 |
| ES-1 | — | — | 尚未實作 |
| ES-2 | — | — | 尚未實作 |
| ES-3 | — | — | 尚未實作 |
| ES-4 | Entities 圈不讀時鐘、不產生亂數 | `EntitiesRules.NO_CLOCK_OR_RANDOM_IN_ENTITIES` | 已實作未驗證（重播測試待 Task 5） |
| ES-5 | — | — | 尚未實作 |
| ES-6 | — | — | 尚未實作 |
| ES-7 | — | — | 尚未實作 |
| ES-8 | — | — | 尚未實作 |
| CQRS-1 | — | — | 尚未實作 |
| CQRS-2 | — | — | 尚未實作 |
| CQRS-3 | — | — | 尚未實作 |
| CQRS-4 | — | — | 尚未實作 |
| CQRS-5 | — | — | 尚未實作 |
| CQRS-6 | — | — | 尚未實作 |
