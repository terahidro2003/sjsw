package com.juoska.samplers;

import com.juoska.config.Config;
import com.juoska.result.StackTraceTreeNode;

import java.io.IOException;
import java.time.Duration;

public interface SamplerExecutorPipeline {
    void execute(long pid, Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void write(String destinationFile);

    StackTraceTreeNode getStackTraceTree();
}
