package io.stubbs.truth.generator.internal;

import com.google.common.base.Joiner;
import com.google.common.truth.Subject;
import com.google.common.truth.Truth;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaDocSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class AssertionEntryPointGenerator {

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

    protected <T> MethodSource<JavaClassSource> addAssertThat(Class<T> source,
                                                              JavaClassSource javaClass,
                                                              String factoryMethodName,
                                                              String factoryContainerQualifiedName) {
        String methodName = "assertThat";
        if (containsMethod(javaClass, methodName, source)) {
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

    protected <T> void addAssertTruth(Class<T> source, JavaClassSource javaClass, MethodSource<JavaClassSource> assertThat) {
        String name = "assertTruth";
        if (!containsMethod(javaClass, name, source)) {
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
}
