package io.github.terahidro2003.cct.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StackTraceTreeNode {
    private final List<String> parentMethodNames = new ArrayList<>();

    private StackTraceTreeNode parent;
    private List<StackTraceTreeNode> children;
    private StackTraceTreePayload payload;

    private Map<String, List<Double>> measurements = new HashMap<>();
    private Map<String, List<VmMeasurement>> vmMeasurements = new HashMap<>();

    private Double initialWeight;

    public StackTraceTreeNode(StackTraceTreeNode parent, List<StackTraceTreeNode> children, StackTraceTreePayload payload) {
        this.parent = parent;
        this.children = children;
        this.payload = payload;

        if(parent != null) {
            this.parentMethodNames.addAll(parent.parentMethodNames);
        }
        this.parentMethodNames.add(this.payload.getMethodName());
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
        var measurementsList = node.getMeasurements();
        final StringBuilder measurementsAsString = new StringBuilder();
        vmMeasurements.forEach((k,v) -> {
            v.forEach(value -> {
                StringBuilder second = new StringBuilder();
                value.getMeasurements().forEach(e -> {
                    second.append(e);
                    second.append(",");
                });
                measurementsAsString.append(second.toString());
                measurementsAsString.append(";");
            });
        });

        System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getPayload().getMethodName() +
                " [Measurements: { " + measurementsAsString.toString() + " }]" +
                ", cWeight: " + this.initialWeight);

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

    public void addMeasurement(String identifier, VmMeasurement weights) {
        if (!vmMeasurements.containsKey(identifier)) {
            vmMeasurements.put(identifier, new ArrayList<>());
        }
        var currentWeights = vmMeasurements.get(identifier)
                .stream().filter(w -> w.vm == weights.vm)
                .collect(Collectors.toCollection(ArrayList::new));
        if(currentWeights.size() == 1 && weights.getMeasurements().get(0) != null) {
            currentWeights.get(0).addMeasurement(weights.getMeasurements().get(0));
        } else if (currentWeights.isEmpty()) {
            vmMeasurements.get(identifier).add(weights);
        }
    }

    public void resetVmMeasurements() {
        this.vmMeasurements = new HashMap<>();
    }

    public Map<String, List<VmMeasurement>> getVmMeasurements() {
        return vmMeasurements;
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

    public List<String> getParentMethodNames() {
        return parentMethodNames;
    }

    public StackTraceTreeNode getParent() {
        return parent;
    }

    public void setParent(StackTraceTreeNode parent) {
        this.parent = parent;
    }

    public void setChildren(List<StackTraceTreeNode> children) {
        this.children = children;
    }

    public void setPayload(StackTraceTreePayload payload) {
        this.payload = payload;
    }

    public void setVmMeasurements(Map<String, List<VmMeasurement>> vmMeasurements) {
        this.vmMeasurements = vmMeasurements;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof StackTraceTreeNode) {
            StackTraceTreeNode other = (StackTraceTreeNode) obj;
            if (this.getPayload() != null && other.getPayload() != null) {
                var payload = this.getPayload();
                var otherPayload = other.getPayload();

                if(!payload.getMethodName().equals(otherPayload.getMethodName())) {
                    return false;
                }

                List<String> o1 = other.parentMethodNames;
                List<String> o2 = this.parentMethodNames;
                return o1.equals(o2);
            }
        }
        return false;
    }
}
