package io.github.terahidro2003.cct.collections;

import io.github.terahidro2003.cct.jfr.ExecutionSample;

import java.util.List;

public class ExecutionSamplesCollection extends GenericSamplingDataCollection<ExecutionSample> {
    public ExecutionSamplesCollection(List<ExecutionSample> data) {
        super(data);
    }
}
