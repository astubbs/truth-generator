package io.stubbs.truth.generator.internal;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;
import lombok.SneakyThrows;
import org.benf.cfr.reader.api.CfrDriver;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;


public class CustomClassLoaderTest {

    @SneakyThrows
    @Test
    public void test() {
        var classfile = "java.base\\java\\time\\Duration.class";

        String filename = getInputFile();

        strobel(filename);

        cfr(filename);
    }

    private String getInputFile() {
        JDKOverrideAnalyser JDKOverrideAnalyser = new JDKOverrideAnalyser();
        Optional<InputStream> inputStream = JDKOverrideAnalyser.inputStreamForClass(Duration.class);
        File file = JDKOverrideAnalyser.dumpToTempFile(inputStream.get());
        String filename = file.toString();
        return filename;
    }

    private void cfr(String filename) {
        CfrDriver driver = new CfrDriver.Builder().build();
        driver.analyse(List.of(filename));
    }

    private void strobel(String filename) {
        PlainTextOutput output = new PlainTextOutput();
        Decompiler.decompile(filename, output);
    }

}
