package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.utils.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FileUtilsTest {

    final File resourcesDir = new File("src/test/resources");

    @Test
    public void test() throws IOException {
        File tmp = new File(resourcesDir.getAbsolutePath() + "/temp");
        if(!tmp.exists()) {
            boolean createTempFolderResult = new File(resourcesDir.getAbsolutePath() + "/temp").mkdir();
            if(!createTempFolderResult) throw new RuntimeException("Failed to create temp folder");
        }

        String result = FileUtils.retrieveAsyncProfilerExecutable(resourcesDir.toPath().resolve("temp"));
        File resultExecutableLinux = new File(result);

        Assertions.assertTrue(resultExecutableLinux.exists());
        Assertions.assertTrue(resultExecutableLinux.canExecute());
    }
}
