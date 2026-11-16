package kanban.fixtures.securerandom.entities;

import java.security.SecureRandom;

/** 違規樣本（ES-4）：用 SecureRandom（Random 的子類別）產生亂數。 */
public final class MakesSecureRandomId {

    int id() {
        return new SecureRandom().nextInt();
    }
}
