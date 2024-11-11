package com.juoska.result;

import java.util.List;

public class StackTraceTreeNode {
    private StackTraceTreeNode parent;
    private List<StackTraceTreeNode> children;
    private String methodName;
    private Long timeTaken;
    private Double percentageOfTotalTimeTaken;
    private Integer totalNumberOfSamples;

    public StackTraceTreeNode(StackTraceTreeNode parent, List<StackTraceTreeNode> children, String methodName, Long timeTaken, Double percentageOfTotalTimeTaken, Integer totalNumberOfSamples) {
        this.parent = parent;
        this.children = children;
        this.methodName = methodName;
        this.timeTaken = timeTaken;
        this.percentageOfTotalTimeTaken = percentageOfTotalTimeTaken;
        this.totalNumberOfSamples = totalNumberOfSamples;
    }

    public StackTraceTreeNode getParent() {
        return parent;
    }

    public Integer getTotalNumberOfSamples() {
        return totalNumberOfSamples;
    }

    public Double getPercentageOfTotalTimeTaken() {
        return percentageOfTotalTimeTaken;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public List<StackTraceTreeNode> getChildren() {
        return children;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setTimeTaken(Long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public void setPercentageOfTotalTimeTaken(Double percentageOfTotalTimeTaken) {
        this.percentageOfTotalTimeTaken = percentageOfTotalTimeTaken;
    }

    public void setTotalNumberOfSamples(Integer totalNumberOfSamples) {
        this.totalNumberOfSamples = totalNumberOfSamples;
    }

    private List<StackTraceTreeNode> sortChildren(List<StackTraceTreeNode> children) {
        return children.stream().sorted((c1, c2) -> c1.getTimeTaken() <= c2.getTimeTaken()?1:-1).toList();
    }

    public void printTree() {
        printTreeRecursive(this, "", true);
    }

    private void printTreeRecursive(StackTraceTreeNode node, String prefix, boolean isLast) {
        if (node.getMethodName() != null) {
            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getMethodName() +
                    " [Time: " + node.getTimeTaken() + "ns (" + node.getTimeTaken() / (1000*1000) + "ms), " +
                    "Percent: " + node.getPercentageOfTotalTimeTaken() + "%, " +
                    "Samples: " + node.getTotalNumberOfSamples() + "]");
        }

        List<StackTraceTreeNode> children = sortChildren(node.getChildren());
        for (int i = 0; i < children.size(); i++) {
            printTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }
}
