package com.juoska.samplers;

import com.juoska.config.Config;

import java.io.IOException;

public interface SamplerExecutorPipeline {
    void execute(int pid, Config config) throws InterruptedException, IOException;

    void write(String destinationFile);
}
