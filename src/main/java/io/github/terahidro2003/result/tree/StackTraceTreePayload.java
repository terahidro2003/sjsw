package io.github.terahidro2003.result.tree;

public class StackTraceTreePayload {
    private String methodName;
    private int vm;

    public StackTraceTreePayload(String methodName) {
        this.methodName = methodName;
    }

    public StackTraceTreePayload(String methodName, int vm) {
        this.methodName = methodName;
        this.vm = vm;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public int getVm() {
        return vm;
    }

    public void setVm(int vm) {
        this.vm = vm;
    }
}
