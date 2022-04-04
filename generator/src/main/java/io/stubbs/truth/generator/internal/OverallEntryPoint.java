package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.GeneratorException;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Method;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a single convenience `ManagedTruth` class, which contains all the `assertThat` entrypoint methods.
 *
 * @author Antony Stubbs
 */
@Slf4j
public class OverallEntryPoint {

    private final List<JavaClassSource> children = new ArrayList<>();

    @Getter
    private final String packageName;

    public OverallEntryPoint(String packageForOverall) {
        if (StringUtils.isBlank(packageForOverall))
            throw new GeneratorException("Package for managed entrypoint cannot be blank");

        this.packageName = packageForOverall;
    }

    /**
     * Having collected together all the access points, creates one large class filled with access points to all of
     * them.
     * <p>
     * The overall access will throw an error if any middle classes don't correctly extend their parent.
     */
    public void createOverallAccessPoints() {
        JavaClassSource overallAccess = Roaster.create(JavaClassSource.class);
        overallAccess.setName("ManagedTruth");
        overallAccess.getJavaDoc().setText("Single point of access for all managed Subjects.");
        overallAccess.setPublic().setPackage(packageName);

        // brute force
        for (JavaClassSource child : children) {
            List<MethodSource<JavaClassSource>> methods = child.getMethods();
            for (Method m : methods) {
                if (!m.isConstructor())
                    overallAccess.addMethod(m);
            }
            // this seems like overkill, but at least in the child style case, there's very few imports - even
            // none extra at all (aside from wild card vs specific methods).
            List<Import> imports = child.getImports();
            for (Import i : imports) {
                // roaster just throws up a NPE when this happens
                Set<String> simpleNames = overallAccess.getImports().stream().map(Import::getSimpleName).filter(x -> !x.equals("*")).collect(Collectors.toSet());
                if (simpleNames.contains(i.getSimpleName())) {
                    log.debug("Expected collision of imports - not adding duplicated import {}", i);
                } else {
                    overallAccess.addImport(i);
                }
            }
        }

        Utils.writeToDisk(overallAccess);
    }

    public void add(ThreeSystem<?> ts) {
        this.children.add(ts.child);
    }
}
