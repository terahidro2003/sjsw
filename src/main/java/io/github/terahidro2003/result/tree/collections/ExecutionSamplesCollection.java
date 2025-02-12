package io.github.terahidro2003.result.tree.collections;

import io.github.terahidro2003.samplers.jfr.ExecutionSample;

import java.util.List;

public class ExecutionSamplesCollection extends GenericSamplingDataCollection<ExecutionSample> {
    public ExecutionSamplesCollection(List<ExecutionSample> data) {
        super(data);
    }
}
