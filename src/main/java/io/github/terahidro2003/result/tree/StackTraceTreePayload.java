package io.github.terahidro2003.result.tree;

public class StackTraceTreePayload {
    private String methodName;

    public StackTraceTreePayload(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
