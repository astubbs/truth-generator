package io.stubbs.truth.generator.internal.model;

import lombok.Getter;
import lombok.Setter;
import org.jboss.forge.roaster.model.source.JavaClassSource;

@Getter
public class ThreeSystem<T> {

    public Class<T> classUnderTest;
    public ParentClass parent;
    public MiddleClass middle;
    public JavaClassSource child;
    @Setter
    boolean legacyMode = false;

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
