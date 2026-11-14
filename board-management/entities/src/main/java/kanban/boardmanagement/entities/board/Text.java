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
        return raw;
    }

    static String name(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleViolation(RejectionReason.NAME_BLANK);
        }
        String stripped = raw.strip();
        if (lengthOf(stripped) > NAME_MAX_LENGTH) {
            throw new BusinessRuleViolation(RejectionReason.NAME_TOO_LONG);
        }
        return stripped;
    }

    static String description(String raw) {
        String text = raw == null ? "" : raw;
        if (lengthOf(text) > DESCRIPTION_MAX_LENGTH) {
            throw new BusinessRuleViolation(RejectionReason.DESCRIPTION_TOO_LONG);
        }
        return text;
    }

    private static int lengthOf(String text) {
        return text.codePointCount(0, text.length());
    }
}
