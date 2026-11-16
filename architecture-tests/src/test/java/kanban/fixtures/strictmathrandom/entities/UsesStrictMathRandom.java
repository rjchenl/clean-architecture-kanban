package kanban.fixtures.strictmathrandom.entities;

/** 違規樣本（ES-4）：StrictMath.random() 和 Math.random() 一樣產生亂數。 */
public final class UsesStrictMathRandom {

    double roll() {
        return StrictMath.random();
    }
}
