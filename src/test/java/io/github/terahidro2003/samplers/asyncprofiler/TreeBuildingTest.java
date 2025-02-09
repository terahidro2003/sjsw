package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.tree.StackTraceTreeBuilder;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.utils.CommandStarter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.*;

import static io.github.terahidro2003.samplers.asyncprofiler.TestConstants.benchmarkProjectDir;

public class TreeBuildingTest {

    final File resourcesDir = new File("src/test/resources");

    /**
     * 1Hz -> 16MB
     *
     */

    @Test
    public void fullTest() {
        String outputPath = benchmarkProjectDir.getAbsolutePath() + "/profiler-results";
        MeasurementIdentifier identifier = new MeasurementIdentifier();
        AsyncProfilerExecutor executor = new AsyncProfilerExecutor();
        Config config = Config.builder()
                .autodownloadProfiler()
                .outputPathWithIdentifier(outputPath, identifier)
                .interval(100)
                .jfrEnabled(true)
                .build();
        MeasurementInformation info = executor.javaAgent(config, 3, "11111", Duration.ofSeconds(300));

        for (int i = 0; i<3; i++) {
            emulateRunOnce(config, i, "11111");
        }
    }

    private void emulateMavenExecutor(String javaAgent, String projectRootDir) {
        CommandStarter.start("mvn",
                "clean",
                "test",
                "-f", projectRootDir + "/pom.xml",
                "-DargLine=" + javaAgent
        );
    }

    private void emulateRunOnce(Config config, int vm, String commit) {
        Duration duration = Duration.ofSeconds(30);
        SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();
        MeasurementInformation agent = pipeline.javaAgent(config, vm, commit, duration);
        emulateMavenExecutor(agent.javaAgentPath(), benchmarkProjectDir.getAbsolutePath());
    }

    @Test
    public void test() {
        String testcase = "testMe()";
        List<File> jfrs = List.of(
                new File(resourcesDir + "/1111_1.jfr"),
                new File(resourcesDir + "/1111_2.jfr")
        );

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();

        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 2, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testMoreVms() {
        String testcase = "testMe()";
        List<File> jfrs = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            jfrs.add(new File(resourcesDir + "/1111_" + i + ".jfr"));
        }

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 5, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testIterativeTree() {
        String testcase = "testMe()";
        File folder = new File(resourcesDir + "/iterativeSamples");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "55bbfafd67ee1f7dc721ea945714a324708787c6", 2, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void test20Vms() {
        String testcase = "testMe()";
        List<File> jfrs = new ArrayList<>();
        for (int i = 1; i < 21; i++) {
            jfrs.add(new File(resourcesDir + "/1111 (" + i + ").jfr"));
        }

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 20, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testFilenamePatterRecognition() {
        int vm = StackTraceTreeBuilder.extractVmNumber("checked_sjsw_partial_vm_1_iteration_2_commit_55bbfafd67ee1f7dc721ea945714a324708787c6.jfr");
        Assertions.assertEquals(vm, 1);
    }
}
