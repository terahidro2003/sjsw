package com.juoska.samplers.asyncprofiler;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.juoska.config.Config;
import com.juoska.result.StackTraceData;
import com.juoska.samplers.SamplerExecutorPipeline;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AsyncProfilerExecutor implements SamplerExecutorPipeline {

    private static List<StackTraceData> result;

    @Override
    public void execute(int pid, Config config) throws InterruptedException, IOException {
        // TODO: specify duration in config
        // TODO: allow custom frequency in hz specified in config
        String[] command = {config.profilerPath(), "-d", "3", String.valueOf(pid)};

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

                    // TODO: sanitize '[0]' from method signatures
                    while ((line = br.readLine()) != null && line.trim().startsWith("[")) {
                        methods.add(line.trim());
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
}
