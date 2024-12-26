package io.github.terahidro2003.samplers.asyncprofiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TestConstants {
    public static final Logger log = LoggerFactory.getLogger(AsyncProfilerExecutorIntegrationTest.class);
    public final static File benchmarkTargetDir = new File("src/test/resources/TestBenchmark/target");
    public final static File benchmarkProjectDir = new File("src/test/resources/TestBenchmark");
    public static final String MEASUREMENTS_PATH = "./sjsw-test-measurements";
    public static final String MAC_OS_ASPROF_AGENT = "./executables/macos/lib/libasyncprofiler.dylib";
    public static final String MAIN_BENCHMARK_CLASS = "io.github.terahidro2003.benchmark.TestBenchmark";
}
