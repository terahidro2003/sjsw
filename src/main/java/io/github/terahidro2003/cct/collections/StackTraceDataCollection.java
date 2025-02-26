package io.github.terahidro2003.cct.collections;

import io.github.terahidro2003.cct.result.StackTraceData;

import java.util.List;

public class StackTraceDataCollection extends GenericSamplingDataCollection<StackTraceData> {
    public StackTraceDataCollection(List<StackTraceData> data) {
        super(data);
    }
}
