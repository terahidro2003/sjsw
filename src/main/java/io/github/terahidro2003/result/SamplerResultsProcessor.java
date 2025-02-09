package io.github.terahidro2003.result;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.tree.StackTraceTreeBuilder;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerHelper;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import io.github.terahidro2003.utils.FileUtils;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters;
import org.openjdk.jmc.flightrecorder.stacktrace.FrameSeparator;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

import static io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor.log;

public class SamplerResultsProcessor {

    public StacktraceTreeModel jfrToStacktraceGraph(List<File> jfrs) {
        try {
            IItemCollection items = JfrLoaderToolkit.loadEvents(jfrs);
            IItemCollection filteredItems = items.apply(JdkFilters.EXECUTION_SAMPLE);
            FrameSeparator frameSeparator = new FrameSeparator(FrameSeparator.FrameCategorization.METHOD, false);
            StacktraceTreeModel model = new StacktraceTreeModel(filteredItems, frameSeparator);
            return model;
        } catch (IOException | CouldNotLoadRecordingException e) {
            log.error("Failed to load JFR", e);
            throw new RuntimeException(e);
        }
    }

    public StackTraceTreeNode getTreeFromJfr(List<File> jfrs) {
        StacktraceTreeModel model = jfrToStacktraceGraph(jfrs);
        StackTraceTreeNode tree = StackTraceTreeBuilder.buildFromStacktraceTreeModel(model);

        return tree;
    }

    private void addLocalMeasurements(StackTraceTreeNode bat, StackTraceTreeNode localTree, String identifier) {
        if (bat == null || localTree == null) {
            throw new IllegalArgumentException("Both stacktrace trees must be non-null");
        }

        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(localTree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            var result = StackTraceTreeBuilder.search(currentNode, bat);
            if (result != null) {
                result.addMeasurement(identifier, currentNode.getInitialWeight());
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    public List<File> listJfrMeasurementFiles(Path directory, List<String> containables) {
        return List.of(Objects.requireNonNull(directory.toFile().listFiles((dir, name) -> isMeasurementJfr(name, containables))));
    }

    public List<File> extractSamplesFromMultipleJFRs(Path directory, Config config, String commit) throws IOException {
        List<File> samples = new ArrayList<>();
        List<File> jfrs = listJfrMeasurementFiles(directory, List.of(commit));

        for (File file : jfrs) {
            log.info("Detected JFR measurement file: {}", file);
        }

        for (File file : jfrs) {
            if (file.getName().endsWith(".jfr")) {
                samples.add(extractSamplesFromJFR(file, "extracted_jfr_samples_" + file.getName() + ".json", config));
            }
        }
        return samples;
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
            log.info("Reading serialized sample json file {}", jfrFile.getName());
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

    public StackTraceTreeNode filterTestcaseSubtree(String testcase, StackTraceTreeNode bat) {
        return StackTraceTreeBuilder.search(testcase, bat);
    }

    public File extractSamplesFromJFR(File file, String jsonFileName, Config config) throws IOException {
        log.info("Extracting samples from jfr file {}", file.getName());

        // retrieve samples from JFR file
        List<String> command = new ArrayList<>();

        command.add("./jfr");
        command.add("print");
        command.add("--json");
        command.add("--categories");
        command.add("JVM");
        command.add("--events");
        command.add("Profiling");
        command.add(file.getAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File("/Users/juozas/Library/Java/JavaVirtualMachines/openjdk-22.0.1/Contents/Home/bin"));
        Process process = processBuilder.start();
        InputStream processInputStream = process.getInputStream();
        String jfr_json = AsyncProfilerHelper.getInstance(config).rawProfilerOutput(jsonFileName).getAbsolutePath();
        FileUtils.inputStreamToFile(processInputStream, jfr_json);
        log.info("Extracted samples from jfr file {}", jfr_json);
        return new File(jfr_json);
    }

    public void processIterationMeasurementFiles(File resultPath, int vm, String commit) {
        if (!resultPath.exists()) {
            throw new IllegalArgumentException("Result path " + resultPath + " does not exist");
        }

        List<File> unprocessedJfrs = listJfrMeasurementFiles(resultPath.toPath(), List.of("unprocessed"));
        if(unprocessedJfrs.isEmpty()) {
           log.error("No unprocessed iterative measurements identified.");
           return;
        }

        for (int i = 0; i < unprocessedJfrs.size(); i++) {
            String newName = "checked_sjsw_partial_vm_" + vm + "_iteration_" + i + "_commit_" + commit + ".jfr";
            if(unprocessedJfrs.get(i) == null) {
                continue;
            }
            try {
                FileUtils.renameFile(unprocessedJfrs.get(i), newName);
            } catch (IOException e) {
                log.error("Something went wrong while renaming partial result JFR file");
            }
        }
    }
}
