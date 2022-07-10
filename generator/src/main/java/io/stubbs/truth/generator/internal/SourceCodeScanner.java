package io.stubbs.truth.generator.internal;

import com.google.common.truth.Subject;
import io.stubbs.truth.generator.FullContext;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Set;

/**
 * Looks for {@link io.stubbs.truth.generator.UserManagedMiddleSubject} classes which aren't compiled, inside source
 * directories - typically just the test source directory of the module running in.
 *
 * @author Antony Stubbs
 */
@Slf4j
@Value
public class SourceCodeScanner {

    UserAnnotatedSourceCache cache;

    FullContext reflectionContext;

    /**
     * Packages to look for existing {@link Subject}s in.
     */
    Set<CPPackage> sourcePackagesToScanForSubjects;

    public SourceCodeScanner(FullContext reflectionContext, Set<CPPackage> sourcePackagesToScanForSubjects) {
        this.reflectionContext = reflectionContext;
        this.sourcePackagesToScanForSubjects = sourcePackagesToScanForSubjects;
        this.cache = new UserAnnotatedSourceCache(reflectionContext, sourcePackagesToScanForSubjects);
    }

    public <T> Optional<UserSourceCodeManagedMiddleClass<T>> tryGetUserManagedMiddle(Class<T> clazzUnderTest) {
        return cache.getUserSubjectWrapFor(clazzUnderTest);
//                .stream()
//                .flatMap(path -> maybeParseFile(clazzUnderTest, path))
//                .findFirst();
    }

    @Value
    public static class CPPackage {
        String packageName;
    }
}
