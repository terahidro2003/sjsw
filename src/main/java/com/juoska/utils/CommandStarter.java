package com.juoska.utils;

import java.io.IOException;

public class CommandStarter {

    public volatile static long latestPid;

    private static Process getProcess(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            latestPid = process.pid();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Error executing perf script.");
            }

            return process;

        } catch (IOException | InterruptedException e) {
            System.out.println("[ERROR]: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void start(String... command) {
        getProcess(command);
    }
}
