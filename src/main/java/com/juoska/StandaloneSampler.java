package com.juoska;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.samplers.asyncprofiler.AsyncProfilerExecutor;
import com.juoska.utils.CommandStarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class StandaloneSampler {

    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();
    private static Config CONFIGURATION;

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectReader reader = mapper.readerFor(SamplerExecutorPipeline.class);
        CONFIGURATION = reader.readValue(new File("config.json"), Config.class);
        try {
            System.out.println("Running java process");
            startProcess();
            System.out.println("Java process launched");

            System.out.println("Retrieving PID of new process...");
            int pid = findJavaProcessPID(CONFIGURATION.mainClass());
            System.out.println("Retrieved. PID: " + pid);

            executor.execute(pid, CONFIGURATION);
            executor.write(CONFIGURATION.outputPath());
            Thread.sleep(5000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startProcess() {
        Thread pidThread = new Thread(() -> {
            try {
                runProcess(CONFIGURATION.classPath(), CONFIGURATION.mainClass());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        pidThread.start();
    }

    private static void runProcess(String classPath, String mainClass) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-Dfile.encoding=UTF-8");
        command.add("-classpath");
        command.add(classPath);
        command.add(mainClass);
        command.add("-XX:+PreserveFramePointer");
        command.add("-XX:+UnlockDiagnosticVMOptions");
        command.add("-XX:+DebugNonSafepoints");

        CommandStarter.start(command.toArray(new String[0]));
    }

    private static int findJavaProcessPID(String mainClass) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("jps");

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        var names = mainClass.split("\\.");
        String name = "";
        if(names.length > 0) {
            name = names[names.length - 1];
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(name)) {
                    var pidArray = line.split(" ");
                    if(pidArray.length == 2) {
                        return Integer.parseInt(pidArray[0]);
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("jps command failed. Status: " + exitCode);
        }
        throw new RuntimeException("Couldn't find PID for specified main class.");
    }
}
