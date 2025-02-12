package io.github.terahidro2003.result.tree.builder;

import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.StackTraceTreePayload;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecutionSampleTreeBuilder extends StackTraceTreeBuilder {
    public ExecutionSampleTreeBuilder() {
        super();
    }

    public StackTraceTreeNode buildFromExecutionSamples(List<ExecutionSample> samples) {
        samples.forEach(sample -> {
            Collections.reverse(sample.getStackTrace());
        });

        samples.forEach(sample -> {
            addExecutionSample(super.root, sample);
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
}
