# 審查清單

這些規則無法完全靠編譯或自動測試守住，只能由人（或審查者）逐項看。依專案 `CLAUDE.md` 第 6 節第 7 點，**每次審查的結果都要記在本文件末尾的「審查紀錄」與 `docs/traceability.md`，不可以當成自動通過**。

## 清單

| 項目 | 對應規則 | 要看什麼 |
|---|---|---|
| R-01 | CA-9 | Vue 元件只顯示 ViewModel 給的字串、布林、列舉；沒有運算、沒有格式轉換（日期、數字、字串拼接出業務意義） |
| R-02 | CA-9 | 任何指令送出後都重新讀取看板現況；前端沒有先行修改畫面資料 |
| R-03 | CA-9、DDD-9 | 前端不判斷操作能不能做（不比較張數與上限、不因封存而擋操作）；被拒絕時原樣顯示後端訊息 |
| R-04 | CA-11 SRP | 每個類別只對一個角色負責：改業務規則只動 Entities 圈；改畫面文案只動 Presenter；改儲存方式只動 Gateway 轉接程式 |
| R-05 | CA-11 OCP | 新增一個 Use Case 不需要修改既有 Use Case、既有 Controller、既有 Presenter 的程式 |
| R-06 | CA-11 LSP | 同一個 Gateway 介面的各個實作（記憶體版、PostgreSQL 版）可以互換，契約測試對兩者跑同一套 |
| R-07 | CA-13 | 會因為同一個理由一起改的類別放在同一個模組與套件；沒有模組被迫相依它用不到的東西 |
| R-08 | CA-15 | 測試只透過 Use Case 邊界、聚合的公開行為、Gateway 介面驗證；沒有測試去讀私有欄位或綁定內部結構 |
| R-09 | DDD-1 | 類別、方法、事件、指令的用詞貼近業務，與 `docs/glossary.md` 一致；沒有 `Manager`、`Helper`、`Data` 這類說不出業務意義的名稱出現在領域模型 |
| R-10 | DDD-5 | 每個指令 Use Case 只載入並儲存一個聚合 |
| R-11 | DDD-9 | Use Case 只做協調（取出聚合、呼叫一個方法、存回去）；Controller、Presenter、Projection 內沒有業務判斷 |
| R-12 | DDD-9 | 流程分析的 Projection 與讀取模型只對聚合**已經判斷好的結果**做加總、平均、計數；統計方式只限設計文件第 7.4 節的 S1（每週從週一 00:00 起算、時區由 Main 傳入、最近 8 週含本週）與 S2（平均與最長只算最近 8 週內完成的、停留只算已結束的）。什麼算開始、什麼算完成、交付時間怎麼算、算不算卡住，一律不在這裡判斷（設計第 0 節 4c） |
| R-13 | DDD-3 | 流程分析的防腐層只做翻譯：把看板動態的欄位對應到流程分析的用語、呼叫「記錄看板動態」指令、依回覆決定前進或重試；沒有任何業務判斷，也不讓看板管理的型別越過它 |
| R-14 | CA-9 | 流程分析的畫面只顯示 ViewModel 給的字串；階段、天數、日期的文字都由 Presenter 整理；前端不判斷能不能改階段、算不算卡住 |

## 審查紀錄

