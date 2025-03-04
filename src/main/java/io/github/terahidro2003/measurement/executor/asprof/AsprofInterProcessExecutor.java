package io.github.terahidro2003.measurement.executor.asprof;

import io.github.terahidro2003.config.Constants;
import io.github.terahidro2003.measurement.executor.SjswInterProcessExecutor;
import one.profiler.AsyncProfiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AsprofInterProcessExecutor implements SjswInterProcessExecutor {
    private final AsyncProfiler asyncProfiler = getAsProfInstance();

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
    public void measure(File resultFile, int interval, String... include) {
        if (resultFile == null) {
            throw new NullPointerException("resultFile");
        }

        try {
            if(include != null && include.length == 1) {
                asyncProfiler.execute("start,jfr,alluser,include=*"+include[0]+"*,exclude=*jdk.internal.*,interval=" + interval + "ms,timeout=400,event=cpu,cstack=fp,file=" + resultFile.getAbsolutePath());
            } else {
                asyncProfiler.execute("start,jfr,alluser,exclude=*jdk.internal.*,interval=" + interval + "ms,event=cpu,cstack=fp,timeout=400,file=" + resultFile.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopMeasure() {
        asyncProfiler.stop();
    }

    private static AsyncProfiler getAsProfInstance() {
        if (new File(Constants.AS_PROF_FULL_PATH).exists()) {
            return AsyncProfiler.getInstance(Constants.AS_PROF_FULL_PATH);
        } else {
            return AsyncProfiler.getInstance();
        }
    }
}
