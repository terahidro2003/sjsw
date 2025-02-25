package io.github.terahidro2003.result.tree.generator;

import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.tree.data.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.TreeUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class VmContextTreeGenerator extends StackTraceTreeGenerator {
    public VmContextTreeGenerator() {
        super();
    }

    public StackTraceTreeNode buildTree(List<File> jfrs, String commit, int vms, String testcase,
                                        boolean filterJvmNativeNodes) {
        log.info("Building tree for testcase method: {}", testcase);
        if (jfrs.isEmpty()) {
            throw new RuntimeException("JFR files cannot be empty");
        }

        jfrs = jfrs.stream().filter(jfr -> jfr.getName().contains(commit))
                .collect(Collectors.toCollection(ArrayList::new));
        log.info("Filtered JFRs for tree generation: {}", jfrs);

        SamplerResultsProcessor processor = new SamplerResultsProcessor();
        List<StackTraceTreeNode> vmTrees = new ArrayList<>();
        for (int i = 0; i<vms; i++) {
            log.info("Building local tree for VM: {} from JFR file: {}", i, jfrs.get(i).getName());
            StackTraceTreeNode vmTree = buildVmTree(jfrs.get(i), processor, testcase);
            vmTrees.add(vmTree);
        }

        // filters out common JVM and native method call nodes from all retrieved subtrees
        if(filterJvmNativeNodes) {
            vmTrees = vmTrees.stream().map(TreeUtils::filterJvmNodes)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        Map<List<String>, List<Double>> measurementsMap = createMeasurementsMap(vmTrees, testcase);
        StackTraceTreeNode mergedTree = TreeUtils.mergeTrees(vmTrees);

        addLocalMeasurements(mergedTree, measurementsMap, commit);
        return mergedTree;
    }

    public StackTraceTreeNode buildVmTree(File jfr, SamplerResultsProcessor processor, String testcase) {
        StackTraceTreeNode bat = processor.getTreeFromJfr(List.of(jfr));

        // retrieves subtrees that share the same parent node
        log.info("Filtering and retrieving diverted testcase method subtrees");
        List<StackTraceTreeNode> filteredSubtrees = TreeUtils.filterMultiple(testcase, bat, false);
        return TreeUtils.mergeTrees(filteredSubtrees);
    }

    public Map<List<String>, List<Double>> createMeasurementsMap(List<StackTraceTreeNode> localTrees,
                                                                 String testcaseSignature) {
        Map<List<String>, List<Double>> measurementsMap = new HashMap<>();
        for (StackTraceTreeNode localTree : localTrees) {

            Stack<StackTraceTreeNode> stack = new Stack<>();
            stack.push(localTree);

            while (!stack.isEmpty()) {
                StackTraceTreeNode currentNode = stack.pop();
                if(currentNode == null) continue;

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

            var result = TreeUtils.search(signatures, bat);
            if (result != null) {
                for (double weight : weights) {
                    result.addMeasurement(identifier, weight);
                }
            }
        }
    }
}
