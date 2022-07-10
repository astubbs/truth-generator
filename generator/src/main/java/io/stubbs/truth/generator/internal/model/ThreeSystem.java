package io.stubbs.truth.generator.internal.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jboss.forge.roaster.model.source.JavaClassSource;

/**
 * @author Antony Stubbs
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ThreeSystem<T> {

    protected Class<T> classUnderTest;
    protected ParentClass parent;
    protected MiddleClass middle;
    protected JavaClassSource child;
    @Setter
    boolean legacyMode = false;

    /**
     * Used for equality - {@link Class}s loaded by different class loaders will be different instances
     *
     * @return {@link Class#getCanonicalName()}
     */
    @EqualsAndHashCode.Include
    public String getClassUnderTestCanonicalName() {
        return this.classUnderTest.getCanonicalName();
    }

    /**
     * @see io.stubbs.truth.generator.internal.SkeletonGenerator#threeLayerSystem
     */
    // todo make protected?
    public ThreeSystem(Class<T> classUnderTest, ParentClass parent, MiddleClass middle, JavaClassSource child) {
        this.classUnderTest = classUnderTest;
        this.parent = parent;
        this.middle = middle;
        this.child = child;
    }

    @Override
    public String toString() {
        return "ThreeSystem{" +
                "classUnderTest=" + classUnderTest + '}';
    }

    public boolean isShaded() {
        return !packagesAreContained();
    }

    // todo rename
    private boolean packagesAreContained() {
        Package underTestPackage = classUnderTest.getPackage();
        String subjectPackage = parent.getGenerated().getPackage();
        return underTestPackage.getName().contains(subjectPackage);
    }

}
