package com.juoska;

import com.juoska.config.Config;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.samplers.asyncprofiler.AsyncProfilerExecutor;

import java.io.File;
import java.time.Duration;

public class StandaloneSampler {

    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();
    private static Config CONFIGURATION;

    public static void main(String[] args) {
        CONFIGURATION = Config.retrieveConfiguration(new File("config.json"));
        try {
            executor.execute(CONFIGURATION, Duration.ofSeconds(60));
            executor.write(CONFIGURATION.outputPath());
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
