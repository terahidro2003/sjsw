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

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AsyncProfilerExecutor implements SamplerExecutorPipeline {

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
            System.err.println("Error while executing async-profiler.");
        }
    }

    @Override
    public void execute(Config config, Duration duration) throws InterruptedException, IOException {

        if(duration == null || duration.getSeconds() <= 0) {
            duration = Duration.ofSeconds(10);
        }
        
        File rawOutputFile = new File(config.profilerRawOutputPath());
        if(rawOutputFile.createNewFile()) {
            System.out.println("Created new file: " + rawOutputFile.getAbsolutePath());
        } else {
            System.out.println("File probably already exists: " + rawOutputFile.getAbsolutePath());
        }

        Thread javaBenchmarkThread = getBenchmarkThread(config, duration);

        CountDownLatch latch = new CountDownLatch(1);

        Duration finalDuration = duration;
        Thread waitingThread = new Thread(() -> {
            try {
                Thread.sleep(finalDuration.getSeconds() * 1000 + 1000);
                javaBenchmarkThread.interrupt();
                latch.countDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
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
    }

    public StackTraceTreeNode generateTree(List<StackTraceData> samples) {
        StackTraceTreeBuilder stackTraceTreeBuilder = new StackTraceTreeBuilder();
        return stackTraceTreeBuilder.build(samples);
    }

    private void toPeasDS(StackTraceTreeNode node, CallTreeNode peasNode) {
        MeasurementConfig measurementConfig = new MeasurementConfig(1);

        if(peasNode == null) {
            peasNode = new CallTreeNode(node.getMethodName(), "", "", measurementConfig);
        } else {
            peasNode.appendChild(node.getMethodName(), "", "");
            peasNode.addMeasurement("00000", node.getTimeTaken());
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (StackTraceTreeNode child : children) {
            toPeasDS(child, peasNode);
        }
    }

    private Thread getBenchmarkThread(Config config, Duration duration) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-agentpath:"+ config.profilerPath()+"=start,timeout=" + duration.getSeconds() + ",file=" + config.profilerRawOutputPath());
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
                        var sanitizedLine = line.trim().substring(4);
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
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String out = objectMapper.writeValueAsString(result);
            ObjectWriter writer = objectMapper.writer(new DefaultPrettyPrinter());
            writer.writeValue(new File(destinationFile), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StackTraceTreeNode getStackTraceTree() {
        return root;
    }
}
