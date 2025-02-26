package io.github.terahidro2003.measurement.executor;

import java.io.File;
import java.util.List;

public interface SjswInterProcessExecutor {
    List<File> prepareForIterativeMeasurements(File resultsFolder, int iterations);

    void measure(File resultsFolder, int interval);

    void stopMeasure();
}
