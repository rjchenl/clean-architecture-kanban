package kanban.fixtures.shuffle.entities;

import java.util.Collections;
import java.util.List;

/** 違規樣本（ES-4）：Collections.shuffle(list) 內部自己產生亂數，程式裡看不到 Random。 */
public final class ShufflesCards {

    void shuffle(List<String> cards) {
        Collections.shuffle(cards);
    }
}
