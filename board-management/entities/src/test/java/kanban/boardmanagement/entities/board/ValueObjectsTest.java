package kanban.boardmanagement.entities.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Function;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValueObjectsTest {

    private static void assertRejected(ThrowingCallable creation, RejectionReason expected) {
        assertThatThrownBy(creation)
                .isInstanceOfSatisfying(BusinessRuleViolation.class,
                        violation -> assertThat(violation.reason()).isEqualTo(expected));
    }

    static Stream<Arguments> names() {
        return Stream.of(
                Arguments.of("看板名稱", (Function<String, String>) raw -> new BoardName(raw).value()),
                Arguments.of("欄位名稱", (Function<String, String>) raw -> new ColumnName(raw).value()),
                Arguments.of("卡片標題", (Function<String, String>) raw -> new CardTitle(raw).value()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    void 名稱只有空白字元_拒絕(String label, Function<String, String> create) {
        assertRejected(() -> create.apply("   "), RejectionReason.NAME_BLANK);
        assertRejected(() -> create.apply(""), RejectionReason.NAME_BLANK);
        assertRejected(() -> create.apply("\u3000\t"), RejectionReason.NAME_BLANK);
        assertRejected(() -> create.apply(null), RejectionReason.NAME_BLANK);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    void 名稱去掉頭尾空白後保存(String label, Function<String, String> create) {
        assertThat(create.apply("  產品開發 ")).isEqualTo("產品開發");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    void 名稱剛好100字可以_101字拒絕(String label, Function<String, String> create) {
        assertThat(create.apply("字".repeat(100))).hasSize(100);
        assertRejected(() -> create.apply("字".repeat(101)), RejectionReason.NAME_TOO_LONG);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    void 一個表情符號算一個字(String label, Function<String, String> create) {
        assertThat(create.apply("😀".repeat(100))).isEqualTo("😀".repeat(100));
        assertRejected(() -> create.apply("😀".repeat(101)), RejectionReason.NAME_TOO_LONG);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("names")
    void 長度以去掉頭尾空白後計算(String label, Function<String, String> create) {
        assertThat(create.apply(" " + "字".repeat(100) + " ")).hasSize(100);
    }

    @Test
    void 卡片說明可以空白_null視為空字串() {
        assertThat(new CardDescription(null).value()).isEmpty();
        assertThat(new CardDescription("").value()).isEmpty();
        assertThat(new CardDescription("第一行\n第二行").value()).isEqualTo("第一行\n第二行");
    }

    @Test
    void 卡片說明剛好2000字可以_2001字拒絕() {
        assertThat(new CardDescription("字".repeat(2000)).value()).hasSize(2000);
        assertThat(new CardDescription("😀".repeat(2000)).value()).isEqualTo("😀".repeat(2000));
        assertRejected(() -> new CardDescription("字".repeat(2001)), RejectionReason.DESCRIPTION_TOO_LONG);
    }

    @Test
    void 識別碼不可空白() {
        assertRejected(() -> new BoardId(" "), RejectionReason.IDENTIFIER_BLANK);
        assertRejected(() -> new ColumnId(null), RejectionReason.IDENTIFIER_BLANK);
        assertRejected(() -> new CardId(""), RejectionReason.IDENTIFIER_BLANK);
        assertThat(new BoardId("board-1").value()).isEqualTo("board-1");
    }

    @Test
    void 在製品上限必須是1以上() {
        assertThat(new WipLimit(1).value()).isEqualTo(1);
        assertRejected(() -> new WipLimit(0), RejectionReason.WIP_LIMIT_NOT_POSITIVE);
        assertRejected(() -> new WipLimit(-3), RejectionReason.WIP_LIMIT_NOT_POSITIVE);
    }

    @Test
    void 位置從0起算_不可為負() {
        assertThat(new Position(0).index()).isZero();
        assertRejected(() -> new Position(-1), RejectionReason.POSITION_OUT_OF_RANGE);
    }

    @Test
    void 值物件以值比較() {
        assertThat(new ColumnId("col-1")).isEqualTo(new ColumnId("col-1")).hasSameHashCodeAs(new ColumnId("col-1"));
        assertThat(new ColumnId("col-1")).isNotEqualTo(new ColumnId("col-2"));
        assertThat(new BoardName(" 產品開發 ")).isEqualTo(new BoardName("產品開發"));
        assertThat(new WipLimit(3)).isEqualTo(new WipLimit(3));
        assertThat(new Position(2)).isEqualTo(new Position(2));
    }
}
