package kanban.flowanalysis.entities.boardflow;

/** 識別碼共用的檢查：不可空白、不可含控制字元（控制字元不是給人看的內容，儲存設備也可能存不進去）。 */
final class Identifiers {

    private Identifiers() {
    }

    static String check(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessRuleViolation(RejectionReason.IDENTIFIER_BLANK);
        }
        if (raw.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessRuleViolation(RejectionReason.INVALID_CHARACTERS);
        }
        return raw;
    }
}
