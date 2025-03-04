package io.github.terahidro2003.measurement.executor.asprof;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.measurement.data.MeasurementInformation;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
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

    public File retrieveRawOutputFile(int vmId, String commit) {
        if (output != null) {
            return this.output;
        }

        String outputFilePrefix = generateOutputFilePrefix(vmId, commit);

        File output = rawProfilerOutput(outputFilePrefix + ".json");
        if(config.JfrEnabled()) {
            output = rawProfilerOutput(outputFilePrefix + ".jfr");
        }
        this.output = output;
        return output;
    }

    private String generateOutputFilePrefix(int vms, String commit) {
        return "sjsw_asprof_results_" + System.currentTimeMillis() + "_" + vms + "-" + commit;
    }

    public MeasurementInformation retrieveJavaAgent(Duration duration, int vmId, String commit) {
        File output = retrieveRawOutputFile(vmId, commit);

        final String asprofAgent;

        if(config.interval() == null || config.interval() == 0) {
            if(config.timeoutDisabled()) {
                asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,interval=100ms,cstack=dwarf,event=wall,file=" + output;
            } else {
                asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,timeout=" + duration.getSeconds() + ",interval=10ms,cstack=dwarf,event=wall,file=" + output;
            }
        } else {
            if(config.timeoutDisabled()) {
                asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,interval=" + config.interval() + "ms,cstack=dwarf,event=wall,file=" + output;
            } else {
                asprofAgent = "-agentpath:"+ config.profilerPath()+"=start,interval=" + config.interval() + "ms,timeout=" + duration.getSeconds() + ",cstack=dwarf,event=wall,file=" + output;
            }
        }
        return new MeasurementInformation(output.getAbsolutePath(), asprofAgent);
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