| 日期 | 審查範圍（commit） | 項目 | 結果 | 備註 |
|---|---|---|---|---|
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-01 | 通過 | Vue 元件只顯示 ViewModel 的字串。備註：`ColumnPanel.positionBefore` 與 `BoardView.moveColumnOnto` 會算「放下的位置是第幾個」，屬於畫面上的位置，不是業務規則；ESLint 規則禁止畫面元件使用日期／數字格式化 API |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-02 | 通過 | `BoardView.run`、`BoardListView.createBoard` 在每個指令之後都重新讀取；沒有任何先改畫面資料的程式 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-03 | 通過 | 前端沒有比較張數與上限的程式；封存後按鈕仍可按，由後端拒絕（`BoardView.spec.ts` 與瀏覽器實測皆驗證） |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-04 | 通過 | 業務規則只在 entities；文案只在 `CommandPresenter`、`CardHistoryPresenter`、`Labels`；儲存方式只在 `adapters.eventstore`、`adapters.projection` |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-05 | 發現一處不符合，已修正 | `CommandPresenter` 原本一個類別實作 14 個 Output Boundary，新增指令就得改它。已改成每個指令一個 Presenter 類別，並新增架構規則 `ONE_PRESENTER_PER_OUTPUT`。備註：新增 Use Case 仍須在 Main 的組裝程式、HTTP 薄殼與 `USECASE_PACKAGES_ON_ALLOW_LIST` 各加一段，這些是最外圈與允許清單，屬於預期中的擴充點 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-06 | 通過 | `EventStoreContract`、`ReadModelStoreContract` 各對記憶體版與 PostgreSQL 版跑同一套；`InMemoryModeIntegrationTest` 證明整套系統換儲存後照常運作 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-07 | 通過 | 每圈一個模組；每個 Use Case 一個套件；事件與聚合同套件（一起改）；查詢端的讀取模型 record 與使用它的查詢放在一起 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-08 | 通過 | 聚合測試只經 `BoardScenario`；Use Case 測試只經 Input／Output Boundary 與假的 Repository；轉接程式測試只經 Gateway 介面。沒有測試讀私有欄位。備註：`EventStoreContract.介面沒有更新與刪除` 用反射檢查介面的方法名稱，檢查的是公開介面，不是內部結構 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-09 | 通過 | 領域模型的公開名稱都在用語表（`GlossaryTest`）。非公開的輔助類別 `Text`、`Ordered` 不是業務概念，沒有 Manager／Helper／Data 這類名稱 |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-10 | 通過 | 14 個指令 Use Case 都只透過 `BoardCommand` 載入並儲存一個看板（另有 `ONE_AGGREGATE_PER_COMMAND` 規則） |
| 2026-11-14 | `460bbda` 之後的工作區（Task 18） | R-11 | 通過 | Use Case 只有「取出→呼叫聚合一個方法→存回」；Controller 只組 Request（數字為空時丟 IllegalArgumentException，屬於輸入格式檢查，不是業務判斷）；Presenter 只查表轉文字；Projector 照抄事件、不重新檢查規則（`ProjectorsTest.Projection不重新檢查業務規則_事件說了算`） |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-01 | 通過 | 流程分析的 `FlowAnalysisView`、`StageTable`、`StuckList` 只顯示 ViewModel 的字串。備註：`StageTable.choose` 送出後把下拉選單放回「改成…」，是畫面狀態；`StuckList` 用標題加時間當 Vue 的 key，不顯示給人看。ESLint 的格式化限制對流程分析的畫面與元件也會擋（`screensDoNotFormat.spec.ts`，先縮小限制看到失敗再還原） |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-02 | 通過 | `FlowAnalysisView.run` 在改階段、儲存門檻、取消門檻之後都重新讀取；四支元件測試都斷言重讀了一次。沒有先改畫面資料的程式 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-03 | 通過 | 前端不檢查天數範圍：0 天照樣送出，由後端回「卡住門檻要是 1 到 365 天」（`被拒絕_顯示訊息`）。「取消門檻」按鈕依 ViewModel 的 `hasStuckThreshold` 顯示；每一欄都可以改階段，能不能改由後端決定 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-14 | 通過 | 階段的中文、平均停留的天數、每週的日期、卡住的起始時間、交付摘要都由 `FlowMetricsPresenter` 整理，畫面原樣顯示；前端不判斷算不算完成、算不算卡住。備註：門檻輸入框一開始是空的、送出數字，畫面沒有用 `stuckThresholdDays` 預填（預填要把字串轉成數字，畫面不做轉換；見第二階段 Task 16 的裁決） |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-04 | 通過 | 流程規則只在 `flow-analysis/entities`（`BoardFlow`、`FlowRules`、值物件）；畫面文字只在 `CommandPresenter`、`FlowMetricsPresenter`；儲存方式只在 `adapters.eventstore`、`adapters.projection`、防腐層的 `ActivityPositionStore` 兩個實作。翻譯只在防腐層 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-05 | 通過 | 加入流程分析沒有修改看板管理既有的 Use Case、Controller、Presenter；看板管理只新增「讀取看板動態」查詢與看板動態的 Projection。修改到的既有檔案（`git diff --name-status` 逐一核對）只有：`BadRequestHandling` 的範圍（裁決 4，行為不變，`看板管理的端點_請求讀不懂時仍回看板管理的訊息` 證明）、Use Cases 圈的 `module-info` 多公開新的查詢套件、前端進入點搬到 `main/web-ui`（設計第 8 節），以及測試 `WriteSideSchemaTest` 照 4d 加上新的表名。流程分析每個指令一個 Presenter（`ONE_PRESENTER_PER_OUTPUT`）。備註：新增 Use Case 仍須在 Main、薄殼與允許清單各加一段，屬於預期中的擴充點 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-06 | 通過 | 同一套契約對兩個實作各跑一次：流程分析 `EventStoreContract`、`ReadModelStoreContract`（含統計方式 S1、S2）、防腐層 `ActivityPositionStoreContract`、看板管理 `BoardActivityLogStoreContract`；`FlowAnalysisInMemoryModeIntegrationTest` 證明整套流程分析換成記憶體儲存照常運作 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-07 | 通過 | 流程分析一圈一個模組，防腐層獨立成 `board-activity-translation` 模組，所以流程分析的 Use Cases 與 Interface Adapters 圈不必相依看板管理；`FlowMetricsAssembler` 與兩個讀取模型實作放同一個套件（統計方式改了會一起改）；流程分析的畫面是自己的前端 workspace |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-08 | 通過 | 聚合測試只經 `BoardFlowScenario`（Given 事件／When 指令／Then 事件）；Use Case 測試只經 Input／Output Boundary 與假的 Repository；轉接程式測試只經 Gateway 介面；整合測試對 Gateway 介面的公開方法做替身（例如讓 `activitiesAfter` 多回一筆）。流程分析與 main 的測試沒有讀私有欄位（搜尋 `getDeclaredField`、`setAccessible` 為 0 筆）。用到反射的只有讀公開介面：`EventStoreContract` 用 `getMethods` 檢查事件庫介面沒有更新與刪除、`ReplayTest` 與 `BoardFlowEventJsonTest` 用 `getPermittedSubclasses` 列出全部事件種類，和第一階段的備註同一類 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-09 | 通過 | 流程分析領域模型的公開名稱都在用語表的流程分析一節（`GlossaryTest` 依情境檢查）；類別名稱沒有 Manager、Helper、Data、Util、Service。非公開的輔助類別 `Immutable`、`FlowRules`、`Identifiers`、`DisplayText` 與內部物件 `FlowColumn`、`WorkItem`、`ColumnVisit`、`FlowResult` 都是套件內可見 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-10 | 通過 | 流程分析 5 個指令 Use Case 都只透過 `BoardFlowCommand` 載入並儲存一個看板流程（`OneFlowPerCommandCheckTest`、`ONE_AGGREGATE_PER_COMMAND`） |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-11 | 通過 | 流程分析的 Use Case 只做「取出→呼叫聚合一個方法→存回」。`RecordBoardActivityUseCase` 依變化種類決定呼叫哪個方法、檢查必要欄位、看板流程不存在時遇到「建立」才建立，屬於協調與輸入完整性；重送略過（F14）等規則都在聚合。卡住門檻的範圍由 `StuckThreshold` 值物件檢查。Controller 只組 Request，Presenter 只查表轉文字 |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-12 | 通過 | `FlowBoardProjector` 只照事件記下的值更新：停留時間由聚合寫在 `WorkItemMoved` 裡，Projector 只加總與計數；`FlowProjection` 只依 `WorkItemFlowResultChanged` 的完成時間與交付時間存或刪完成紀錄。週的切法只在 `WeekCalendar`（S1：週一 00:00、Main 傳入時區、最近 8 週含本週）；平均與最長只算 8 週內完成的（S2，記憶體版與 PostgreSQL 版各跑同一套契約） |
| 2026-11-17 | `e7e94cf`（第二階段，流程分析） | R-13 | 通過 | `BoardActivityTranslation` 只把看板動態的欄位對應到流程分析的用語（逐一列出 11 種、沒有 default）；`BoardActivitySync` 只讀取、翻譯、送「記錄看板動態」，依回覆前進、重試（版本衝突 3 次）或停住；看板管理的型別不出防腐層（`RecordBoardActivityRequest` 全是流程分析的用語）。另有 `TRANSLATION_ONLY_TRANSLATES` 與 `CONTEXTS_INTEGRATE_ONLY_THROUGH_PUBLISHED_LANGUAGE` 自動檢查 |

