package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.utils.CommandStarter;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class AsyncProfilerExecutorIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AsyncProfilerExecutorIntegrationTest.class);
    final File benchmarkTargetDir = new File("src/test/resources/TestBenchmark/target");
    final File benchmarkProjectDir = new File("src/test/resources/TestBenchmark");
    private static final String MEASUREMENTS_PATH = "./sjsw-test-measurements";
    private static final String MAIN_BENCHMARK_CLASS = "io.github.terahidro2003.benchmark.TestBenchmark";

    @Test
    public void test() {
        // run mvn install on benchmark application
        CommandStarter.start("mvn", "clean", "install", "-DskipTests", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/classes",
                MAIN_BENCHMARK_CLASS,
                determineProfilerPathByOS(),
                MEASUREMENTS_PATH,
                false,
                0
        );
        Duration duration = Duration.ofSeconds(30);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("io.github.terahidro2003.benchmark.TestBenchmark.methodB",
                "io.github.terahidro2003.benchmark.TestBenchmark.methodA", "io.github.terahidro2003.benchmark.TestBenchmark.main");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void testWithJarFile() {
        // compile JAR file for TestBenchmark
        CommandStarter.start("mvn", "clean", "package", "-DskipTests" , "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/TestBenchmark-1.0-SNAPSHOT.jar",
                MAIN_BENCHMARK_CLASS,
                determineProfilerPathByOS(),
                MEASUREMENTS_PATH,
                false,
                0
        );
        Duration duration = Duration.ofSeconds(30);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("io.github.terahidro2003.benchmark.TestBenchmark.methodB",
                "io.github.terahidro2003.benchmark.TestBenchmark.methodA", "io.github.terahidro2003.benchmark.TestBenchmark.main");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void testJfrAsTheOutput() {
        // compile JAR file for TestBenchmark
        CommandStarter.start("mvn", "clean", "package", "-DskipTests", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/TestBenchmark-1.0-SNAPSHOT.jar",
                MAIN_BENCHMARK_CLASS,
                determineProfilerPathByOS(),
                MEASUREMENTS_PATH,
                true,
                0
        );
        Duration duration = Duration.ofSeconds(30);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("9 methodB((Ljava/util/List;Ljava/util/stream/DoubleStream;)V)",
                "9 methodA((Ljava/util/List;)V)", "9 main(([Ljava/lang/String;)V)");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void testWithoutSpecifiedProfilerPath() {
        // compile JAR file for TestBenchmark
        CommandStarter.start("mvn", "clean", "package", "-DskipTests", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/TestBenchmark-1.0-SNAPSHOT.jar",
                MAIN_BENCHMARK_CLASS,
                "",
                MEASUREMENTS_PATH,
                true,
                0
        );
        Duration duration = Duration.ofSeconds(30);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("9 methodB((Ljava/util/List;Ljava/util/stream/DoubleStream;)V)",
                "9 methodA((Ljava/util/List;)V)", "9 main(([Ljava/lang/String;)V)");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void miniPeasIntegrationTest() throws IOException {
        // STEP 1: create semi-empty configuration with output path and full samples (JFR) sampling enabled.
        Config config = new Config(
                null,
                null,
                null,
                benchmarkProjectDir.getAbsolutePath() + "/profiler-results",
                true,
                0
        );

        // Step 2: Set sampling maximum duration
        Duration duration = Duration.ofSeconds(60);

        // Step 3: init the pipeline
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // Step 4: generate java asprof agent as string together with JFR file location
        MeasurementInformation agent = pipeline.javaAgent(config, 1, "11111", duration);

        // Step 5: attach that agent to maven test lifecycle job
        CommandStarter.start("mvn",
                "clean",
                "test",
                "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml",
                "-DargLine=" + agent.javaAgentPath()
        );

        // Step 6: parse JFR file to JSON with all the execution samples
        SamplerResultsProcessor processor = new SamplerResultsProcessor();
//        var parsedJFR = processor.extractSamplesFromJFR(new File(agent.rawOutputPath()), config);

        // Step 7: convert samples to Peass tree
//        var root = processor.convertResultsToPeassTree(parsedJFR, "00000", "11111");

//        System.out.println(root);
        System.out.println(agent);
    }

    private String determineProfilerPathByOS() {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().contains("windows")) {
            log.error("SJSW does not support windows");
        } else if(os.toLowerCase().contains("linux")) {
            return "./executables/linux/lib/libasyncProfiler.so";
        } else if(os.toLowerCase().contains("mac")) {
            return "./executables/macos/lib/libasyncProfiler.dylib";
        }
        return "./executables/linux/lib/libasyncProfiler.so";
    }

    private Set<String> isTreeAssumedValid(StackTraceTreeNode root) {
        Set<String> treeMethodNames = new HashSet<>();
        isTreeAssumedValidRecursive(root, "", false, treeMethodNames);
        return treeMethodNames;
    }

    private void isTreeAssumedValidRecursive(StackTraceTreeNode node, String prefix, boolean isLast, Set<String> methodNames) {
        if (node.getPayload().getMethodName() != null) {
            methodNames.add(node.getPayload().getMethodName());
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            isTreeAssumedValidRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1, methodNames);
        }
    }
}