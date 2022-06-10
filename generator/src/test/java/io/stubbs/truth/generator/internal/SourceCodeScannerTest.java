package io.stubbs.truth.generator.internal;

import com.google.common.truth.Truth;
import com.google.common.truth.Truth8;
import io.stubbs.truth.generator.internal.model.ThreeSystem;
import org.junit.Test;

import java.util.Set;

import static io.stubbs.truth.generator.TestClassFactories.TEST_SRC_ROOT;

public class SourceCodeScannerTest {

    @Test
    public void tryGetUserManagedMiddle() {
        var scanner = new SourceCodeScanner(Set.of(new SourceCodeScanner.CPPackage("io.stubbs")), TEST_SRC_ROOT);
        var middle = scanner.tryGetUserManagedMiddle(ThreeSystem.class);
        Truth8.assertThat(middle).isPresent();
        var found = middle.get();
        String canonicalName = found.getCanonicalName();
        String underTestFound = found.getClassUnderTest().getCanonicalName();
        Truth.assertThat(underTestFound).isEqualTo(ThreeSystem.class.getCanonicalName());
    }
}
