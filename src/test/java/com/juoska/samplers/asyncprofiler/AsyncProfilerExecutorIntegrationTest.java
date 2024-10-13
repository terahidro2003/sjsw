package com.juoska.samplers.asyncprofiler;

import com.juoska.config.Config;
import com.juoska.result.StackTraceTreeNode;
import com.juoska.samplers.SamplerExecutorPipeline;
import groovy.util.logging.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class AsyncProfilerExecutorIntegrationTest {
    @Test
    public void test() {
        // config
        Config config = new Config(
                "./target/classes",
                "com.juoska.benchmark.TestBenchmark",
                "./executables/linux/lib/libasyncProfiler.so",
                "./output.sampler-test.json",
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