package com.juoska;

import com.juoska.config.Config;
import com.juoska.samplers.SamplerExecutorPipeline;
import com.juoska.samplers.asyncprofiler.AsyncProfilerExecutor;

import java.io.File;
import java.time.Duration;

public class StandaloneSampler {

    private static final SamplerExecutorPipeline executor = new AsyncProfilerExecutor();

    public static void main(String[] args) {
        Config CONFIGURATION = Config.retrieveConfiguration(new File("config.json"));
        try {
            executor.execute(CONFIGURATION, Duration.ofSeconds(8));
            executor.write(CONFIGURATION.outputPath());
            System.exit(0);
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }
}
