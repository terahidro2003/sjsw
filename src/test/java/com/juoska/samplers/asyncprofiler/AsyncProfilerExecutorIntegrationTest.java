package com.juoska.samplers.asyncprofiler;

import com.juoska.config.Config;
import com.juoska.result.StackTraceTreeNode;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.utils.CommandStarter;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    @Test
    public void test() {
        // run mvn install on benchmark application
        CommandStarter.start("mvn", "clean", "install", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/classes",
                "com.juoska.benchmark.TestBenchmark",
                determineProfilerPathByOS(),
                "./output.sampler-test" + System.currentTimeMillis()+".json",
                "./asprof.sjsw.output.test.raw.json"
        );
        Duration duration = Duration.ofSeconds(10);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("com.juoska.benchmark.TestBenchmark.methodB",
                "com.juoska.benchmark.TestBenchmark.methodA", "com.juoska.benchmark.TestBenchmark.main");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void testWithJarFile() {
        // run mvn install on benchmark application
        CommandStarter.start("mvn", "clean", "install", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // compile JAR file for TestBenchmark
        CommandStarter.start("mvn", "clean", "package", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/TestBenchmark-1.0-SNAPSHOT.jar",
                "com.juoska.benchmark.TestBenchmark",
                determineProfilerPathByOS(),
                "./output.sampler-test-jar" + System.currentTimeMillis()+".json",
                "./asprof.sjsw.output.test.raw.json"
        );
        Duration duration = Duration.ofSeconds(10);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("com.juoska.benchmark.TestBenchmark.methodB",
                "com.juoska.benchmark.TestBenchmark.methodA", "com.juoska.benchmark.TestBenchmark.main");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
    }

    @Test
    public void testJfrAsTheOutput() {
        // run mvn install on benchmark application
        CommandStarter.start("mvn", "clean", "install", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // compile JAR file for TestBenchmark
        CommandStarter.start("mvn", "clean", "package", "-f", benchmarkProjectDir.getAbsolutePath() + "/pom.xml");

        // config
        Config config = new Config(
                benchmarkTargetDir.getAbsolutePath() + "/TestBenchmark-1.0-SNAPSHOT.jar",
                "com.juoska.benchmark.TestBenchmark",
                determineProfilerPathByOS(),
                "./output.sampler-test-jar" + System.currentTimeMillis()+".json",
                "./asprof.sjsw.output.test.raw" + System.currentTimeMillis() + ".jfr"
        );
        Duration duration = Duration.ofSeconds(60);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();

        // run (and assert whether both phases throw an exception)
        Assertions.assertDoesNotThrow(() -> pipeline.execute(config, duration));
        Assertions.assertDoesNotThrow(() -> pipeline.write("destination.json"));

        // assert that tree at least includes benchmark method names
        Set<String> methodNames = Set.of("com.juoska.benchmark.TestBenchmark.methodB",
                "com.juoska.benchmark.TestBenchmark.methodA", "com.juoska.benchmark.TestBenchmark.main");
        assertThat(isTreeAssumedValid(pipeline.getStackTraceTree())).containsAnyOf(methodNames.toArray(new String[0]));
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
        if (node.getMethodName() != null) {
            methodNames.add(node.getMethodName());
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            isTreeAssumedValidRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1, methodNames);
        }
    }
}