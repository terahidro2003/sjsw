package io.github.terahidro2003.result.tree.builder;

import io.github.terahidro2003.result.tree.data.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.generator.IterativeContextTreeGenerator;
import io.github.terahidro2003.result.tree.generator.VmContextTreeGenerator;

import java.io.File;
import java.util.List;

public class CctBuilder implements ICctBuilder {

    private MeasurementType measurementType;
    private List<File> jfrs;
    private String commit;
    private int vms;
    private boolean filterNativeAndJvmMethods = false;
    private String method;

    private StackTraceTreeNode buildFromIterativeMeasurements() {
        IterativeContextTreeGenerator builder = new IterativeContextTreeGenerator();
        return builder.buildTree(this.jfrs, this.commit, this.method, this.filterNativeAndJvmMethods);
    }

    private StackTraceTreeNode buildFromVmMeasurements() {
        VmContextTreeGenerator builder = new VmContextTreeGenerator();
        return builder.buildTree(this.jfrs, this.commit, this.vms, this.method, this.filterNativeAndJvmMethods);
    }

    private StackTraceTreeNode buildFromSingleMeasurement() {
        throw new RuntimeException("Not implemented yet");
    }

    private StackTraceTreeNode buildFromChunkedMeasurements() {
        throw new RuntimeException("Not implemented yet");
    }

    public CctBuilder measurementType(MeasurementType measurementType) {
        this.measurementType = measurementType;
        return this;
    }

    public CctBuilder withFiles(List<File> jfrFiles) {
        this.jfrs = jfrFiles;
        return this;
    }

    public CctBuilder commit(String commit) {
        this.commit = commit;
        return this;
    }

    public CctBuilder vms(int vms) {
        this.vms = vms;
        return this;
    }

    public CctBuilder filterNativeAndJvmMethods(boolean filterNativeAndJvmMethods) {
        this.filterNativeAndJvmMethods = filterNativeAndJvmMethods;
        return this;
    }

    public CctBuilder method(String method) {
        this.method = method;
        return this;
    }

    public StackTraceTreeNode build() {
        if (measurementType == null || jfrs == null || jfrs.isEmpty()) {
            throw new IllegalArgumentException("measurementType or jfrs is null or empty");
        }

        if (commit == null || commit.isEmpty()) {
            throw new IllegalArgumentException("Commit is empty");
        }

        if (this.method == null || this.method.isEmpty()) {
            throw new IllegalArgumentException("Method is empty");
        }

        final StackTraceTreeNode result;

        switch (measurementType) {
            case VM_MEASUREMENT -> result = this.buildFromVmMeasurements();
            case SINGLE_MEASUREMENT -> result = this.buildFromSingleMeasurement();
            case CHUNKED_MEASUREMENT -> result = this.buildFromChunkedMeasurements();
            case ITERATIVE_MEASUREMENT -> result = this.buildFromIterativeMeasurements();
            default -> throw new IllegalArgumentException("measurementType is invalid");
        }

        return result;
    }
}
