package io.github.terahidro2003.result.tree;

import java.io.Serializable;
import java.util.List;

public class StackTraceData implements Serializable {
    private List<String> methods;
    private Long timeTaken;
    private Double percentageOfTotalTimeTaken;
    private Integer totalNumberOfSamples;

    public StackTraceData(List<String> methods, Long timeTaken, Double percentageOfTotalTimeTaken, Integer totalNumberOfSamples) {
        this.methods = methods;
        this.timeTaken = timeTaken;
        this.percentageOfTotalTimeTaken = percentageOfTotalTimeTaken;
        this.totalNumberOfSamples = totalNumberOfSamples;
    }

    public List<String> getMethods() {
        return methods;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public Double getPercentageOfTotalTimeTaken() {
        return percentageOfTotalTimeTaken;
    }

    public Integer getTotalNumberOfSamples() {
        return totalNumberOfSamples;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }
}
