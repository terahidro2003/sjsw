package io.github.terahidro2003.cct.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmMeasurement implements Serializable {
    List<Double> measurements = new ArrayList<Double>();
    int vm;

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
