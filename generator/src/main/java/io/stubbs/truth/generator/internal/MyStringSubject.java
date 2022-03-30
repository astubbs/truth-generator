package io.stubbs.truth.generator.internal;

import com.google.common.io.Resources;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.StringSubject;
import io.stubbs.truth.generator.BaseSubjectExtension;
import io.stubbs.truth.generator.SubjectFactoryMethod;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @see IgnoringWhiteSpaceComparison
 * @author Antony Stubbs
 */
@BaseSubjectExtension(String.class)
public class MyStringSubject extends StringSubject {

    String actual;

    @SubjectFactoryMethod
    public static Factory<MyStringSubject, String> strings() {
        return MyStringSubject::new;
    }

    protected MyStringSubject(FailureMetadata failureMetadata, String actual) {
        super(failureMetadata, actual);
        this.actual = actual;
    }

    public IgnoringWhiteSpaceComparison ignoringTrailingWhiteSpace() {
        return new IgnoringWhiteSpaceComparison();
    }

    private String loadFileToString(String expectedFileName) throws IOException {
        return Resources.toString(Resources.getResource(expectedFileName), Charset.defaultCharset());
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class IgnoringWhiteSpaceComparison {

        @SneakyThrows
        public void equalToFile(String expectedMyEmployeeParent) {
            String fileContent = loadFileToString(expectedMyEmployeeParent);
            equalTo(fileContent);
        }

        public void equalTo(String expected) {
            String expectedNormal = normalise(expected);
            String actualNormal = normalise(actual);

            check("").that(actualNormal).isEqualTo(expectedNormal);
        }

        private String normalise(String raw) {
            String normal = normaliseEndingsEndings(raw);
            normal = normaliseWhiteSpaceAtEndings(normal);
            return normal;
        }

        /**
         * make line endings consistent
         */
        private String normaliseEndingsEndings(String raw) {
            return raw.replaceAll("\\r\\n?", "\n");
        }

        /**
         * lazy remove trailing whitespace on lines
         */
        private String normaliseWhiteSpaceAtEndings(String raw) {
            return raw.replaceAll("(?m)\\s+$", "");
        }
    }

}
