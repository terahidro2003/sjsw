package com.juoska.utils;

import java.io.IOException;

public class CommandStarter {
    public static void start(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Error executing perf script.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
