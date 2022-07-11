package io.stubbs.truth.generator.testModel;

/**
 * Simple replacmeent for testing type parameter behaiviour, while avoiding the previous use of {@link
 * java.util.Optional} - as Optional has complications due to being involved in the Truth8 support (which contains an
 * {@code OptionalSubject} which gets very confusing).
 *
 * @param <T>
 */
public class ThingWithTypeParam<T> {

    private static final ThingWithTypeParam<?> EMPTY = new ThingWithTypeParam<>(null);
    @SuppressWarnings("unused")
    private final T wrapped;

    public ThingWithTypeParam(T o) {
        wrapped = o;
    }

    public static <T> ThingWithTypeParam<T> empty() {
        //noinspection unchecked
        return (ThingWithTypeParam<T>) EMPTY;
    }
}
