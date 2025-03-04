package io.github.terahidro2003.measurement.executor;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.measurement.data.MeasurementInformation;

import java.time.Duration;

public interface SjswJavaAgentCreator {
    MeasurementInformation javaAgent(Config config, int vmId, String commit, Duration samplingDuration);

    MeasurementInformation javaAgent(Config config, int vmId, String commit, Duration samplingDuration, String pattern);
}
