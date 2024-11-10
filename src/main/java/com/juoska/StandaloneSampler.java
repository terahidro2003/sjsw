package com.juoska;

import com.juoska.config.Config;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.samplers.asyncprofiler.AsyncProfilerExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;

public class StandaloneSampler {

    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();
    private static final Logger log = LoggerFactory.getLogger(StandaloneSampler.class);

    public static void main(String[] args) {
        Config CONFIGURATION = Config.retrieveConfiguration(new File("config.json"));
        try {
            executor.execute(CONFIGURATION, Duration.ofSeconds(8));
            executor.write(CONFIGURATION.outputPath());
            System.exit(0);
        } catch (Exception e) {
            log.error("Error during execution", e);
        }
    }
}
