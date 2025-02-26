package io.github.terahidro2003.cct.jfr;

public class Method {
    private String classLoader;
    private String classSignature;
    private String packageName;

    /**
     * async-profiler is unable to retrieve class modifier
     * but JFR format contains such value (with async-profiler such value is zero = unknown modifier)
     * <a href="https://docs.oracle.com/javase/7/docs/api/constant-values.html#java.lang.reflect.Modifier.STRICT">Look for modifier mappings here</a>
     */
    private String classModifier;
    private String methodName;

    /**
     * Names the parameters that a method takes in the format of <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3">field descriptors</a>
     */
    private String methodDescriptor;

    /**
     * <a href="https://docs.oracle.com/javase/7/docs/api/constant-values.html#java.lang.reflect.Modifier.STRICT">Method modifier mappings</a>
     */
    private String methodModifier;

    private int lineNumber;

    private int byteCodeIndex;

    private boolean hidden;

    private String type;

    public String getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(String classLoader) {
        this.classLoader = classLoader;
    }

    public String getClassSignature() {
        return classSignature;
    }

    public void setClassSignature(String classSignature) {
        this.classSignature = classSignature;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassModifier() {
        return classModifier;
    }

    public void setClassModifier(String classModifier) {
        this.classModifier = classModifier;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public void setMethodDescriptor(String methodDescriptor) {
        this.methodDescriptor = methodDescriptor;
    }

    public String getMethodModifier() {
        return methodModifier;
    }

    public void setMethodModifier(String methodModifier) {
        this.methodModifier = methodModifier;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getByteCodeIndex() {
        return byteCodeIndex;
    }

    public void setByteCodeIndex(int byteCodeIndex) {
        this.byteCodeIndex = byteCodeIndex;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFullMethodSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(methodModifier);
        sb.append(" ");
        sb.append(methodName);
        sb.append("(");
        sb.append(methodDescriptor);
        sb.append(")");
        return sb.toString();
    }
}
