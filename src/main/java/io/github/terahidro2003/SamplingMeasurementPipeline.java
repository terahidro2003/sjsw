package io.github.terahidro2003;

import java.io.File;
import java.util.List;

public interface SamplingMeasurementPipeline {

    List<File> prepareForIterativeMeasurements(File resultsFolder, int iterations);

    void measure(File resultsFolder, int interval);

    void stopMeasure();
}
