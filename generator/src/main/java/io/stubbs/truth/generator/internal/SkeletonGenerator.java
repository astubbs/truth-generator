package io.stubbs.truth.generator.internal;

import com.google.common.base.Joiner;
import com.google.common.flogger.FluentLogger;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import io.stubbs.truth.generator.UserManagedTruth;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import io.stubbs.truth.generator.internal.model.ParentClass;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import lombok.Setter;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.time.Clock;
import java.util.Optional;

import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Generates skeleton source class files so that a reference graph can be correctly created, before adding assertion
 * methods (see {@link SubjectMethodGenerator}
 *
 * @author Antony Stubbs
 * @see SubjectMethodGenerator
 */
public class SkeletonGenerator implements SkeletonGeneratorAPI {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private static final String BACKUP_PACKAGE = "io.stubbs.common.truth.extension.generator";

    /**
     * For testing. Used to force generating of middle class, even if it's detected.
     */
    public static boolean forceMiddleGenerate;
    @Setter
    private static Clock clock = Clock.systemUTC();
    private final OverallEntryPoint overallEntryPoint;
    private final BuiltInSubjectTypeStore subjectTypeStore;
    private Optional<String> targetPackageName;
    private MiddleClass middle;
    private ParentClass parent;
    @Setter
    private boolean legacyMode = false;

    public SkeletonGenerator(Optional<String> targetPackageName, OverallEntryPoint overallEntryPoint, BuiltInSubjectTypeStore subjectTypeStore) {
        this.targetPackageName = targetPackageName;
        this.overallEntryPoint = overallEntryPoint;
        this.subjectTypeStore = subjectTypeStore;
    }

    @Override
    public String maintain(Class<?> source, Class<?> userAndGeneratedMix) {
        throw new IllegalStateException("Not implemented yet");
    }

    /**
     * @return if possible, a {@link ThreeSystem} using an already existing {@link MiddleClass}
     */
    @Override
    public <T> Optional<ThreeSystem<T>> threeLayerSystem(Class<T> source, Class<T> usersMiddleClass) {
        if (SourceChecking.checkSource(source, empty()))
            return empty();

        // make parent - boilerplate access
        ParentClass parent = createParent(source);

        String factoryMethodName = Utils.getFactoryName(source);

        // make child - client code entry point
        JavaClassSource child = createChild(parent, usersMiddleClass.getName(), source, factoryMethodName);

        MiddleClass middleClass = MiddleClass.of(usersMiddleClass);

        return of(new ThreeSystem<>(source, parent, middleClass, child));
    }

    @Override
    public <T> Optional<ThreeSystem<T>> threeLayerSystem(Class<T> clazzUnderTest) {
        if (SourceChecking.checkSource(clazzUnderTest, targetPackageName))
            return empty();

        // todo make sure this doesn't override explicit shading settings
        if (SourceChecking.needsShading(clazzUnderTest)) {
            targetPackageName = of(this.overallEntryPoint.getPackageName() + ".autoShaded." + clazzUnderTest.getPackage().getName());
        }

        ParentClass parent = createParent(clazzUnderTest);
        this.parent = parent;

        MiddleClass middle = createMiddleUserTemplate(parent.getGenerated(), clazzUnderTest);
        this.middle = middle;

        String factoryName = Utils.getFactoryName(clazzUnderTest);
        JavaClassSource child = createChild(parent, middle.getSimpleName(), clazzUnderTest, factoryName);

        return of(new ThreeSystem<>(clazzUnderTest, parent, middle, child));
    }

    private MiddleClass createMiddleUserTemplate(JavaClassSource parent, Class source) {
        String middleClassName = getSubjectName(source.getSimpleName());

        Optional<Class<?>> compiledMiddleClass = middleExists(parent, middleClassName, source);
        if (compiledMiddleClass.isPresent()) {
            logger.atInfo().log("Skipping middle class Template creation as class already exists: %s", middleClassName);
            return MiddleClass.of(compiledMiddleClass.get());
        }

        JavaClassSource middle = Roaster.create(JavaClassSource.class);
        middle.setName(middleClassName);
        middle.setPackage(parent.getPackage());
        middle.extendSuperType(parent);
        JavaDocSource<JavaClassSource> jd = middle.getJavaDoc();
        jd.setText("Optionally move this class into source control, and add your custom assertions here.\n\n" +
                "<p>If the system detects this class already exists, it won't attempt to generate a new one. Note that " +
                "if the base skeleton of this class ever changes, you won't automatically get it updated.");
        jd.addTagValue("@see", source.getSimpleName());
        jd.addTagValue("@see", parent.getName());

        addConstructor(source, middle, false);

        middle.addAnnotation(UserManagedTruth.class).setClassValue(source);

        MethodSource factory = addFactoryAccesor(source, middle, source.getSimpleName());

        addGeneratedMarker(middle);

        Utils.writeToDisk(middle, targetPackageName);
        return MiddleClass.of(middle, factory);
    }

    private Optional<Class<?>> middleExists(JavaClassSource parent, String middleClassName, Class source) {
        if (forceMiddleGenerate)
            return empty();

        try {
            // load from annotated classes instead using Reflections?
            String fullName = parent.getPackage() + "." + middleClassName;
            Class<?> aClass = Class.forName(fullName);
            return of(aClass);
        } catch (ClassNotFoundException e) {
            return empty();
        }
    }

    private <T> MethodSource<JavaClassSource> addFactoryAccesor(Class<T> source, JavaClassSource javaClass, String sourceName) {
        String factoryName = Utils.getFactoryName(source);
        if (containsMethodCalled(javaClass, factoryName)) {
            return getMethodCalled(javaClass, factoryName);
        } else {
            // factory accessor
            String returnType = getTypeWithGenerics(Subject.Factory.class, javaClass.getName(), sourceName);
            MethodSource<JavaClassSource> factory = javaClass.addMethod()
                    .setName(factoryName)
                    .setPublic()
                    .setStatic(true)
                    // todo replace with something other than the string method - I suppose it's not possible to do generics type safely
                    .setReturnType(returnType)
                    .setBody("return " + javaClass.getName() + "::new;");
            JavaDocSource<MethodSource<JavaClassSource>> factoryDocs = factory.getJavaDoc();
            factoryDocs.setText("Returns an assertion builder for a {@link " + sourceName + "} class.");
            return factory;
        }
    }

    private String getTypeWithGenerics(Class<?> factoryClass, String... classes) {
        String genericsList = Joiner.on(", ").skipNulls().join(classes);
        String generics = new StringBuilder("<>").insert(1, genericsList).toString();
        return factoryClass.getSimpleName() + generics;
    }

    @Override
    public <T> String combinedSystem(Class<T> clazzUnderTest) {
        JavaClassSource javaClass = Roaster.create(JavaClassSource.class);

//        JavaClassSource handWrittenExampleCode = Roaster.parse(JavaClassSource.class, handWritten);

//        registerManagedClass(clazzUnderTest, handWrittenExampleCode);

//        javaClass = handWrittenExampleCode;

        String packageName = clazzUnderTest.getPackage().getName();
        String sourceName = clazzUnderTest.getSimpleName();
        String subjectClassName = getSubjectName(sourceName);


        addPackageSuperAndAnnotation(clazzUnderTest, javaClass, packageName);

        addClassJavaDoc(javaClass, sourceName);

        addActualField(clazzUnderTest, javaClass);

        addConstructor(clazzUnderTest, javaClass, true);

        MethodSource<JavaClassSource> factory = addFactoryAccesor(clazzUnderTest, javaClass, sourceName);

        addAccessPoints(clazzUnderTest, javaClass, factory.getName(), javaClass.getQualifiedName());

        // todo add static import for Truth.assertAbout somehow?
//        Import anImport = javaClass.addImport(Truth.class);
//        javaClass.addImport(anImport.setStatic(true));
//        javaClass.addImport(new Im)

        String classSource = Utils.writeToDisk(javaClass, targetPackageName);

        return classSource;
    }

//    private <T> void registerManagedClass(Class<T> sourceClass, JavaClassSource gengeratedClass) {
//        managedSubjects.add(new ManagedClassSet(sourceClass, gengeratedClass));
//    }

    private <T> ParentClass createParent(Class<T> clazzUnderTest) {
        JavaClassSource parent = Roaster.create(JavaClassSource.class);
        String sourceName = clazzUnderTest.getSimpleName();
        String parentName = getSubjectName(sourceName + "Parent");
        parent.setName(parentName);

        addPackageSuperAndAnnotation(parent, clazzUnderTest);

        addClassJavaDoc(parent, sourceName);

        addActualField(clazzUnderTest, parent);

        addConstructor(clazzUnderTest, parent, true);

        Utils.writeToDisk(parent, targetPackageName);
        return new ParentClass(parent);
    }

    private JavaClassSource createChild(ParentClass parent,
                                        String usersMiddleClassName,
                                        Class<?> source,
                                        String factoryMethodName) {
        // todo if middle doesn't extend parent, warn

        JavaClassSource child = Roaster.create(JavaClassSource.class);
        child.setName(getSubjectName(source.getSimpleName() + "Child"));
        child.setPackage(parent.getGenerated().getPackage());
        JavaDocSource<JavaClassSource> javaDoc = child.getJavaDoc();
        javaDoc.setText("Entry point for assertions for @{" + source.getSimpleName() + "}. Import the static accessor methods from this class and use them.\n" +
                "Combines the generated code from {@" + parent.getGenerated().getName() + "}and the user code from {@" + usersMiddleClassName + "}.");
        javaDoc.addTagValue("@see", source.getName());
        javaDoc.addTagValue("@see", usersMiddleClassName);
        javaDoc.addTagValue("@see", parent.getGenerated().getName());

        middle.makeChildExtend(child);

        MethodSource<JavaClassSource> constructor = addConstructor(source, child, false);
        constructor.getJavaDoc().setText("This constructor should not be used, instead see the parent's.")
                .addTagValue("@see", usersMiddleClassName);
        constructor.setPrivate();

        addAccessPoints(source, child, factoryMethodName, middle.getCanonicalName());

        addGeneratedMarker(child);

        Utils.writeToDisk(child, targetPackageName);
        return child;
    }

    private String getSubjectName(final String sourceName) {
        return sourceName + "Subject";
    }

    private void addPackageSuperAndAnnotation(final JavaClassSource javaClass, final Class<?> clazzUnderTest) {
        addPackageSuperAndAnnotation(clazzUnderTest, javaClass, clazzUnderTest.getPackage().getName());
    }

    private void addClassJavaDoc(JavaClassSource javaClass, String sourceName) {
        // class javadc
        JavaDocSource<JavaClassSource> classDocs = javaClass.getJavaDoc();
        if (classDocs.getFullText().isEmpty()) {
            classDocs.setText("Truth Subject for the {@link " + sourceName + "}." +
                    "\n\n" +
                    "Note that this class is generated / managed, and will change over time. So any changes you might " +
                    "make will be overwritten.");
            classDocs.addTagValue("@see", sourceName);
            classDocs.addTagValue("@see", getSubjectName(sourceName));
            classDocs.addTagValue("@see", getSubjectName(sourceName + "Child"));
        }
    }

    private <T> void addActualField(Class<T> source, JavaClassSource javaClass) {
        String fieldName = "actual";
        if (javaClass.getField(fieldName) == null) {
            // actual field
            javaClass.addField()
                    .setProtected()
                    .setType(source)
                    .setName(fieldName)
                    .setFinal(true);
        }
    }

    private <T> MethodSource<JavaClassSource> addConstructor(Class<?> source, JavaClassSource javaClass, boolean setActual) {
        if (!javaClass.getMethods().stream().anyMatch(x -> x.isConstructor())) {
            // constructor
            MethodSource<JavaClassSource> constructor = javaClass.addMethod()
                    .setConstructor(true)
                    .setProtected();
            constructor.addParameter(FailureMetadata.class, "failureMetadata");
            constructor.addParameter(source, "actual");
            StringBuilder sb = new StringBuilder("super(failureMetadata, actual);\n");
            if (setActual)
                sb.append("this.actual = actual;");
            constructor.setBody(sb.toString());
            return constructor;
        }
        return null;
    }

    private <T> void addAccessPoints(Class<T> source, JavaClassSource javaClass,
                                     String factoryMethod,
                                     String factoryContainerQualifiedName) {
        MethodSource<JavaClassSource> assertThat = addAssertThat(source, javaClass, factoryMethod, factoryContainerQualifiedName);

        addAssertTruth(source, javaClass, assertThat);
    }

    private void addGeneratedMarker(final JavaClassSource javaClass) {
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

    private void addPackageSuperAndAnnotation(final Class<?> clazzUnderTest, JavaClassSource javaClass, String packageName) {
        String packageNameToUse = targetPackageName.isPresent() ? targetPackageName.get() : packageName;
        javaClass.setPackage(packageNameToUse);

        addClassExtension(clazzUnderTest, javaClass);

        //
        addGeneratedMarker(javaClass);
    }

    private <T> MethodSource<JavaClassSource> addAssertThat(Class<T> source,
                                                            JavaClassSource javaClass,
                                                            String factoryMethodName,
                                                            String factoryContainerQualifiedName) {
        String methodName = "assertThat";
        if (containsMethodCalled(javaClass, methodName)) {
            return getMethodCalled(javaClass, methodName);
        } else {
            // entry point
            MethodSource<JavaClassSource> assertThat = javaClass.addMethod()
                    .setName(methodName)
                    .setPublic()
                    .setStatic(true)
                    .setReturnType(factoryContainerQualifiedName);
            assertThat.addParameter(source, "actual");
            //         return assertAbout(things()).that(actual);
            // add explicit static reference for now - see below
            javaClass.addImport(factoryContainerQualifiedName + ".*")
                    .setStatic(true);
            String entryPointBody = "return Truth.assertAbout(" + factoryMethodName + "()).that(actual);";
            assertThat.setBody(entryPointBody);
            javaClass.addImport(Truth.class);
            assertThat.getJavaDoc().setText("Entry point for {@link " + source.getSimpleName() + "} assertions.");
            return assertThat;
        }
    }

    private <T> void addAssertTruth(Class<T> source, JavaClassSource javaClass, MethodSource<JavaClassSource> assertThat) {
        String name = "assertTruth";
        if (!containsMethodCalled(javaClass, name)) {
            // convenience entry point when being mixed with other "assertThat" assertion libraries
            MethodSource<JavaClassSource> assertTruth = javaClass.addMethod()
                    .setName(name)
                    .setPublic()
                    .setStatic(true)
                    .setReturnType(assertThat.getReturnType());
            assertTruth.addParameter(source, "actual");
            assertTruth.setBody("return " + assertThat.getName() + "(actual);");
            assertTruth.getJavaDoc().setText("Convenience entry point for {@link " + source.getSimpleName() + "} assertions when being " +
                            "mixed with other \"assertThat\" assertion libraries.")
                    .addTagValue("@see", "#assertThat");
        }
    }

    private void addClassExtension(final Class<?> clazzUnderTest, JavaClassSource javaClass) {
        Class<?> bestSubject = findBestSubjectToExtend(clazzUnderTest);
        javaClass.extendSuperType(bestSubject);
    }

    private boolean containsMethodCalled(JavaClassSource javaClass, String factoryName) {
        return javaClass.getMethods().stream().anyMatch(x -> x.getName().equals(factoryName));
    }

    private MethodSource<JavaClassSource> getMethodCalled(JavaClassSource javaClass, String methodName) {
        return javaClass.getMethods().stream().filter(x -> x.getName().equals(methodName)).findFirst().get();
    }

    private Class<?> findBestSubjectToExtend(Class<?> clazzUnderTest) {
        // does the class extend classes that we have built in Subjects for?
        var subjectForType = subjectTypeStore.getSubjectForNotNativeType(clazzUnderTest.getSimpleName(), clazzUnderTest);
        if (subjectForType.isPresent()) {
            return subjectForType.get();
        } else {
            return Subject.class;
        }
    }

}
