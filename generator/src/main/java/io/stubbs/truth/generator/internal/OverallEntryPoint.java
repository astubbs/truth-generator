package io.stubbs.truth.generator.internal;

import com.google.common.truth.StandardSubjectBuilder;
import com.google.common.truth.Subject;
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

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static io.stubbs.truth.generator.internal.AssertionEntryPointGenerator.ASSERT_WITH_MESSAGE;

/**
 * Creates a single convenience `ManagedTruth` class, which contains all the `assertThat` entrypoint methods.
 *
 * @author Antony Stubbs
 */
@Slf4j
public class OverallEntryPoint {

    /**
     * {@link TreeSet} just to keep it sorted, so that the final output is in a stable order.
     */
    private final TreeSet<ThreeSystem<?>> threeSystemChildSubjects;

    @Getter
    private final String packageName;
    private final AssertionEntryPointGenerator aepg = new AssertionEntryPointGenerator();
    private final BuiltInSubjectTypeStore builtInSubjectTypeStore = new BuiltInSubjectTypeStore();
    @Getter
    private JavaClassSource overallEntryPointGenerated;
    @Getter
    private JavaClassSource managedSubjectBuilderGenerated;

    public OverallEntryPoint(String packageForOverall) {
        if (StringUtils.isBlank(packageForOverall))
            throw new GeneratorException("Package for managed entrypoint cannot be blank");

        this.packageName = packageForOverall;
        this.threeSystemChildSubjects = new TreeSet<>(Comparator.comparing(javaClassSource -> javaClassSource.getClassUnderTest().getCanonicalName()));
    }

    public void create() {
        createManagedSubjectBuilder();
        createOverallAccessPoints();
    }

    private void createManagedSubjectBuilder() {
        JavaClassSource managedSubjectBuilder = Roaster.create(JavaClassSource.class)
                .setName("ManagedSubjectBuilder")
                .setPublic()
                .setPackage(packageName);

        managedSubjectBuilder.getJavaDoc()
                .addTagValue("see", "{@link StandardSubjectBuilder}");

        //
        managedSubjectBuilder.addField()
                .setName("standardSubjectBuilder")
                .setType(StandardSubjectBuilder.class);

        //
        managedSubjectBuilder.addMethod()
                .setConstructor(true)
                .setBody("this.standardSubjectBuilder = standardSubjectBuilder;")
                .addParameter(StandardSubjectBuilder.class, "standardSubjectBuilder");


        for (var ts : threeSystemChildSubjects) {
            addThatForSubject(managedSubjectBuilder, ts);
        }

        this.managedSubjectBuilderGenerated = managedSubjectBuilder;

        GeneratedMarker.addGeneratedMarker(managedSubjectBuilder);

        Utils.writeToDisk(managedSubjectBuilder);
    }

    /**
     * @see com.google.common.truth.StandardSubjectBuilder#that
     */
    private <T> void addThatForSubject(JavaClassSource overallAccess, ThreeSystem<T> ts) {
        //        public MyEmployeeSubject that(MyEmployee actual) {
        //            return standardSubjectBuilder.about(MyEmployeeChildSubject.myEmployees()).that(actual);
        //        }

        MethodSource<JavaClassSource> that = overallAccess.addMethod()
                .setName("that")
                .setPublic();

        //
        that.addParameter(ts.classUnderTest, "actual");

        //
        String subjectCanonicalName = ts.getMiddle().getCanonicalName();
        that.setReturnType(subjectCanonicalName);

        //
        String factoryEnclosing = ts.getMiddle().getCanonicalName();
        String factoryName = ts.getMiddle().getFactoryMethodName();
        that.setBody("return standardSubjectBuilder.about(" + factoryEnclosing + "." + factoryName + "()).that(actual);");

        //
        that.getJavaDoc().addTagValue("see", "{@link " + subjectCanonicalName + "}");
    }

    /**
     * Having collected together all the access points, creates one large class filled with access points to all of
     * them.
     * <p>
     * The overall access will throw an error if any middle classes don't correctly extend their parent.
     */
    protected void createOverallAccessPoints() {
        JavaClassSource overallAccess = Roaster.create(JavaClassSource.class);
        overallAccess.setName("ManagedTruth");
        overallAccess.getJavaDoc().setText("Single point of access for all managed Subjects.");
        overallAccess.setPublic().setPackage(packageName);

        aepg.addWithMessage(packageName, overallAccess);

        addChildEntryPoints(overallAccess);

        addStaticEntryPoints(overallAccess);

        copyStaticEntryPointsFromGTruthEntryPoint();

        GeneratedMarker.addGeneratedMarker(overallAccess);

        Utils.writeToDisk(overallAccess);

        this.overallEntryPointGenerated = overallAccess;
    }

    private void addChildEntryPoints(JavaClassSource overallAccess) {
        // brute force - add both the assertTruth and assertThat generated entry points
        for (var ts : threeSystemChildSubjects) {
            var child = ts.getChild();

            List<MethodSource<JavaClassSource>> methods = child.getMethods();
            for (Method<?, ?> m : methods) {
                if (!m.isConstructor() && !m.getName().equals(ASSERT_WITH_MESSAGE))
                    overallAccess.addMethod(m);
            }
            // this seems like overkill, but at least in the child style case, there's very few imports - even
            // none extra at all (aside from wild card vs specific methods).
            List<Import> imports = child.getImports();
            for (Import anImport : imports) {
                // roaster just throws up a NPE when this happens
                Set<String> simpleNames = overallAccess.getImports().stream().map(Import::getSimpleName).filter(x -> !x.equals("*")).collect(Collectors.toSet());
                if (simpleNames.contains(anImport.getSimpleName())) {
                    log.debug("Expected collision of imports - not adding duplicated import {}", anImport);
                } else if (overallAccess.getName().equals(anImport.getSimpleName())) {
                    // skip
                } else {
                    overallAccess.addImport(anImport);
                }
            }
        }
    }

    private void addStaticEntryPoints(JavaClassSource overallAccess) {
        var allSubjectExtensions = builtInSubjectTypeStore.getAllSubjectExtensions();
        for (var entry : allSubjectExtensions.entrySet()) {
            var classTargetOfSubject = entry.getKey();
            Class<? extends Subject> subject = allSubjectExtensions.get(classTargetOfSubject);
            java.lang.reflect.Method factoryMethod = Utils.findFactoryMethod(subject);
            MethodSource<JavaClassSource> that = aepg.addAssertThat(classTargetOfSubject, overallAccess, factoryMethod.getName(), subject.getCanonicalName());
            aepg.addAssertTruth(classTargetOfSubject, overallAccess, that);
        }
    }

    /**
     * ManagedTruth should also have entry points for all Truth8 and Truth assertions #74
     * <p>
     * TODO https://github.com/astubbs/truth-generator/issues/74
     *
     * @see com.google.common.truth.Truth#assertThat
     * @see com.google.common.truth.Truth8#assertThat
     */
    private void copyStaticEntryPointsFromGTruthEntryPoint() {
        // for each method in
        // add the same method
    }

    public void add(ThreeSystem<?> ts) {
        this.threeSystemChildSubjects.add(ts);
    }
}
