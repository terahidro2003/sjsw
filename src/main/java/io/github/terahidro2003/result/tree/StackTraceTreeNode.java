package io.github.terahidro2003.result.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StackTraceTreeNode {
    private StackTraceTreeNode parent;
    List<StackTraceTreeNode> children;
    private StackTraceTreePayload payload;

    private Map<String, List<Double>> measurements = new HashMap<>();
    private Double initialWeight;

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
            var measurementsList = node.getMeasurements();
            final StringBuilder measurementsAsString = new StringBuilder();
            measurementsList.forEach((k,v) -> {
                v.forEach(value -> {
                    measurementsAsString.append(value);
                    measurementsAsString.append(",");
                });
            });

            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getPayload().getMethodName() +
                    " [Measurements: { " + measurementsAsString.toString() + " }]" +
                    ", cWeight: " + this.initialWeight);
        }

        List<StackTraceTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

    public void addMeasurement(String identifier, Double weight) {
        if (!measurements.containsKey(identifier)) {
            measurements.put(identifier, new ArrayList<Double>());
        }
        measurements.get(identifier).add(weight);
    }

    public Double getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(Double initialWeight) {
        this.initialWeight = initialWeight;
    }

    public Map<String, List<Double>> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(Map<String, List<Double>> measurements) {
        this.measurements = measurements;
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
