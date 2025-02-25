package io.github.terahidro2003.result.tree.collections;

import java.util.ArrayList;
import java.util.List;

public class AbstractMeasurementDataCollection<T> {
    private List<T> data;

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public void addMeasurement(T measurement) {
        if (data == null) {
            data = new ArrayList<T>();
        }
        data.add(measurement);
    }
}
