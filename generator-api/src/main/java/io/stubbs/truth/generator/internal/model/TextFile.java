package io.stubbs.truth.generator.internal.model;

import com.google.common.io.Resources;
import lombok.SneakyThrows;
import lombok.Value;

import java.net.URL;
import java.nio.charset.Charset;

/**
 * @author Antony Stubbs
 */
@Value
public class TextFile {

    String resourcePath;

    public static TextFile fromResourcePath(String resourcePath) {
        return new TextFile(resourcePath);
    }

    @SneakyThrows
    public String content() {
        URL resource = Resources.getResource(getResourcePath());
        return Resources.toString(resource, Charset.defaultCharset());
    }
}
