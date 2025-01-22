package io.github.terahidro2003.result.tree;

import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

public class StackTraceTreeBuilder {
    private StackTraceTreeNode root;

    public StackTraceTreeBuilder() {
        // "root" is not really a method, but we assume it is
        // since there are calls from GC, and asprof java agent that do not originate from "main" method
        // such "not main" calls have considerable overhead (from 5 to 15%)
        // such situation and its influence on overhead needs to be investigated
        this.root = new StackTraceTreeNode(null, new ArrayList<>(), new StackTraceTreePayload("root"));
    }

    public StackTraceTreeNode build(List<StackTraceData> organizedSamples) {
        // we need to reverse the asprof output method names
        // because the first element in asprof output is the last method call in the stack trace.
        organizedSamples.forEach(sampleBlock -> {
            Collections.reverse(sampleBlock.getMethods());
        });

        organizedSamples.forEach(sampleBlock -> addSampleBlock(root, sampleBlock));
        return root;
    }

    private void addSampleBlock(StackTraceTreeNode parent, StackTraceData sampleBlock) {
        // recursion exit check
        if(sampleBlock == null) {
            return;
        }

        // get measurement properties
        var methodNames = sampleBlock.getMethods();
        var timeTaken = sampleBlock.getTimeTaken();
        var amountOfSamples = sampleBlock.getTotalNumberOfSamples();
        var percentageOfSamples = sampleBlock.getPercentageOfTotalTimeTaken();

        // If sample has no methodNames, return.
        // Maybe consider throwing some exception at this point?
        if(methodNames == null || methodNames.isEmpty()) {
            return;
        }

        // We select the first child of current sample block
        String current = methodNames.get(0);
        StackTraceTreeNode child = null;

        // We're trying to find whether current parent has our current method as a child
        for(StackTraceTreeNode parentChild : parent.getChildren()) {
            if (parentChild.getPayload().getMethodName().equals(current)) {
                child = parentChild;
                break;
            }
        }

        // If our current parent node doesn't have our current method name as a child,
        // we assume that this is a new child of the current parent node.
        if(child == null) {
            child = new StackTraceTreeNode(parent, new ArrayList<>(), new StackTraceTreePayload(methodNames.get(0)));
            parent.getChildren().add(child);
        }

        // prepare for recursion
        // we remove the first element in method names list of a sample block (because we processed that node)
        sampleBlock.setMethods(sampleBlock.getMethods().subList(1, sampleBlock.getMethods().size()));

        // recursive call
        addSampleBlock(child, sampleBlock);
    }

    public StackTraceTreeNode buildFromExecutionSamples(List<ExecutionSample> samples) {
        samples.forEach(sample -> {
            Collections.reverse(sample.getStackTrace());
        });

        samples.forEach(sample -> {
            addExecutionSample(root, sample);
        });
        return root;
    }

    private void addExecutionSample(StackTraceTreeNode parent, ExecutionSample sample) {
        // recursion exit check
        if(sample == null) {
            return;
        }

        // get measurement properties
        var methodNames =  sample.getMethodSignatures();
        var timeTaken = 1;
        var amountOfSamples = 1;
        var percentageOfSamples = 0;

        // If sample has no methodNames, return.
        // Maybe consider throwing some exception at this point?
        if(methodNames == null || methodNames.isEmpty()) {
            return;
        }

        // We select the first child of current sample block
        String current = methodNames.get(0);
        StackTraceTreeNode child = null;

        // We're trying to find whether current parent has our current method as a child
        for(StackTraceTreeNode parentChild : parent.getChildren()) {
            if (parentChild.getPayload().getMethodName().equals(current)) {
                child = parentChild;
                break;
            }
        }

        // If our current parent node doesn't have our current method name as a child,
        // we assume that this is a new child of the current parent node.
        if(child == null) {
            child = new StackTraceTreeNode(parent, new ArrayList<>(), new StackTraceTreePayload(methodNames.get(0)));
            parent.getChildren().add(child);
        }

        // prepare for recursion
        // we remove the first element in method names list of a sample block (because we processed that node)

        sample.setStackTrace(sample.getStackTrace().subList(1, sample.getStackTrace().size()));

        // recursive call
        addExecutionSample(child, sample);
    }

    public static StackTraceTreeNode buildFromStacktraceTreeModel(StacktraceTreeModel stacktraceTreeModel) {
        StackTraceTreeNode root = null;
        var stacktraceRootNode = stacktraceTreeModel.getRoot();
        root = addNodeFromStacktraceTreeModel(stacktraceRootNode, root);
        return root;
    }

    private static StackTraceTreeNode addNodeFromStacktraceTreeModel(Node stacktraceTreeModel, StackTraceTreeNode callee) {
        StackTraceTreePayload payload = new StackTraceTreePayload(
                stacktraceTreeModel.getFrame().getMethod().getMethodName()
        );

        StackTraceTreeNode node = new StackTraceTreeNode(callee, new ArrayList<>(), payload);
        node.setInitialWeight(stacktraceTreeModel.getCumulativeWeight());

        var children = stacktraceTreeModel.getChildren();
        List<StackTraceTreeNode> newChildren = new ArrayList<>();
        for (var child : children) {
            newChildren.add(addNodeFromStacktraceTreeModel(child, node));
        }
        node.children = newChildren;
        return node;
    }

    public static StackTraceTreeNode search(StackTraceTreeNode searchable, StackTraceTreeNode tree) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(searchable.equals(currentNode)) {
                return currentNode;
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return null;
    }

    public static StackTraceTreeNode search(String searchableContent, StackTraceTreeNode tree) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(currentNode.getPayload().getMethodName().contains(searchableContent)) {
                return currentNode;
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return null;
    }
}