### 2026-11-14 這次審查的說明

- 審查者是撰寫這些程式的同一個 AI（Claude），不是獨立的第二人。獨立審查另由整個分支的最終審查負責，結果會追加在本表。
- 瀏覽器實測（同日）：在瀏覽器實際操作了看板的建立、改名、封存；欄位的新增、改名、拖動調整順序、設定與取消上限、刪除；卡片的新增、改內容、拖到另一欄、拖到已滿的欄（被拒絕並顯示訊息）、欄內拖動排序、刪除；看板現況與卡片移動歷程；重新整理與重啟後端後資料仍在。其中拖放四次是用真實的滑鼠拖放，其餘是在頁面上對真實的輸入框與按鈕觸發輸入與點擊。

### 2026-11-14 獨立審查（整個分支）

審查者：另一個沒有參與撰寫的 AI 審查者（唯讀），範圍是整個分支。它實際跑了後端全部測試與前端檢查，並寫了 8 個「繞過樣本」對專案規則實跑。

- **沒有找到被違反的規則，也沒有會弄壞資料的路徑。**
- **找到 5 處架構檢查可以被實際繞過**（對應 CQRS-4、ES-1、CQRS-5、CA-7、ES-3、CA-8、CA-4）。已全部補強，10 種繞過寫法做成違規樣本，見 `ReviewFindingsBiteTest`。
- **找到 5 個使用者會碰到的行為問題**，處理如下：
  - 名稱含控制字元（例如 NUL）會變成系統錯誤 → 值物件現在直接拒絕，回覆「內容含有不能使用的字元」。
  - 後端出錯時畫面顯示「找不到這個看板」、連不上時畫面空白 → 畫面改為顯示「系統暫時無法處理，請稍後再試」。
  - 事件紀錄裡一筆不認得的事件會連累同一批正常事件、並讓應用程式啟動失敗 → 改為逐筆處理；啟動時補不上只記錯誤、不擋啟動。
  - 收緊驗證規則會讓舊事件讀不回來 → 加了邊界樣本測試，收緊規則時會失敗並提醒先處理舊事件。
  - 程式錯誤會被包裝成「請求內容不完整」→ 現在只有真的缺欄位才這樣回覆，程式錯誤照常回系統錯誤。
