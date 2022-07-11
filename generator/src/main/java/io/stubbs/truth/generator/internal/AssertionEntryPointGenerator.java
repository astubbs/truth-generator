package io.stubbs.truth.generator.internal;

import com.google.common.base.Joiner;
import com.google.common.truth.SimpleSubjectBuilder;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import io.stubbs.truth.generator.internal.model.MiddleClass;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

import java.util.List;
import java.util.Optional;

/**
 * @author Antony Stubbs
 */
public class AssertionEntryPointGenerator {

    public static final String ASSERT_WITH_MESSAGE = "assertWithMessage";

    /**
     * todo docs, rename
     */
    protected <T> MethodSource<JavaClassSource> addFactoryAccessor(Class<T> source, JavaClassSource javaClass, String sourceName) {
        String factoryName = Utils.createFactoryName(source);
        if (containsMethod(javaClass, factoryName, source)) {
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
            var annotation = factory.addAnnotation(SubjectFactoryMethod.class);
            return factory;
        }
    }

    private boolean containsMethod(JavaClassSource javaClass, String factoryName, Class<?> paramType) {
        return javaClass.getMethods().stream()
                .anyMatch(x ->
                        x.getName().equals(factoryName) &&
                                x.getParameters().size() == 1 &&
                                x.getParameters().get(0).getType().isType(paramType));
    }

    private MethodSource<JavaClassSource> getMethodCalled(JavaClassSource javaClass, String methodName) {
        return javaClass.getMethods().stream().filter(x -> x.getName().equals(methodName)).findFirst().get();
    }

    private String getTypeWithGenerics(Class<?> factoryClass, String... classes) {
        String genericsList = Joiner.on(", ").skipNulls().join(classes);
        String generics = new StringBuilder("<>").insert(1, genericsList).toString();
        return factoryClass.getSimpleName() + generics;
    }

    protected <T> MethodSource<JavaClassSource> addAssertThat(Class<T> classUnderTest,
                                                              JavaClassSource javaSourceClassToAddTo,
                                                              String factoryMethodName,
                                                              String factoryContainerQualifiedName) {
        String methodName = "assertThat";
        if (containsMethod(javaSourceClassToAddTo, methodName, classUnderTest)) {
            return getMethodCalled(javaSourceClassToAddTo, methodName);
        } else {
            // entry point
            MethodSource<JavaClassSource> assertThat = javaSourceClassToAddTo.addMethod()
                    .setName(methodName)
                    .setPublic()
                    .setStatic(true)
                    .setReturnType(factoryContainerQualifiedName);
            assertThat.addParameter(classUnderTest, "actual");

            //
            javaSourceClassToAddTo.addImport(factoryContainerQualifiedName + ".*")
                    .setStatic(true);

            //
            String entryPointBody = "return Truth.assertAbout(" + factoryMethodName + "()).that(actual);";
            assertThat.setBody(entryPointBody);
            javaSourceClassToAddTo.addImport(Truth.class);
            assertThat.getJavaDoc().setText("Entry point for {@link " + classUnderTest.getSimpleName() + "} assertions.");
            return assertThat;
        }
    }

    protected <T> void addAssertTruth(Class<T> classUnderTest, JavaClassSource javaClass, MethodSource<JavaClassSource> assertThat) {
        String name = "assertTruth";
        if (!containsMethod(javaClass, name, classUnderTest)) {
            // convenience entry point when being mixed with other "assertThat" assertion libraries
            MethodSource<JavaClassSource> assertTruth = javaClass.addMethod()
                    .setName(name)
                    .setPublic()
                    .setStatic(true)
                    .setReturnType(assertThat.getReturnType());
            assertTruth.addParameter(classUnderTest, "actual");
            assertTruth.setBody("return " + assertThat.getName() + "(actual);");
            assertTruth.getJavaDoc().setText("Convenience entry point for {@link " + classUnderTest.getSimpleName() + "} assertions when being " +
                            "mixed with other \"assertThat\" assertion libraries.")
                    .addTagValue("@see", "#assertThat");
        }
    }

    public void addWithMessage(String packageName, JavaClassSource overallAccess) {
        addWithMessage(packageName, Optional.empty(), overallAccess);
    }

    public void addWithMessage(String overallPointPackageName, Optional<MiddleClass> middleClass, JavaClassSource overallAccess) {
        addWithMessage(overallPointPackageName, middleClass, overallAccess, false);
        addWithMessage(overallPointPackageName, middleClass, overallAccess, true);
    }

    private void addWithMessage(String overallPointPackageName, Optional<MiddleClass> middle, JavaClassSource managedTruthFileSource, boolean withArgs) {
        if (middle.isPresent()) {
            String canonicalName = middle.get().getCanonicalName();
            // todo delete
            boolean idCard = canonicalName.contains("IdCard");
            boolean idCardtw = canonicalName.contains("IdCard");
        }


        MethodSource<JavaClassSource> with = managedTruthFileSource.addMethod()
                .setName(ASSERT_WITH_MESSAGE)
                .setStatic(true)
                .setPublic();

        managedTruthFileSource.addImport(overallPointPackageName + ".ManagedSubjectBuilder");
        managedTruthFileSource.addImport(overallPointPackageName + ".ManagedTruth");

        //
        if (withArgs) {
            with.addParameter(String.class, "format");
            // todo Roaster doesn't support var args? (add javdoc to generated method to explain issue)
            //            var o = new Object[]{};
            //            Class<? extends Object[]> aClass = o.getClass();
            //            with.addParameter("Object...", "args");
            with.addParameter(List.class, "args");
        } else {
            with.addParameter(String.class, "messageToPrepend");
        }


        //
        boolean isSubjectChild = middle.isPresent();
        if (isSubjectChild) {
            MiddleClass threeSystem = middle.get();
            String factoryName = threeSystem.getFactoryMethodName();

            //
            if (withArgs) {
                with.setBody("return Truth.assertWithMessage(format, args.toArray()).about(" + factoryName + "());");
            } else {
                with.setBody("return Truth.assertWithMessage(messageToPrepend).about(" + factoryName + "());");
            }

            //
            var subject = threeSystem.getSimpleName();
            var classUnderTest = threeSystem.getClassUnderTestSimpleName();
            with.setReturnType("SimpleSubjectBuilder<" + subject + ", " + classUnderTest + ">");
            managedTruthFileSource.addImport(SimpleSubjectBuilder.class);
        } else {

            //
            if (withArgs) {
                with.setBody("return new ManagedSubjectBuilder(Truth.assertWithMessage(format, args.toArray()));");
            } else {
                with.setBody("return new ManagedSubjectBuilder(Truth.assertWithMessage(messageToPrepend));");
            }
            with.setReturnType("ManagedSubjectBuilder");
        }

        //
        var javaDoc = with.getJavaDoc();
        javaDoc.addTagValue("see", "{@link Truth#assertWithMessage}");
    }

}
