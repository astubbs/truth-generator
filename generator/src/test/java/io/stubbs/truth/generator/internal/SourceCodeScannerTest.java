package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.TestClassFactories;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import io.stubbs.truth.generator.internal.model.UserSourceCodeManagedMiddleClass;
import org.junit.Test;

import java.util.Optional;

/**
 * @see SourceCodeScanner
 */
public class SourceCodeScannerTest {

    @Test
    public void tryGetUserManagedMiddle() {
        var scanner = TestClassFactories.newScanner();
        var middle = scanner.tryGetUserManagedMiddle(ThreeSystem.class);
        Truth8.assertThat(middle).isPresent();

        var found = middle.get();

        String canonicalName = found.getCanonicalName();
        String underTestFound = found.getClassUnderTestSimpleName();
        Truth.assertThat(underTestFound).isEqualTo(ThreeSystem.class.getCanonicalName());
    }

    @Test
    public void basic() {
        var scanner = TestClassFactories.newScanner();

        //
        var target = ThreeSystem.class;
        Optional<UserSourceCodeManagedMiddleClass<Object>> pathIfExists = scanner
                .getUserSubjectWrapFor(target);

        //
        Truth8.assertThat(pathIfExists).isPresent();
        Truth.assertThat(pathIfExists.get().getClassUnderTestSimpleName())
                .isEqualTo(target.getCanonicalName());
    }
}