- 對 CA-9、CA-11、CA-13、CA-15、DDD-9 這幾項靠審查清單守的規則，獨立審查沒有發現不符合。
- **沒有處理、留待決定的事**（審查者列為次要）：`NO_PACKAGE_CYCLES` 沒有檢查圈內套件循環；`NO_CLOCK_OR_RANDOM_IN_ENTITIES` 擋不到 `new Date()` 與 `SecureRandom`；`AGGREGATE_TESTS_USE_SCENARIO` 只認特定檔名；`ONE_AGGREGATE_PER_COMMAND` 只數 Repository 欄位；`AGGREGATE_INTERNALS_NOT_PUBLIC` 寫死兩個類別名稱；由多個字元組成的表情符號（家庭、國旗）會算成多個字；修改卡片內容時沒帶說明會把說明清空；連續快速操作兩次可能自己撞到版本衝突；請求的 Content-Type 不對時回的是英文訊息；CI 對 PR 會跑兩次。
- **Projection 長期停擺時沒有對外的訊號**：只有錯誤日誌，沒有健康檢查端點（超出第一階段範圍）。
- 後續（2026-11-16）：上面留待決定的事已逐條開成 issue #2～#12。其中前五項架構檢查（#2～#6）已補強，每一種繞過寫法各有違規樣本，見 `docs/traceability.md` 的 CA-12、ES-4、ES-5、DDD-4、DDD-5 列；其餘各項仍是開著的 issue。

### 2026-11-14 修正後的瀏覽器複測

獨立審查的修正改到了前後端程式，先前的瀏覽器實測不能沿用，所以對改到的路徑重新實測（commit `4dc567b`）：

- 建立看板、加欄位、設定上限、加卡片：正常。
- 用真實滑鼠把卡片拖到另一欄：第一張進去；第二張拖到已滿的欄被拒絕，畫面顯示「這一欄已達在製品上限，放不進去」。
- 上限欄位留白就按「設定上限」：畫面顯示「請求內容不完整或格式不正確」。
- 直接對後端送出含 NUL 字元的看板名稱：回 422「內容含有不能使用的字元」。
- 把後端停掉後送出指令、重新整理頁面：畫面顯示「系統暫時無法處理，請稍後再試」，沒有出現「找不到這個看板」；原本畫面上的看板在送出指令失敗時保持不動。
- 沒有重測的項目（程式沒有改到，沿用第一次實測）：看板改名與封存、欄位改名／調整順序／取消上限／刪除、卡片改內容／欄內排序／刪除、卡片移動歷程、重啟後端後資料仍在。

