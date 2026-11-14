package kanban.boardmanagement.entities.board;

/** 值物件共用的文字檢查。長度以 Unicode code point 計，一個表情符號算一個字。 */
final class Text {

    static final int NAME_MAX_LENGTH = 100;
    static final int DESCRIPTION_MAX_LENGTH = 2000;

    private Text() {
    }

    static String identifier(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleViolation(RejectionReason.IDENTIFIER_BLANK);
        }
        requireNoControlCharacters(raw, false);
        return raw;
    }

    static String name(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleViolation(RejectionReason.NAME_BLANK);
        }
        String stripped = raw.strip();
        requireNoControlCharacters(stripped, false);
        if (lengthOf(stripped) > NAME_MAX_LENGTH) {
            throw new BusinessRuleViolation(RejectionReason.NAME_TOO_LONG);
        }
        return stripped;
    }

    static String description(String raw) {
        String text = raw == null ? "" : raw;
        requireNoControlCharacters(text, true);
        if (lengthOf(text) > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessRuleViolation(RejectionReason.DESCRIPTION_TOO_LONG);
        }
        return text;
    }

    /**
     * 控制字元（例如 NUL）不是給人看的內容，儲存設備也可能存不進去，所以一律不收。
     * 多行文字（卡片說明）另外允許換行與定位字元。
     */
    private static void requireNoControlCharacters(String text, boolean multiline) {
        boolean invalid = text.codePoints().anyMatch(codePoint -> Character.isISOControl(codePoint)
                && !(multiline && (codePoint == '\n' || codePoint == '\r' || codePoint == '\t')));
        if (invalid) {
            throw new BusinessRuleViolation(RejectionReason.INVALID_CHARACTERS);
        }
    }

    private static int lengthOf(String text) {
        return text.codePointCount(0, text.length());
    }
}
