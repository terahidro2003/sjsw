package io.github.terahidro2003.result.tree;

import java.util.HashMap;
import java.util.Map;

public class StackTraceTreePayload {
    private String methodName;

    private Map<String, Long> measurements = new HashMap<>();

    public StackTraceTreePayload(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Map<String, Long> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, Long> measurements) {
        this.measurements = measurements;
    }

    public void addMeasurement(String identifier, long time) {
        measurements.put(identifier, time);
    }
}
