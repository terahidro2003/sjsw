package io.github.terahidro2003.result.tree.data;

import java.util.ArrayList;
import java.util.List;

public class VmMeasurement {
    List<Double> measurements = new ArrayList<Double>();
    int vm;

    public VmMeasurement(List<Double> measurementValue, int vm) {
        this.vm = vm;
        this.measurements = measurementValue;
    }

    public int getVm() {
        return vm;
    }

    public void setVm(int vm) {
        this.vm = vm;
    }

    public List<Double> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<Double> measurements) {
        this.measurements = measurements;
    }

    public void addMeasurement(double value) {
        measurements.add(value);
    }
}
