package io.github.terahidro2003.samplers;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.StackTraceTreeNode;
import io.github.terahidro2003.samplers.asyncprofiler.MeasurementInformation;

import java.io.IOException;
import java.time.Duration;

public interface SamplerExecutorPipeline {

    /**
     * Generates javaagent string with configured async-profiler agent.
     * Async-profiler executables are downloaded if not specified in the configuration variable.
     * @param config - configuration object.
     * @param samplingDuration - maximum sampling duration. If Java process takes less time to execute than samplingDuration
     *                         , then the real sampling duration is equal to Java process execution time.
     * @return object with two strings: rawOutputPath - the path of the output file, javaAgentPath - java agent with async-profiler as a string.
     */
    MeasurementInformation javaAgent(Config config, Duration samplingDuration);


    void execute(long pid, Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration, String commit, String oldCommit) throws InterruptedException, IOException;

    void execute(Config config ,Duration samplingDuration, Duration frequency) throws InterruptedException, IOException;

    void write(String destinationFile);

    StackTraceTreeNode getStackTraceTree();
}
