package io.github.terahidro2003.samplers;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.StackTraceTreeNode;

import java.io.IOException;
import java.time.Duration;

public interface SamplerExecutorPipeline {
    void execute(long pid, Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config ,Duration samplingDuration, Duration frequency) throws InterruptedException, IOException;

    void write(String destinationFile);

    StackTraceTreeNode getStackTraceTree();
}
