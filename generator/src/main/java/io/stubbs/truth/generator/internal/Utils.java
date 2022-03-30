package io.stubbs.truth.generator.internal;

import io.stubbs.truth.generator.UserManagedTruth;
import org.atteo.evo.inflector.English;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * @author Antony Stubbs
 */
public class Utils {

    public static final String DIR_TRUTH_ASSERTIONS_MANAGED = "truth-assertions-managed";
    public static final String DIR_TRUTH_ASSERTIONS_TEMPLATES = "truth-assertions-templates";

    // todo remove static
    private static java.nio.file.Path testOutputDir;

    public static String writeToDisk(JavaClassSource javaClass) {
        return writeToDisk(javaClass, Optional.empty());
    }

    public static String writeToDisk(JavaClassSource javaClass, Optional<String> targetPackageName) {
        String classSource = javaClass.toString();
        Path fileName = getFileName(javaClass, targetPackageName);
        try (PrintWriter out = new PrintWriter(fileName.toFile())) {
            out.println(classSource);
        } catch (FileNotFoundException e) {
            throw new IllegalStateException(format("Cannot write to file %s", fileName));
        }
        return classSource;
    }

    private static Path getFileName(JavaClassSource javaClass, Optional<String> targetPackageName) {
        Path directoryName = getDirectoryName(javaClass, targetPackageName);
        File dir = new File(directoryName.toUri());
        if (!dir.exists()) {
            boolean mkdir = dir.mkdirs();
        }
        return directoryName.resolve(javaClass.getName() + ".java");
    }

    private static Path getDirectoryName(JavaClassSource javaClass, Optional<String> targetPackageName) {
        String packageName = targetPackageName.isEmpty() ? javaClass.getPackage() : targetPackageName.get();

        // don't use, as won't be able to access package level access methods if we live in a different package
        // user can still use a custom target package if they like
        String packageNameSuffix = ".truth";

        // todo use annotations not name strings
        List<String> ids = List.of("Parent", "Child");
        boolean isGeneratorManaged = javaClass.getAnnotation(UserManagedTruth.class) == null;

        Path base = (isGeneratorManaged) ? getManagedPath() : getTemplatesPath();

        String packageNameDir = packageName.replace('.', File.separatorChar);

        return base.resolve(packageNameDir);
    }

    public static Path getManagedPath() {
        return testOutputDir.resolve(Utils.DIR_TRUTH_ASSERTIONS_MANAGED);
    }

    public static Path getTemplatesPath() {
        return testOutputDir.resolve(Utils.DIR_TRUTH_ASSERTIONS_TEMPLATES);
    }

    public static <T> String getFactoryName(Class<T> source) {
        String simpleName = source.getSimpleName();
        String plural = English.plural(simpleName);
        String normal = toLowerCaseFirstLetter(plural);
        return normal;
    }

    private static String toLowerCaseFirstLetter(String plural) {
        return plural.substring(0, 1).toLowerCase() + plural.substring(1);
    }

    public static void requireNotEmpty(final List<Class<?>> classes) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("No classes to generate from");
        }
    }

    public static void requireNotEmpty(final String[] modelPackages) {
        if (modelPackages.length == 0) {
            throw new IllegalArgumentException("No packages to generate from");
        }
    }

    public static void requireNotEmpty(final Set<Class<?>> classes) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("No classes to generate from");
        }
    }

    public static void setOutputBase(java.nio.file.Path testOutputDir) {
        Utils.testOutputDir = testOutputDir;
    }

    public static String msg(String s, Object... args) {
        return MessageFormatter.arrayFormat(s, args).getMessage();
    }

}
