package io.github.terahidro2003;

import one.profiler.AsyncProfiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SamplingResultsAnalyzer implements SamplingMeasurementPipeline {

    private final AsyncProfiler asyncProfiler = AsyncProfiler.getInstance();

    @Override
    public List<File> prepareForIterativeMeasurements(File resultsFolder, int iterations) {
        if (resultsFolder == null) {
            throw new NullPointerException("resultsFolder");
        }

        if (!resultsFolder.exists()) {
            throw new IllegalArgumentException("resultsFolder does not exist");
        }

        List<File> resultFiles = new ArrayList<>();
        UUID uuid = UUID.randomUUID();

        for (int i = 0; i<iterations; i++) {
            resultFiles.add(new File(resultsFolder, "unprocessed_sjsw_iterative_result" + uuid + "_iteration_" + i + ".jfr"));
            try {
                resultFiles.get(i).createNewFile();
            } catch (IOException e) {
                System.out.println("Error creating file: " + resultsFolder.getAbsolutePath());
            }
        }

        return resultFiles;
    }

    @Override
    public void measure(File resultFile, int interval) {
       if (resultFile == null) {
           throw new NullPointerException("resultFile");
       }

       if (!resultFile.exists()) {
           throw new IllegalArgumentException("resultFile does not exist");
       }

       try {
           asyncProfiler.execute("start,jfr,interval=" + interval + ",event=wall,cstack=dwarf,file=" + resultFile.getAbsolutePath());
       } catch (Exception e) {
           e.printStackTrace();
       }
    }

    @Override
    public void stopMeasure() {
        asyncProfiler.stop();
    }
}
