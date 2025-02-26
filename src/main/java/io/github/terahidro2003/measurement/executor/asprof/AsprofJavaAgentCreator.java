package io.github.terahidro2003.measurement.executor.asprof;

import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.measurement.data.MeasurementInformation;
import io.github.terahidro2003.measurement.executor.SjswJavaAgentCreator;

import java.io.IOException;
import java.time.Duration;

import static io.github.terahidro2003.utils.FileUtils.configureResultsFolder;
import static io.github.terahidro2003.utils.FileUtils.retrieveAsyncProfiler;

public class AsprofJavaAgentCreator implements SjswJavaAgentCreator {

    @Override
    public MeasurementInformation javaAgent(Config config, int vmId, String commit, Duration samplingDuration) {
        configureResultsFolder(config);
        try {
            config = retrieveAsyncProfiler(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return AsyncProfilerHelper.getInstance(config).retrieveJavaAgent(samplingDuration, vmId, commit);
    }
}
