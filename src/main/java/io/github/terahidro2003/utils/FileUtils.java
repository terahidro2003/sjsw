package io.github.terahidro2003.utils;

import io.github.terahidro2003.config.Constants;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor.log;

public class FileUtils {
    public static String readFileToString(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }

    public static File inputStreamToFile(InputStream inputStream, String fileName) throws IOException {
        File targetFile = new File(fileName);

        java.nio.file.Files.copy(
                inputStream,
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        IOUtils.closeQuietly(inputStream);

        return targetFile;
    }

    public static String retrieveAsyncProfilerExecutable(Path folder) throws IOException {
        String os = determineOS();
        switch (os) {
            case "LINUX":
                downloadAsyncProfiler(folder.resolve("async-profiler-3.0-linux-x64.tar.gz"), Constants.LINUX_ASYNC_PROFILER_URL);
                unzip(folder.toAbsolutePath().resolve("").toAbsolutePath(), folder.resolve("async-profiler-3.0-linux-x64.tar.gz").toAbsolutePath().toString());
                return folder.resolve(Constants.LINUX_ASYNC_PROFILER_NAME).resolve("lib").resolve("libasyncProfiler.so").toString();
            case "MACOS":
                throw new RuntimeException("Not implemented yet");
        }
        return "";
    }

    public static String determineOS() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("windows")) {
            throw new RuntimeException("SJSW does not support Windows.");
        } else if(os.toLowerCase().contains("linux")) {
            return "LINUX";
        } else if(os.toLowerCase().contains("mac")) {
            return "MACOS";
        }
        return "LINUX";
    }

    private static void downloadAsyncProfiler(Path tmp, String url) {
        try {
            InputStream in = new URL(url).openStream();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to download async-profiler executable");
        }
    }

    /**
     * Code of this method adapted from <a href="https://commons.apache.org/proper/commons-compress/examples.html">Apache Commons Compress User Guide</a>
     * @param target
     * @param name
     * @throws IOException
     */
    private static void unzip(Path target, String name) throws IOException {
        try (InputStream fi = Files.newInputStream(Path.of(name))) {
            try (InputStream bi = new BufferedInputStream(fi)) {
                try (InputStream gzi = new GzipCompressorInputStream(bi)) {
                    try (ArchiveInputStream i = new TarArchiveInputStream(gzi)) {
                        ArchiveEntry entry = null;
                        while ((entry = i.getNextEntry()) != null) {
                            if (!i.canReadEntryData(entry)) {
                                continue;
                            }
                            String n = String.valueOf(target.resolve(entry.getName()));
                            File f = new File(n);
                            if (entry.isDirectory()) {
                                if (!f.isDirectory() && !f.mkdirs()) {
                                    throw new IOException("failed to create directory " + f);
                                }
                            } else {
                                File parent = f.getParentFile();
                                if (!parent.isDirectory() && !parent.mkdirs()) {
                                    throw new IOException("failed to create directory " + parent);
                                }
                                try (OutputStream o = Files.newOutputStream(f.toPath())) {
                                    IOUtils.copy(i, o);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
