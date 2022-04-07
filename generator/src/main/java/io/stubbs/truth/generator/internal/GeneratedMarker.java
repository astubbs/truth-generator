package io.stubbs.truth.generator.internal;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.time.Clock;

@UtilityClass
public class GeneratedMarker {

    /**
     * Setter for testing.
     */
    @Setter
    private static Clock clock = Clock.systemUTC();

    /**
     * Conditionally adds either the pre Java 9 or post Java 9 version of the {@code Generated} annotation.
     *
     * @see Options#releaseTarget
     */
    public void addGeneratedMarker(final JavaClassSource javaClass) {
        AnnotationSource<JavaClassSource> generated;
        if (Options.get().isCompilationTargetLowerThanNine()) {
            generated = javaClass.addAnnotation(javax.annotation.Generated.class);
        } else {
            // requires java 9
            // annotate generated
            // @javax.annotation.Generated(value="")
            // only in @since 1.9, so can't add it programmatically
            generated = javaClass.addAnnotation(javax.annotation.processing.Generated.class);
            // Can't add it without the value param, see https://github.com/forge/roaster/issues/201
        }

        generated.setStringValue("value", TruthGenerator.class.getCanonicalName());
        generated.setStringValue("date", clock.instant().toString());

        //generated.setStringValue("comments", "?")
    }

}
