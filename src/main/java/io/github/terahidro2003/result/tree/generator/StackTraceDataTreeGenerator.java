package io.github.terahidro2003.result.tree.generator;

import io.github.terahidro2003.result.tree.data.StackTraceData;
import io.github.terahidro2003.result.tree.data.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.data.StackTraceTreePayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StackTraceDataTreeGenerator extends StackTraceTreeGenerator {
    public StackTraceDataTreeGenerator() {
        super();
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
}
