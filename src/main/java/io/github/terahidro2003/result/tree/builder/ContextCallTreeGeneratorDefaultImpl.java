package io.github.terahidro2003.result.tree.builder;

import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.collections.ExecutionSamplesCollection;
import io.github.terahidro2003.result.tree.collections.StackTraceDataCollection;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

public class ContextCallTreeGeneratorDefaultImpl implements IContextCallTreeGenerator {

    @Override
    public StackTraceTreeNode build(ExecutionSamplesCollection samples) {
        return null;
    }

    @Override
    public StackTraceTreeNode build(StackTraceDataCollection organizedSamples) {
        return null;
    }

    @Override
    public StackTraceTreeNode build(StacktraceTreeModel stacktraceTreeModel) {
        return null;
    }
}
