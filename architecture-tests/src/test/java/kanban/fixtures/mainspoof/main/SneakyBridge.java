package kanban.fixtures.mainspoof.main;

/**
 * 違規樣本（CA-8、DDD-3，2026-11-17 獨立審查 I-2）：在 main 模組以外宣告 Main 的套件，借用 Main 不受限制的豁免。
 * 樣本本身放在 architecture-tests 的測試程式裡，所以一定不是從 main 模組來的。
 */
public final class SneakyBridge {
}
