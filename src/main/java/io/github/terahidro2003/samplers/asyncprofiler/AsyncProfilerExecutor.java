package io.github.terahidro2003.samplers.asyncprofiler;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.StackTraceData;
import io.github.terahidro2003.result.StackTraceTreeBuilder;
import io.github.terahidro2003.result.StackTraceTreeNode;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import io.github.terahidro2003.utils.CommandStarter;
import io.github.terahidro2003.utils.FileUtils;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AsyncProfilerExecutor implements SamplerExecutorPipeline {

    public static final Logger log = LoggerFactory.getLogger(AsyncProfilerExecutor.class);
    private static List<StackTraceData> result;

    private volatile boolean benchmarkException = false;

    private StackTraceTreeNode root;

    private Duration chooseDuration(Duration duration) {
        if(duration == null || duration.getSeconds() <= 0) {
            log.warn("No duration was specified. Setting default sampling duration of 10 seconds.");
            duration = Duration.ofSeconds(10);
        } else {
            log.info("Setting sampling duration of {} seconds.", duration);
        }
        return duration;
    }

    private void retrieveAsyncProfiler(Config config) throws IOException {
        if (config.profilerPath().isEmpty()) {
            File folder = new File(config.outputPath() + "/executables");
            if(!folder.exists()) {
                folder.mkdirs();
            }
            String profilerPath = FileUtils.retrieveAsyncProfilerExecutable(Path.of(config.outputPath()).resolve("/executables"));
            config = new Config(config.classPath(), config.mainClass(), profilerPath, config.outputPath(), config.fullSamples(), config.frequency());
        }
    }

    @Override
    public void execute(long pid, Config config, Duration duration) throws InterruptedException, IOException {
        configureResultsFolder(config);
        retrieveAsyncProfiler(config);
        // TODO: allow custom frequency in hz specified in config
        String[] command = {config.profilerPath(), "-d", String.valueOf(duration.getSeconds()), String.valueOf(pid)};

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        Process process = processBuilder.start();

        InputStream processInputStream = process.getInputStream();

        var samples = parseProfile(processInputStream);
        print(samples);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Failed to execute async profiler. Async profiler returned exit code 0");
        }
    }

    @Override
    public void execute(Config config, Duration duration) throws InterruptedException, IOException {
        execute(config, duration, "00000", "11111");
    }

    private void configureResultsFolder(Config config) {
        if (config.outputPath().isEmpty()) {
            throw new IllegalArgumentException("No output path specified");
        }

        File outputFolder = new File(config.outputPath());
        if (!outputFolder.exists()) {
            if (!outputFolder.mkdirs()) {
                throw new IllegalStateException("Failed to create output folder: " + outputFolder.getAbsolutePath());
            }
        }
    }

    @Override
    public void execute(Config config, Duration duration, String commit, String oldCommit) throws InterruptedException, IOException {
        configureResultsFolder(config);
        retrieveAsyncProfiler(config);
        log.info("Executing async-profiler sampler with the following configuration: classPath: {}, mainClass: {}, profilerPath: {}, outputPath: {}", config.classPath(), config.mainClass(), config.profilerPath(), config.outputPath());
        duration = chooseDuration(duration);

        File output = AsyncProfilerHelper.getInstance(config).retrieveRawOutputFile();
        Thread javaBenchmarkThread = getBenchmarkThread(config, duration, output);
        execute(javaBenchmarkThread, config, duration);

        InputStream input = new FileInputStream(output);
        var samples = parseProfile(input);

        if(config.fullSamples()) {
            // retrieve samples from JFR file
            List<String> command = new ArrayList<>();
            command.add("jfr");
            command.add("print");
            command.add("--json");
            command.add("--categories");
            command.add("JVM");
            command.add("--events");
            command.add("Profiling");
            command.add(output.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            InputStream processInputStream = process.getInputStream();
            String jfr_json = AsyncProfilerHelper.getInstance(config).rawProfilerOutput("parsed_jfr_samples" + System.currentTimeMillis() + ".json").getAbsolutePath();
            FileUtils.inputStreamToFile(processInputStream, jfr_json);

            SamplerResultsProcessor samplerResultsProcessor = new SamplerResultsProcessor();
            List<ExecutionSample> jfrSamples = samplerResultsProcessor.readJfrFile(new File(jfr_json));
            StackTraceTreeBuilder stackTraceTreeBuilder = new StackTraceTreeBuilder();
            var tree = stackTraceTreeBuilder.buildFromExecutionSamples(jfrSamples);
            root = tree;
            tree.printTree();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Failed to execute async profiler. Async profiler returned exit code 0");
            }

        } else {
            // build my tree
            var tree = generateTree(samples);
            root = tree;
            tree.printTree();
        }

        CallTreeNode callTreeNode = null;
        toPeasDS(root, callTreeNode, commit, oldCommit);
    }

    private void execute(Thread workload, Config config, Duration duration) throws InterruptedException, IOException {
        CountDownLatch latch = new CountDownLatch(1);

        final Duration finalDuration = duration;
        Thread waitingThread = new Thread(() -> {
            try {
                Thread.sleep(finalDuration.getSeconds() * 1000 + 1000);
                workload.interrupt();
                latch.countDown();
                log.debug("Benchmark thread interrupted");
            } catch (InterruptedException e) {
                log.warn("Benchmark process interrupted", e);
            }
        });

        workload.start();
        waitingThread.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            log.warn("Benchmark thread interrupted", e);
        }


        if(benchmarkException) {
            throw new RuntimeException("Benchmark exception");
        }
    }

    @Override
    public void execute(Config config, Duration samplingDuration, Duration frequency) throws InterruptedException, IOException {

    }


    private StackTraceTreeNode generateTree(List<StackTraceData> samples) {
        StackTraceTreeBuilder stackTraceTreeBuilder = new StackTraceTreeBuilder();
        return stackTraceTreeBuilder.build(samples);
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

    private Thread getBenchmarkThread(Config config, Duration duration, File output) {
        log.info("Sampling for {} seconds", duration.getSeconds());
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add(AsyncProfilerHelper.getInstance(config, output).retrieveJavaAgent(duration));
        command.add("-Dfile.encoding=UTF-8");

        if(config.classPath().contains(".jar")) {
            command.add("-jar");
        } else {
            command.add("-cp");
        }
        command.add(config.classPath());

        if(config.mainClass().contains(" ")) {
            var mains = config.mainClass().split(" ");
            command.addAll(Arrays.asList(mains));
        } else {
            command.add(config.mainClass());
        }

        return new Thread(() -> {
            try {
                CommandStarter.start(command.toArray(new String[0]));
            } catch (Exception e) {
                this.benchmarkException = true;
                throw new RuntimeException("Benchmark process failed to start", e);
            }
        });
    }

    private static List<StackTraceData> parseProfile(InputStream asyncProfilerOutput) throws IOException {
        log.info("Parsing async-profiler output");
        List<StackTraceData> samples = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(asyncProfilerOutput))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("---") && !line.contains("Execution") && !line.contains("profile")) {
                    String[] firstSplittedLine = line.split("[\\s(),%]+");
                    long timeNs = Long.parseLong(firstSplittedLine[1]);
                    double percent = Double.parseDouble(firstSplittedLine[3]);
                    int sampleCount = Integer.parseInt(firstSplittedLine[4]);

                    List<String> methods = new ArrayList<>();

                    while ((line = br.readLine()) != null && line.trim().startsWith("[")) {
                        var sanitizedLine = line.trim().substring(5);
                        methods.add(sanitizedLine);
                    }

                    samples.add(new StackTraceData(methods, timeNs, percent, sampleCount));
                }
            }
        }
        result = samples;
        return samples;
    }

    private void print(List<StackTraceData> traces) {
        for(StackTraceData trace : traces) {
            System.out.println();
            System.out.println("======================================");
            System.out.println(trace.getTimeTaken());
            for(String method : trace.getMethods()) {
                System.out.println("    " + method);
            }
            System.out.println("======================================");
            System.out.println();
        }
    }

    @Override
    public void write(String destinationFile) {
        log.info("Writing sample blocks to {}", destinationFile);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(new File(destinationFile), result);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StackTraceTreeNode getStackTraceTree() {
        return root;
    }
}
