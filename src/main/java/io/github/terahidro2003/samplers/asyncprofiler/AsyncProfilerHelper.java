package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.config.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor.log;

public class AsyncProfilerHelper {
    final Config config;

    private File output;

    public AsyncProfilerHelper(Config config) {
        this.config = config;
        this.output = null;
    }

    private AsyncProfilerHelper(Config config, File output) {
        this.config = config;
        this.output = output;
    }

    private static AsyncProfilerHelper instance;

    public static AsyncProfilerHelper getInstance(Config config) {
        return instance == null ? new AsyncProfilerHelper(config) : instance;
    }

    public static AsyncProfilerHelper getInstance(Config config, File output) {
        return instance == null ? new AsyncProfilerHelper(config, output) : instance;
    }

    public File retrieveRawOutputFile() {
        if (output != null) {
            return this.output;
        }
        File output = rawProfilerOutput("asprof_results_" + System.currentTimeMillis() + ".json");
        if(config.fullSamples()) {
            output = rawProfilerOutput("asprof_results_" + System.currentTimeMillis() + ".jfr");
        }
        this.output = output;
        return output;
    }

    public String retrieveJavaAgent(Duration duration) {
        File output = retrieveRawOutputFile();
        final String asprofAgent;

        if(config.frequency() == null || config.frequency() == 0) {
            asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,timeout=" + duration.getSeconds() + ",interval=10ms,event=wall,clock=monotonic,file=" + output;
        } else {
            asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,interval=" + config.frequency() + "ms,timeout=" + duration.getSeconds() + ",event=wall,clock=monotonic,file=" + output;
        }
        return asprofAgent;
    }

    public File rawProfilerOutput(String name) {
        try {
            File rawOutputFile = new File(Path.of(config.outputPath(), name).toAbsolutePath().toString());
            if(rawOutputFile.createNewFile()) {
                log.info("Created new file for raw asprof output: {}", rawOutputFile.getAbsolutePath());
            } else {
                log.info("File for asprof output probably already exists: {}", rawOutputFile.getAbsolutePath());
            }
            return rawOutputFile;
        } catch (IOException e) {
            log.error("Failed to create / find profiler output file: {}", e.getMessage());
        }
        return null;
    }
}