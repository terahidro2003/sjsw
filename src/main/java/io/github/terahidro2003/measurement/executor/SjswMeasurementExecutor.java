package io.github.terahidro2003.measurement.executor;

import io.github.terahidro2003.config.Config;

import java.io.IOException;
import java.time.Duration;

public interface SjswMeasurementExecutor {
    void execute(Config config, long pid, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration) throws InterruptedException, IOException;

    void execute(Config config, Duration samplingDuration, String identifier) throws InterruptedException, IOException;
}
