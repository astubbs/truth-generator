package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassUtils {

  private List<ClassLoader> loaders = new ArrayList<>();

  public Set<Class<?>> findNativeExtensions(String... modelPackages) {
    // TODO share Reflections instance?
    ConfigurationBuilder build = new ConfigurationBuilder()
        .forPackages(modelPackages)
        .filterInputsBy(new FilterBuilder().includePackage(modelPackages[0])) // TODO test different packages work?
        .setParallel(true)
        .setScanners(Scanners.TypesAnnotated, Scanners.SubTypes);

    Reflections reflections = new Reflections(build);

    return reflections.getTypesAnnotatedWith(BaseSubjectExtension.class);
  }

  // TODO cleanup
  public Set<Class<?>> collectSourceClasses(String... modelPackages) {
    // TODO share Reflections instance?
    // for all classes in package
    ConfigurationBuilder build = new ConfigurationBuilder()
        .forPackages(modelPackages)
        .filterInputsBy(new FilterBuilder().includePackage(modelPackages[0])) // TODO test different packages work?
        .setScanners(Scanners.SubTypes)
        .setExpandSuperTypes(true);
//            .addClassLoaders(this.loaders);

    Reflections reflections = new Reflections(build);
//    reflections.expandSuperTypes(); // get things that extend something that extend object

    // https://github.com/ronmamo/reflections/issues/126
    Set<Class<? extends Enum>> subTypesOfEnums = reflections.getSubTypesOf(Enum.class);

    Set<String> allTypes1 = reflections.getAllTypes();
    Set<Class<?>> collect = allTypes1.stream().filter(x -> {
      int i = x.lastIndexOf('.');
      String packagee = x.substring(0, i);
      String modelPackage = modelPackages[0];
      return packagee.equals(modelPackage);
    }).map(x-> {
      try {
        return Class.forName(x);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
        return null;
      }
    }).collect(Collectors.toSet());

    List<ClassLoader> classLoadersList = new LinkedList<ClassLoader>();
    classLoadersList.add(ClasspathHelper.contextClassLoader());
    classLoadersList.add(ClasspathHelper.staticClassLoader());
    Reflections reflection2s = new Reflections(new ConfigurationBuilder()
        .setScanners(Scanners.SubTypes)
        .setExpandSuperTypes(true)
        .forPackages(modelPackages)
        .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
        .filterInputsBy(new FilterBuilder().includePackage(modelPackages[0])));
    Set<Class<?>> subTypesOf = reflection2s.getSubTypesOf(Object.class);

    Set<Class<?>> allTypes = reflections.getSubTypesOf(Object.class)
        // remove Subject classes from previous runs
        .stream().filter(x -> !Subject.class.isAssignableFrom(x))
        .collect(Collectors.toSet());
    allTypes.addAll(subTypesOfEnums);

    return collect;
  }

  public static String maybeGetSimpleName(Type elementType) {
    return (elementType instanceof Class<?>) ? ((Class<?>) elementType).getSimpleName() : elementType.getTypeName();
  }

  static Type getStrippedReturnTypeFirstGenericParam(Method method) {
    Type genericReturnType = method.getGenericReturnType();
    return getStrippedReturnTypeFirstGenericParam(genericReturnType);
  }

  private static Type getStrippedReturnTypeFirstGenericParam(Type genericReturnType) {
    Class<?> keyType = Object.class; // default fall back
    if (genericReturnType instanceof ParameterizedType) {
      ParameterizedType parameterizedReturnType = (ParameterizedType) genericReturnType;
      Type[] actualTypeArguments = parameterizedReturnType.getActualTypeArguments();
      if (actualTypeArguments.length > 0) { // must have at least 1
        Type key = actualTypeArguments[0];
        return getStrippedReturnType(key);
      }
    } else if (genericReturnType instanceof Class<?>) {
      return genericReturnType; // terminal
    }
    return keyType;
  }

  private static Type getStrippedReturnType(Type key) {
    if (key instanceof ParameterizedType) {
      // strip type arguments
      // could potentially add this as a type parameter to the method instead?
      ParameterizedType parameterizedKey = (ParameterizedType) key;
      Type rawType = parameterizedKey.getRawType();
      Type recursive = getStrippedReturnTypeFirstGenericParam(rawType);
      return recursive;
    } else if (key instanceof WildcardType) {
      // strip type arguments
      // could potentially add this as a type parameter to the method instead?
      WildcardType wildcardKey = (WildcardType) key;
      Type[] upperBounds = wildcardKey.getUpperBounds();
      if (upperBounds.length > 0) {
        Type upperBound = upperBounds[0];
        Type recursive = getStrippedReturnType(upperBound);
        return recursive;
      }
    }
    // else
    return key;
  }

  public void addClassLoaders(List<ClassLoader> loaders) {
    this.loaders.addAll(loaders);
  }
}
