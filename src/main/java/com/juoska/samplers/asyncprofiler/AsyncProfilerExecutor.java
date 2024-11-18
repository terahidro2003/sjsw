package com.juoska.samplers.asyncprofiler;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.juoska.config.Config;
import com.juoska.result.StackTraceData;
import com.juoska.result.StackTraceTreeBuilder;
import com.juoska.result.StackTraceTreeNode;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.utils.CommandStarter;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import groovy.util.logging.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class AsyncProfilerExecutor implements SamplerExecutorPipeline {

    private static final Logger log = LoggerFactory.getLogger(AsyncProfilerExecutor.class);
    private static List<StackTraceData> result;

    private volatile boolean benchmarkException = false;

    private StackTraceTreeNode root;

    @Override
    public void execute(long pid, Config config, Duration duration) throws InterruptedException, IOException {
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
        log.info("Executing async-profiler sampler with the following configuration: classPath: {}, mainClass: {}, profilerPath: {}, tempFilePath: {}, profilerRawOutputPath: {}", config.classPath(), config.mainClass(), config.profilerPath(), config.outputPath(), config.profilerRawOutputPath());
        if(duration == null || duration.getSeconds() <= 0) {
            log.warn("No duration was specified. Setting default sampling duration of 10 seconds.");
            duration = Duration.ofSeconds(10);
        } else {
            log.info("Setting sampling duration of {} seconds.", duration);
        }
        
        File rawOutputFile = new File(config.profilerRawOutputPath());
        if(rawOutputFile.createNewFile()) {
            log.info("Created new file for raw asprof output: {}", rawOutputFile.getAbsolutePath());
        } else {
            log.info("File for asprof output probably already exists: {}", rawOutputFile.getAbsolutePath());
        }

        Thread javaBenchmarkThread = getBenchmarkThread(config, duration);

        CountDownLatch latch = new CountDownLatch(1);

        Duration finalDuration = duration;
        Thread waitingThread = new Thread(() -> {
            try {
                Thread.sleep(finalDuration.getSeconds() * 1000 + 1000);
                javaBenchmarkThread.interrupt();
                latch.countDown();
                log.debug("Benchmark thread interrupted");
            } catch (InterruptedException e) {
                log.warn("Benchmark process interrupted", e);
            }
        });

        javaBenchmarkThread.start();
        waitingThread.start();
        latch.await();

        if(benchmarkException) {
            throw new RuntimeException("Benchmark exception");
        }

        File output = new File(config.profilerRawOutputPath());
        InputStream input = new FileInputStream(output);
        var samples = parseProfile(input);

        // build my tree
        var tree = generateTree(samples);
        root = tree;
        tree.printTree();

        CallTreeNode callTreeNode = null;
        toPeasDS(root, callTreeNode);
    }

    private StackTraceTreeNode generateTree(List<StackTraceData> samples) {
        StackTraceTreeBuilder stackTraceTreeBuilder = new StackTraceTreeBuilder();
        return stackTraceTreeBuilder.build(samples);
    }

    private void toPeasDS(StackTraceTreeNode node, CallTreeNode peasNode) {
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

            createPeassNode(node, peasNode);
        } else {
            createPeassNode(node, peasNode);
            peasNode = peasNode.getChildByKiekerPattern(node.getMethodName() + "()");
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (StackTraceTreeNode child : children) {
            toPeasDS(child, peasNode);
        }
    }

    private void createPeassNode(StackTraceTreeNode node, CallTreeNode peasNode) {
        peasNode.initCommitData();
        peasNode.initVMData("00000");
        peasNode.addMeasurement("00000", node.getTimeTaken());

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

        peasNode.createStatistics("00000");
    }

    private Thread getBenchmarkThread(Config config, Duration duration) {
        log.info("Sampling for {} seconds", duration.getSeconds());
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-agentpath:"+ config.profilerPath()+"=start,timeout=" + duration.getSeconds() + ",event=cpu,clock=monotonic,file=" + config.profilerRawOutputPath());
        command.add("-Dfile.encoding=UTF-8");
        command.add("-cp");
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
