package io.github.terahidro2003.samplers.asyncprofiler;

import groovy.util.logging.Slf4j;
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

@Slf4j
public class PeassIntegrationTest {

    @Test
    void integrate() {
        final String[] commits = {"11111"};
        final int vms = 5;
        final String testcaseMethod = "testing";

        MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier();
        String outputPath = benchmarkProjectDir.getAbsolutePath() + "/profiler-results";

        Config config = Config.builder()
                .outputPathWithIdentifier(outputPath, measurementIdentifier)
                .autodownloadProfiler()
                .jfrEnabled(true)
                .frequency(15)
                .build();

        SamplerResultsProcessor processor = new SamplerResultsProcessor();

        // Measure
        for (int i = 0; i < vms; i++) {
            log.info("Starting VM {}", i);
            for (int j = 0; j < commits.length; j++) {
                log.info("Testing commit {}", commits[j]);
                emulateRunOnce(config, i, commits[j]);
            }
        }


        for (int i = 0; i < commits.length; i++) {
            log.info("Starting commit {}", commits[i]);
            File resultsDir = new File(outputPath + "/measurement_" +measurementIdentifier.getUuid().toString());
            Path resultsPath = resultsDir.toPath();

            // Build BAT
            List<File> commitJfrs = processor.listJfrMeasurementFiles(resultsPath, List.of(commits[i]));
            StackTraceTreeNode tree = processor.getTreeFromJfr(commitJfrs, commits[i]);
            StackTraceTreeNode filteredTestcaseTree = processor.filterTestcaseSubtree(testcaseMethod, tree);
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
