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

### 2026-11-14 這次審查的說明

- 審查者是撰寫這些程式的同一個 AI（Claude），不是獨立的第二人。獨立審查另由整個分支的最終審查負責，結果會追加在本表。
- 瀏覽器實測（同日）：在瀏覽器實際操作了看板的建立、改名、封存；欄位的新增、改名、拖動調整順序、設定與取消上限、刪除；卡片的新增、改內容、拖到另一欄、拖到已滿的欄（被拒絕並顯示訊息）、欄內拖動排序、刪除；看板現況與卡片移動歷程；重新整理與重啟後端後資料仍在。其中拖放四次是用真實的滑鼠拖放，其餘是在頁面上對真實的輸入框與按鈕觸發輸入與點擊。
