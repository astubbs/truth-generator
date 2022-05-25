package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.TruthGeneratorAPI;
import io.stubbs.truth.generator.internal.model.ThreeSystem;

import java.io.FileNotFoundException;
import java.util.Optional;

/**
 * @author Antony Stubbs
 */
public interface SkeletonGeneratorAPI {

    /**
     * @see TruthGeneratorAPI#maintain(Class, Class)
     */
    String maintain(Class<?> source, Class<?> userAndGeneratedMix);

    /**
     * Uses an optional three layer system to manage the Subjects.
     * <ol>
     * <li> The top layer extends Subject and stores the actual and factory.
     * <li> The second layer is the user's code - they extend the first layer, and add their custom assertion methods there.
     * <li> The third layer extends the user's class, and stores the generated entry points, so that users's get's access
     * to all three layers, by only importing the bottom layer.
     * </ol>
     * <p>
     * For any source class that doesn't have a user created middle class, an empty one will be generated, that the user
     * can copy into their source control. If it's used, a helpful message will be written to the console, prompting the
     * user to do so. These messages can be globally disabled.
     * <p>
     * This way there's no complexity with mixing generated and user written code, but at the cost of 2 extra classes
     * per source. While still allowing the user to leverage the full code generation system but maintaining their own extensions
     * with clear separation from the code generation.
     *
     * @return
     */
    <T> Optional<ThreeSystem<T>> threeLayerSystem(Class<T> source, Class<? extends Subject> usersMiddleClass) throws FileNotFoundException;

    /**
     * Create the placeholder middle class, for optional copying into source code
     *
     * @return null if for some reason the class isn't supported
     * @see #threeLayerSystem(Class, Class)
     */
    <T> Optional<ThreeSystem<T>> threeLayerSystem(Class<T> source);

    /**
     * @see TruthGeneratorAPI#combinedSystem(Class)
     */
    <T> String combinedSystem(Class<T> source);
}
