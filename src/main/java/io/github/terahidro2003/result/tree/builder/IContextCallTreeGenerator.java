package io.github.terahidro2003.result.tree.builder;

import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.collections.ExecutionSamplesCollection;
import io.github.terahidro2003.result.tree.collections.StackTraceDataCollection;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;


public interface IContextCallTreeGenerator {

    /**
     * Build a CCT (Context-call tree) from deserialized JFR execution samples.
     * @return CCT
     */
    public StackTraceTreeNode build(ExecutionSamplesCollection samples);

    /**
     * Build a CCT (Context-call tree) from deserialized async-profiler raw output.
     * Please note, that the raw output was meant for flamegraphs and not for comprehensive analysis.
     * @param organizedSamples - deserialized collection of raw asprof data
     * @return CCT
     */
    public StackTraceTreeNode build(StackTraceDataCollection organizedSamples);

    /**
     * Convert a CCT (Context-call tree) from OpenJDKs JMC library format to SJSWs format.
     * @param stacktraceTreeModel - OpenJDK JMC tree
     * @return CCT
     */
    public StackTraceTreeNode build(StacktraceTreeModel stacktraceTreeModel);

}
