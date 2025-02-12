package io.github.terahidro2003.result.tree.collections;

import io.github.terahidro2003.result.tree.StackTraceData;

import java.util.List;

public class StackTraceDataCollection extends GenericSamplingDataCollection<StackTraceData> {
    public StackTraceDataCollection(List<StackTraceData> data) {
        super(data);
    }
}
