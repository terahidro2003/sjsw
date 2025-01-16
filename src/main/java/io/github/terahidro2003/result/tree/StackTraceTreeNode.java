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
            var measurementsList = node.payload.getMeasurements();
            final StringBuilder measurementsAsString = new StringBuilder();
            measurementsList.forEach((k,v) -> {
                v.forEach(value -> {
                    measurementsAsString.append(value);
                    measurementsAsString.append(",");
                });
            });

            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getPayload().getMethodName() +
                    " [Measurements: { " + measurementsAsString.toString() + " }]" +
                    ", cWeight: " + this.getPayload().getInitialWeight());
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StackTraceTreeNode) {
            StackTraceTreeNode other = (StackTraceTreeNode) obj;
            if (this.getPayload() != null && other.getPayload() != null) {
                var payload = this.getPayload();
                var otherPayload = other.getPayload();
                if (payload.getMethodName().equals(otherPayload.getMethodName())) {
                    return true;
                }
            }
        }
        return false;
    }
}