### 2026-11-17 第二階段審查的說明

- 範圍：第二階段（流程分析）在 `e7e94cf` 的程式，加上看板管理為第二階段新增的部分。R-01～R-03 這次只審流程分析的畫面；看板管理的畫面在第二階段只搬了進入點，畫面元件沒有改，沿用 2026-11-14 的紀錄。
- 審查者是撰寫這些程式的同一個 AI（Claude），不是獨立的第二人。獨立審查另由整個分支的最終審查負責，結果會追加在下面。

### 2026-11-17 第二階段瀏覽器實測（commit `e7e94cf`）

用 Chrome（chrome-devtools）對真的後端（PostgreSQL）與前端開發伺服器操作。主目錄另有第一階段的伺服器占用 8080／5173，所以這次後端用 8081、前端用 5174（臨時設定，未提交）。

- **第一次升級（Review Focus 4）**：本機開發資料庫原本是第一階段的資料（6 個看板、52 筆事件、資料庫版本 V2）。第二階段後端啟動時自動升到 V3、建立 `flow_analysis` 的三版結構；看板動態紀錄從第一筆事件補齊 47 筆（另外 5 筆是欄內排序與在製品上限，照設計不公布），處理位置 52；流程分析在幾秒內同步到位置 52，6 個看板都有流程分析。
- **第一階段重走一遍**（前端進入點搬家後）：建看板、看板改名、封存；欄位新增、改名、拖動調整順序、設定與取消在製品上限、刪除；卡片新增、編輯、拖到另一欄、拖到已滿的欄被拒絕（「這一欄已達在製品上限，放不進去」）、欄內拖動排序、刪除；卡片移動歷程。拖放都是真的拖放操作。
- **第二階段**：
  - 從看板頁的「流程分析」連結進入，流程分析頁有「回到看板」。
  - 替每一欄指定階段，表格的階段文字跟著變。
  - 在另一個分頁把卡片拖到「完成」欄，1.5 秒後流程分析的資料跟著變（用流程分析的 API 每 0.2 秒查一次量到的；流程分析頁不會自己更新，要重新整理才看到）。
  - 「完成」欄原本未分類、改成「完成」後，交付立刻變成「完成 1 張」、本週「1 張」（整段歷史重算）。
  - 卡住門檻：填 0 被拒絕（「卡住門檻要是 1 到 365 天」）；設定 5 天顯示「卡住門檻 5 天」與「取消門檻」按鈕；取消後回到「沒有設定卡住門檻」。
  - 在看板上刪除「暫存」欄，1.9 秒後流程分析同步（同樣用 API 量）；在還開著的舊流程分析頁改「暫存」的階段，被拒絕並顯示「這個欄位已經刪除，不能再改階段」，畫面重讀後該欄消失。
  - 剛建立、還沒同步的看板打開流程分析頁，顯示「流程分析還沒準備好，請稍後重新整理」；重新整理後顯示該看板。
  - 封存看板後，流程分析的資料 1 秒內變成「已封存」（用 API 量）。
- **console**：讀到的範圍內沒有 JavaScript 錯誤。範圍只涵蓋第二階段那段的操作：瀏覽器只保留最近幾次頁面載入的紀錄，第一階段重走的那段沒有在當下讀 console，所以那一段的 console 未經查核。被標成 error 的只有瀏覽器對 3 個刻意觸發的 4xx 回覆自動記的網路紀錄（兩個 422：門檻 0 天、改已刪除欄位的階段；一個 404：還沒同步的看板），這是設計上用 HTTP 狀態碼表示「被拒絕」「找不到」。另有「表單欄位沒有 id 或 name」的提示，第一階段的畫面就有，不是錯誤。
- **沒有在瀏覽器驗到的**：卡住清單出現工作項目要真的待滿門檻天數（最少 1 天），瀏覽器實測等不了；由整合測試 `情況3_待滿門檻_檢查後列入卡住_換欄後下一次同步就移出`（把流程分析的時間調到 6 天後）與畫面測試 `顯示各欄階段張數與平均停留_原樣顯示ViewModel字串` 分別證明（L3＋元件測試，不是 L4）。
