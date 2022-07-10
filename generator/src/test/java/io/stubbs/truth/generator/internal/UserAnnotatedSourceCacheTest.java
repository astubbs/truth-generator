package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import org.junit.Test;

import java.util.Optional;

public class UserAnnotatedSourceCacheTest {

    @Test
    public void basic() {
        UserAnnotatedSourceCache userAnnotatedSourceCache = new UserAnnotatedSourceCache(
                TestClassFactories.newFullContext(),
                TestClassFactories.newScanner().getSourcePackagesToScanForSubjects());

        Optional<UserSourceCodeManagedMiddleClass<Object>> pathIfExists = userAnnotatedSourceCache
                .getUserSubjectWrapFor(ThreeSystem.class);

        //
        Truth8.assertThat(pathIfExists).isPresent();
    }
}