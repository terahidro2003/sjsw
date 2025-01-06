package io.github.terahidro2003.result.tree;

import java.util.List;

public class StackTraceTreeNode {
    private StackTraceTreeNode parent;
    List<StackTraceTreeNode> children;
    private StackTraceTreePayload payload;

    public StackTraceTreeNode(StackTraceTreeNode parent, List<StackTraceTreeNode> children, StackTraceTreePayload payload) {
        this.parent = parent;
        this.children = children;
        this.payload = payload;
    }

    public StackTraceTreePayload getPayload() {
        return this.payload;
    }

    public List<StackTraceTreeNode> getChildren() {
        return children;
    }

    public void printTree() {
        printTreeRecursive(this, "", true);
    }

    private void printTreeRecursive(StackTraceTreeNode node, String prefix, boolean isLast) {
        if (node.getPayload().getMethodName() != null) {
            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getPayload().getMethodName() +
                    " [Total Samples: " + "[N.I.]" + "]");
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }
}
