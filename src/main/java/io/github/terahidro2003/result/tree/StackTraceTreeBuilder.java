package io.github.terahidro2003.result.tree;

import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.AggregatableFrame;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.Node;
import org.openjdk.jmc.flightrecorder.stacktrace.tree.StacktraceTreeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class StackTraceTreeBuilder {
    public static final Logger log = LoggerFactory.getLogger(StackTraceTreeBuilder.class);
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

    public static StackTraceTreeNode search(List<String> searchableContent, StackTraceTreeNode tree) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(currentNode.getParentMethodNames().equals(searchableContent)) {
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

    public List<StackTraceTreeNode> filterMultiple(String searchableContent,
                                                          StackTraceTreeNode tree,
                                                          Boolean strict) {
        List<StackTraceTreeNode> filteredSubtrees = new ArrayList<>();
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(currentNode.getPayload().getMethodName().contains(searchableContent) && !strict) {
                log.info("Found testcase method subtree for {}", currentNode.getPayload().getMethodName());
                filteredSubtrees.add(currentNode);
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return filteredSubtrees;
    }

    public StackTraceTreeNode filterJvmNodes(StackTraceTreeNode root) {
        List<String> exclude = List.of(
                "libjvm.so",
                "JVM_SLEEP",
                "PlatformEvent::",
                "::PlatformEvent",
                "libc.so",
                "jdk.internal"
        );
        return filterJvmNodesRecursive(root, exclude);
    }

    public StackTraceTreeNode filterJvmNodesRecursive(StackTraceTreeNode callee, List<String> exclude) {
        for (var toExclude : exclude) {
            if(callee.getPayload().getMethodName().contains(toExclude))
            {
                return null;
            }
        }

        var children = callee.getChildren();
        List<StackTraceTreeNode> newChildren = new ArrayList<>();
        for (var child : children) {
            StackTraceTreeNode childNode = filterJvmNodesRecursive(child, exclude);
            if (child != null && childNode != null) {
                newChildren.add(childNode);
            }
        }
        callee.children = newChildren;
        return callee;
    }

    public static StackTraceTreeNode mergeTrees(List<StackTraceTreeNode> trees) {
        if (trees == null || trees.isEmpty()) {
            log.warn("Supplied tree list was null or empty");
            return null;
        }

        log.info("Merging {} amount of subtrees", trees.size());

        if (trees.size() == 1) {
            log.info("Only one subtree was provided. Returning only one subtree as merged tree.");
            return trees.get(0);
        }

        log.info("Obtaining root signatures for mergable tree root node equality check");
        List<String> rootSignatures = new ArrayList<>();
        for (StackTraceTreeNode tree : trees) {
            rootSignatures.add(tree.getPayload().getMethodName());
        }
        String previousSignature = rootSignatures.get(0);
        List<String> unequalSignatures = new ArrayList<>();
        for (String signature : rootSignatures) {
            if(!previousSignature.equals(signature)) {
                unequalSignatures.add(signature);
                log.error("One or more of the root signatures are not equal! Previous: {}, was: {}", previousSignature, signature);
            }
            previousSignature = signature;
        }

        if(!unequalSignatures.isEmpty()) {
            log.info("Unequal signatures detected: {}", unequalSignatures);
            log.info("Attempting to remove unequal root nodes");
            String finalPreviousSignature = previousSignature;
            List<StackTraceTreeNode> toDelete = trees.stream().filter(t -> !Objects.equals(t.getPayload().getMethodName(), finalPreviousSignature)).toList();
            trees.removeAll(toDelete);
            log.info("Amount of subtrees removed {}", trees.size());
        }

        StackTraceTreeNode firstTree = trees.get(0);
        if (firstTree == null) {
            throw new RuntimeException("First tree cannot be null");
        }

        Map<String, StackTraceTreeNode> mergedChildren = new HashMap<>();
        for (StackTraceTreeNode child : firstTree.getChildren()) {
            mergedChildren.put(child.getPayload().getMethodName(), child);
        }

        trees.remove(firstTree);

        for (StackTraceTreeNode tree : trees) {
            for (StackTraceTreeNode child : tree.getChildren()) {
                for (StackTraceTreeNode firstChild : firstTree.getChildren()) {
                    merge(firstChild, child, mergedChildren);
                }
            }
        }

        firstTree.children = new ArrayList<>(mergedChildren.values());
        return firstTree;
    }

    private static void merge(StackTraceTreeNode node1, StackTraceTreeNode node2,
                              Map<String, StackTraceTreeNode> mergedChildren) {
        if (mergedChildren == null) {
            throw new RuntimeException("Merged children map cannot be null");
        }

        if (node1 == null || node2 == null) {
            throw new RuntimeException("One or both of the root nodes were null");
        }

        removeMeasurements(node1);
        removeMeasurements(node2);

        if (mergedChildren.containsKey(node1.getPayload().getMethodName())) {
            mergedChildren.put(node1.getPayload().getMethodName(), node1);
        }

        if (mergedChildren.containsKey(node2.getPayload().getMethodName())) {
            mergedChildren.put(node2.getPayload().getMethodName(), node2);
        }

        if (!mergedChildren.containsKey(node1.getPayload().getMethodName())) {
            mergedChildren.put(node1.getPayload().getMethodName(), node1);
        }

        if (!mergedChildren.containsKey(node2.getPayload().getMethodName())) {
            mergedChildren.put(node2.getPayload().getMethodName(), node2);
        }

        if(node1.getPayload().getMethodName().equals(node2.getPayload().getMethodName())) {
            node1.setInitialWeight(node1.getInitialWeight() + node2.getInitialWeight());
        }
    }

    private static void removeMeasurements(StackTraceTreeNode root) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            currentNode.setMeasurements(new HashMap<>());

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    public Map<List<String>, List<Double>> createMeasurementsMap(List<StackTraceTreeNode> localTrees,
                                                                  String testcaseSignature) {
        Map<List<String>, List<Double>> measurementsMap = new HashMap<>();
        for (StackTraceTreeNode localTree : localTrees) {

            Stack<StackTraceTreeNode> stack = new Stack<>();
            stack.push(localTree);

            while (!stack.isEmpty()) {
                StackTraceTreeNode currentNode = stack.pop();

                List<String> parentSignatures = currentNode.getParentMethodNames();
                List<String> signaturesToRemove = new ArrayList<>();
                for (int i = 0; i < parentSignatures.size(); i++) {
                    if(parentSignatures.get(i).contains(testcaseSignature)) {
                        break;
                    } else {
                        signaturesToRemove.add(parentSignatures.get(i));
                    }
                }
                parentSignatures.removeAll(signaturesToRemove);

                if (!measurementsMap.containsKey(parentSignatures)) {
                    measurementsMap.put(parentSignatures, new ArrayList<>());
                    currentNode.setMeasurements(new HashMap<>());
                }
                measurementsMap.get(parentSignatures).add(currentNode.getInitialWeight());

                for (StackTraceTreeNode child : currentNode.getChildren()) {
                    if (child != null) {
                        stack.push(child);
                    }
                }
            }
        }
        return measurementsMap;
    }

    public void addLocalMeasurements(StackTraceTreeNode bat, Map<List<String>, List<Double>> measurementsMap, String identifier) {
        if (bat == null || measurementsMap == null) {
            throw new IllegalArgumentException("BAT and measurements map cannot be null");
        }

        for (Map.Entry<List<String>, List<Double>> entry : measurementsMap.entrySet()) {
            List<String> signatures = entry.getKey();
            List<Double> weights = entry.getValue();

            var result = StackTraceTreeBuilder.search(signatures, bat);
            if (result != null) {
                for (double weight : weights) {
                    result.addMeasurement(identifier, weight);
                }
            }
        }
    }

    public StackTraceTreeNode buildTree(List<File> jfrs, String commit, int vms, String testcase,
                                        boolean filterJvmNativeNodes) {
        log.info("Building tree for testcase method: {}", testcase);
        if (jfrs.isEmpty()) {
            throw new RuntimeException("JFR files cannot be empty");
        }

        jfrs = jfrs.stream().filter(jfr -> jfr.getName().contains(commit)).toList();
        log.info("Filtered JFRs for tree generation: {}", jfrs);

        SamplerResultsProcessor processor = new SamplerResultsProcessor();
        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        List<StackTraceTreeNode> vmTrees = new LinkedList<>();
        for (int i = 0; i<vms; i++) {
            log.info("Building local tree for VM: {} from JFR file: {}", i, jfrs.get(i).getName());
            StackTraceTreeNode vmTree = buildVmTree(jfrs.get(i), processor, testcase);
            vmTrees.add(vmTree);
        }

        // filters out common JVM and native method call nodes from all retrieved subtrees
        if(filterJvmNativeNodes) {
            vmTrees = vmTrees.stream().map(builder::filterJvmNodes).toList();
        }

        Map<List<String>, List<Double>> measurementsMap = builder.createMeasurementsMap(vmTrees, testcase);
        StackTraceTreeNode mergedTree = StackTraceTreeBuilder.mergeTrees(vmTrees);

        builder.addLocalMeasurements(mergedTree, measurementsMap, "11111");
        return mergedTree;
    }

    public StackTraceTreeNode buildVmTree(File jfr, SamplerResultsProcessor processor, String testcase) {
        StackTraceTreeNode bat = processor.getTreeFromJfr(List.of(jfr));

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();

        // retrieves subtrees that share the same parent node
        log.info("Filtering and retrieving diverted testcase method subtrees");
        List<StackTraceTreeNode> filteredSubtrees = builder.filterMultiple(testcase, bat, false);
        return StackTraceTreeBuilder.mergeTrees(filteredSubtrees);
    }
}
