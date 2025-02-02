package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.result.tree.StackTraceTreeBuilder;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

public class TreeBuildingTest {

    final File resourcesDir = new File("src/test/resources");

    @Test
    public void test() {
        String testcase = "testing";
        List<File> jfrs = List.of(
                new File(resourcesDir + "/1.jfr"),
                new File(resourcesDir + "/2.jfr")
        );

        SamplerResultsProcessor processor = new SamplerResultsProcessor();
        StackTraceTreeNode bat = processor.getTreeFromJfr(jfrs, "11111");

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        List<StackTraceTreeNode> filteredSubtrees = builder.filterMultiple(testcase, bat, false);
        filteredSubtrees = filteredSubtrees.stream().map(builder::filterJvmNodes).toList();
        filteredSubtrees.forEach(tree -> {
            System.out.println();
            tree.printTree();
            System.out.println();
        });

        Map<List<String>, List<Double>> measurementsMap = createMeasurementsMap(filteredSubtrees, testcase);
        StackTraceTreeNode mergedTree = StackTraceTreeBuilder.mergeTrees(filteredSubtrees.get(0), filteredSubtrees.get(1));

        addLocalMeasurements(mergedTree, measurementsMap, "11111");

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    private Map<List<String>, List<Double>> createMeasurementsMap(List<StackTraceTreeNode> localTrees,
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

    private void addLocalMeasurements(StackTraceTreeNode bat, Map<List<String>, List<Double>> measurementsMap, String identifier) {
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
}
