package io.stubbs.truth.generator.internal.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Antony Stubbs
 */
@Getter
//@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ThreeSystem<T> {

    @EqualsAndHashCode.Include
    protected Class<T> classUnderTest;
    protected ParentClass parent;
    protected MiddleClass middle;
    protected JavaClassSource child;
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

    Set<ThreeSystem<?>> union = new HashSet<>();

}
