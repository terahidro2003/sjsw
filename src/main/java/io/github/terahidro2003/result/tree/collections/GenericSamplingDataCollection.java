package io.github.terahidro2003.result.tree.collections;

import java.util.List;

public abstract class GenericSamplingDataCollection<T> {
    private final List<T> data;

    public GenericSamplingDataCollection(List<T> data) {
        this.data = data;
    }

    public List<T> getData() {
        return data;
    }
}
