package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.utils.CommandStarter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static io.github.terahidro2003.samplers.asyncprofiler.TestConstants.*;

public class PeassIntegrationTest {

    @Test
    void integrate() {
        final String[] commits = {"11111"};
        final int vms = 2;
        final String testcaseMethod = "testing";

        MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier();
        String outputPath = benchmarkProjectDir.getAbsolutePath() + "/profiler-results";

        Config config = Config.builder()
                .outputPathWithIdentifier(outputPath, measurementIdentifier)
                .autodownloadProfiler()
                .jfrEnabled(true)
                .withTimeoutDisabled()
                .interval(50)
                .build();

        measure(vms, commits, config, measurementIdentifier, outputPath, testcaseMethod);
    }

    @Test
    void integrateIntensive() {
        final String[] commits = {"11111"};
        final int vms = 20;
        final String testcaseMethod = "testing";

        MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier();
        String outputPath = benchmarkProjectDir.getAbsolutePath() + "/profiler-results";

        Config config = Config.builder()
                .outputPathWithIdentifier(outputPath, measurementIdentifier)
                .autodownloadProfiler()
                .jfrEnabled(true)
                .withTimeoutDisabled()
                .interval(10)
                .build();

        measure(vms, commits, config, measurementIdentifier, outputPath, testcaseMethod);
    }

    private void measure(int vms, String[] commits, Config config, MeasurementIdentifier measurementIdentifier,
                         String outputPath, String testcaseMethod) {
        SamplerResultsProcessor processor = new SamplerResultsProcessor();

        // Measure
        for (int i = 0; i < vms; i++) {
            log.info("Starting VM {}", i);
            for (int j = 0; j < commits.length; j++) {
                log.info("Testing commit {}", commits[j]);
                emulateRunOnce(config, i, commits[j]);
                log.info("Commit {} measurement completed.", commits[j]);
            }
            log.info("Finished VM {}", i);
        }


        for (int i = 0; i < commits.length; i++) {
            log.info("Starting commit {}", commits[i]);
            File resultsDir = new File(outputPath + "/measurement_" +measurementIdentifier.getUuid().toString());
            Path resultsPath = resultsDir.toPath();

            // Build BAT
            List<File> commitJfrs = processor.listJfrMeasurementFiles(resultsPath, List.of(commits[i]));
            StackTraceTreeNode tree = processor.getTreeFromJfr(commitJfrs);
            StackTraceTreeNode filteredTestcaseTree = processor.filterTestcaseSubtree(testcaseMethod, tree);

            System.out.println();
            System.out.println("FILTERED TREE:");
            filteredTestcaseTree.printTree();
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
}
