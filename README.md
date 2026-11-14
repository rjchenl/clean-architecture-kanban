# clean-architecture-todolist

用一個 Todo List 應用，完整實踐 Robert C. Martin《Clean Architecture》（無瑕的程式碼：整潔的軟體設計與架構篇）的設計原則。

## 目標

- 以最簡單的業務（待辦事項）當載體，把注意力放在架構本身。
- 遵守相依性規則（The Dependency Rule）：原始碼的相依方向只能由外向內，內層不知道外層的任何事。
- 業務規則不依賴框架、UI、資料庫或任何外部代理，並且可以獨立測試。

## 預計的分層

由內到外：

1. **Entities**：企業級業務規則。
2. **Use Cases**：應用程式專屬的業務規則。
3. **Interface Adapters**：Controllers、Presenters、Gateways。
4. **Frameworks & Drivers**：Web、資料庫、UI 等細節。

## 狀態

專案剛建立，尚未選定語言與框架，也還沒有程式碼。
