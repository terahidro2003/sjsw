package io.github.terahidro2003.result.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackTraceTreePayload {
    private String methodName;

    private Map<String, List<Double>> measurements = new HashMap<>();

    private Double initialWeight;

    public StackTraceTreePayload(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Map<String, List<Double>> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, List<Double>> measurements) {
        this.measurements = measurements;
    }

    public void setInitialWeight(Double initialWeight) {
        this.initialWeight = initialWeight;
    }

    public Double getInitialWeight() {
        return initialWeight;
    }

    public void addMeasurement(String identifier, Double weight) {
        if (!measurements.containsKey(identifier)) {
            measurements.put(identifier, new ArrayList<Double>());
        }
        measurements.get(identifier).add(weight);
    }

    @Override
    public String toString() {
        return "StackTraceTreePayload [methodName=" + methodName + "]";
    }
}
