package io.github.terahidro2003.result.tree.generator;

import io.github.terahidro2003.result.tree.data.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.data.StackTraceTreePayload;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.List;

public class StackTraceModelTreeGenerator extends StackTraceTreeGenerator {
    public StackTraceModelTreeGenerator() {
        super();
    }

    public static StackTraceTreeNode buildFromStacktraceTreeModel(StacktraceTreeModel stacktraceTreeModel) {
        StackTraceTreeNode root = null;
        var stacktraceRootNode = stacktraceTreeModel.getRoot();
        root = addNodeFromStacktraceTreeModel(stacktraceRootNode, root);
        return root;
    }

    private static StackTraceTreeNode addNodeFromStacktraceTreeModel(Node stacktraceTreeModel, StackTraceTreeNode callee) {
        AggregatableFrame frame = stacktraceTreeModel.getFrame();
        StackTraceTreePayload payload = new StackTraceTreePayload(
                frame.getHumanReadableSeparatorSensitiveString()
        );

        String methodName = frame.getMethod().getMethodName();
        Boolean isHidden = frame.getMethod().isHidden();
        if(isHidden == null) isHidden = false;

        if (methodName == null || methodName.isEmpty() || isHidden) {
            log.info("Attempting to exclude an empty node");
            if (!stacktraceTreeModel.isRoot()) {
                return null;
            }
        }

        StackTraceTreeNode node = new StackTraceTreeNode(callee, new ArrayList<>(), payload);
        node.setInitialWeight(stacktraceTreeModel.getCumulativeWeight());

        var children = stacktraceTreeModel.getChildren();
        List<StackTraceTreeNode> newChildren = new ArrayList<>();
        for (var child : children) {
            StackTraceTreeNode childNode = addNodeFromStacktraceTreeModel(child, node);
            if (child != null && childNode != null) {
                newChildren.add(childNode);
            }
        }
        node.setChildren(newChildren);
        return node;
    }
}
