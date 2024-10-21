package com.juoska.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CommandStarter {

    private static final Logger log = LoggerFactory.getLogger(CommandStarter.class);
    public volatile static long latestPid;

    private static Process getProcess(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Combine stdout and stderr
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            latestPid = process.pid();

            int exitCode = 0;
            try {
                exitCode = process.waitFor();
            } catch (InterruptedException e) {
                log.info("Process was interrupted. Business as usual. ");
            }

            if (exitCode != 0) {
                throw new RuntimeException("Error occurred during benchmark execution");
            }

            return process;

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static void start(String... command) {
        getProcess(command);
    }
}
