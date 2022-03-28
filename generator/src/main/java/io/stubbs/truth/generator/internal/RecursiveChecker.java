package io.stubbs.truth.generator.internal;

import com.google.common.flogger.FluentLogger;
import io.stubbs.truth.generator.SourceClassSets;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.startsWithAny;

public class RecursiveChecker {

  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final HashSet<Class<?>> seen = new HashSet<>();

  private final String[] allowablePrefixes = new String[]{"is", "get", "to"};

  public Set<Class<?>> addReferencedIncluded(final SourceClassSets ss) {
    Set<Class<?>> referencedNotIncluded = findReferencedNotIncluded(ss);
    return ss.addIfMissing(referencedNotIncluded);
  }

  public Set<Class<?>> findReferencedNotIncluded(SourceClassSets ss) {
    Set<Class<?>> explicitlyConfigured = ss.getAllClasses();

    final Set<Class<?>> visited = new HashSet<>();

    // for all the classes
    explicitlyConfigured.forEach(x ->
                    // for all the methods
                    recursive(x, ss, visited)
            );

    return seen;
  }

  private void recursive(Class<?> theClass, SourceClassSets ss, Set<Class<?>> visited) {
    // track visited
    if (visited.contains(theClass))
      return; // Stream.of(theClass); // already done - return
    else
      visited.add(theClass);

    // gather all the types, and add myself to the list (in case none of my methods return me)
    // not sure if there's a way to do this while remaining in a stream (you can't add elements to a stream, except from it's source)
    Method[] methods = theClass.getMethods();
    List<Class<?>> methodReturnTypes = Arrays.stream(methods)
            // only no param methods (getters, issers)
            .filter(y -> y.getParameterCount() == 0)
            // don't filter method names on legacy classes
            .filter(m -> ss.isLegacyClass(theClass) || startsWithAny(m.getName(), allowablePrefixes))
            // for all the return types
            .map((Method method) -> ClassUtils.primitiveToWrapper(method.getReturnType()))
            .distinct()
            .collect(Collectors.toList());

    //
    methodReturnTypes.add(theClass);

    // todo docs
    // transform / filter
    List<Class<?>> filtered = methodReturnTypes.stream()
            .map(y -> {
              if (y.isArray()) {
                return y.getComponentType();
              } else {
                return y;
              }
            })
            .filter(cls -> !seen.contains(cls))
            .filter(cls -> !Void.TYPE.isAssignableFrom(cls) && !cls.isPrimitive() && !cls.equals(Object.class))
            // todo smelly access ChainStrategy.getNativeTypes()
            .filter(type -> !ChainStrategy.getNativeTypes().contains(type) && !ss.isClassIncluded(type))
            .collect(Collectors.toList());

    if (!filtered.isEmpty()) {
      log.atInfo().log("Adding return types from %s : %s", theClass,
              filtered.stream().map(Class::getSimpleName).collect(Collectors.toList()));
    }

    seen.addAll(filtered);

    filtered.forEach(type ->
                    recursive(type, ss, visited)
            );
  }

}
