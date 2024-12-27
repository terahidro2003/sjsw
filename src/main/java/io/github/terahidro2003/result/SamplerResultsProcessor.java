package io.github.terahidro2003.result;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import groovy.util.logging.Slf4j;
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerHelper;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import io.github.terahidro2003.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor.log;

@Slf4j
public class SamplerResultsProcessor {

    public List<File> listJfrMeasurementFiles(Path directory, List<String> containables) {
        return List.of(Objects.requireNonNull(directory.toFile().listFiles((dir, name) -> isMeasurementJfr(name, containables))));
    }

    public List<File> extractSamplesFromMultipleJFRs(Path directory, Config config, String commit) throws IOException {
        List<File> samples = new ArrayList<>();
        List<File> jfrs = listJfrMeasurementFiles(directory, List.of(commit));

        for (File file : jfrs) {
            if (file.getName().endsWith(".jfr")) {
                samples.add(extractSamplesFromJFR(file, "extracted_jfr_samples_" + file.getName() + ".json", config));
            }
        }
        return jfrs;
    }

    private boolean isMeasurementJfr(String filename, List<String> containables) {
        if (!filename.endsWith(".jfr")) {
            return false;
        }

        for (String containable : containables) {
            if (!filename.contains(containable)) {
                return false;
            }
        }

        return true;
    }

    /**
     *
     * @param jfrFile - json file containing samples derived from JFR file
     * @return
     */
    public List<ExecutionSample> readJfrFile(File jfrFile) {
        try {
            log.info("Reading serialized sample json file {}", jfrFile);
            List<ExecutionSample> samples = ExecutionSample.parseJson(jfrFile.getAbsolutePath());
            log.info("Loaded {} samples", samples.size());
            return samples;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ExecutionSample> getExecutionSamplesFromMultipleJsons(List<File> serializedSamples) {
        List<ExecutionSample> samples = new ArrayList<>();
        log.info("Reading serialized sample json files");
        for (File file : serializedSamples) {
            samples.addAll(readJfrFile(file));
        }
        log.info("Loaded {} samples", samples.size());
        return samples;
    }

    public File extractSamplesFromJFR(File file, String jsonFileName, Config config) throws IOException {
        // retrieve samples from JFR file
        List<String> command = new ArrayList<>();
        command.add("jfr");
        command.add("print");
        command.add("--json");
        command.add("--categories");
        command.add("JVM");
        command.add("--events");
        command.add("Profiling");
        command.add(file.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        InputStream processInputStream = process.getInputStream();
        String jfr_json = AsyncProfilerHelper.getInstance(config).rawProfilerOutput(jsonFileName).getAbsolutePath();
        FileUtils.inputStreamToFile(processInputStream, jfr_json);
        return new File(jfr_json);
    }

    public CallTreeNode convertResultsToPeassTree(File parsedJFRSamples, String commit, String oldCommit) {
        List<ExecutionSample> jfrSamples = readJfrFile(parsedJFRSamples);
        StackTraceTreeBuilder stackTraceTreeBuilder = new StackTraceTreeBuilder();
        var tree = stackTraceTreeBuilder.buildFromExecutionSamples(jfrSamples);
        StackTraceTreeNode root = tree;
        tree.printTree();
        CallTreeNode callTreeNode = null;
        toPeasDS(root, callTreeNode, commit, oldCommit);
        return callTreeNode;
    }

    private void toPeasDS(StackTraceTreeNode node, CallTreeNode peasNode, String commit, String oldCommit) {
        MeasurementConfig measurementConfig = new MeasurementConfig(1, "00000", "00000");

        if(peasNode == null) {
            String methodNameWithNew = node.getMethodName() + "()";
            if(node.getMethodName().contains("<init>")) {
                methodNameWithNew = "new " + node.getMethodName() + "()";
            }
            peasNode = new CallTreeNode(node.getMethodName(),
                    methodNameWithNew,
                    methodNameWithNew,
                    measurementConfig);

            createPeassNode(node, peasNode, commit, oldCommit);
        } else {
            createPeassNode(node, peasNode, commit, oldCommit);
            peasNode = peasNode.getChildByKiekerPattern(node.getMethodName() + "()");
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (StackTraceTreeNode child : children) {
            toPeasDS(child, peasNode, commit, oldCommit);
        }
    }

    private void createPeassNode(StackTraceTreeNode node, CallTreeNode peasNode, String commit, String oldCommit) {
        peasNode.initCommitData();
        peasNode.initVMData(commit);
        peasNode.addMeasurement(commit, node.getTimeTaken());

        // check is done as a workaround for Peass kieker pattern check
        if(node.getMethodName().contains("<init>")) {
            String methodNameWithNew = "new " + node.getMethodName() + "()";
            peasNode.appendChild(node.getMethodName(),
                    methodNameWithNew,
                    methodNameWithNew
            );
        } else {
            peasNode.appendChild(node.getMethodName(),
                    node.getMethodName() + "()",
                    node.getMethodName() + "()"
            );
        }

        peasNode.createStatistics(commit);
    }
}
