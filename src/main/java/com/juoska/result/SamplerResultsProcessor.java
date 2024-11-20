package com.juoska.result;

import com.juoska.samplers.jfr.ExecutionSample;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SamplerResultsProcessor {
    public List<ExecutionSample> readJfrFile(File jfrFile) {
        try {
            List<ExecutionSample> samples = ExecutionSample.parseJson(jfrFile.getAbsolutePath());

            return samples;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
