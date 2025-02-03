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
        String testcase = "testing()";
        List<File> jfrs = List.of(
                new File(resourcesDir + "/1.jfr"),
                new File(resourcesDir + "/2.jfr")
        );

        SamplerResultsProcessor processor = new SamplerResultsProcessor();
        StackTraceTreeNode bat = processor.getTreeFromJfr(jfrs);

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();

        // retrieves subtrees that share the same parent node
        List<StackTraceTreeNode> filteredSubtrees = builder.filterMultiple(testcase, bat, false);

        // filters out common JVM and native method call nodes from all retrieved subtrees
        filteredSubtrees = filteredSubtrees.stream().map(builder::filterJvmNodes).toList();
        filteredSubtrees.forEach(tree -> {
            System.out.println();
            tree.printTree();
            System.out.println();
        });

        Map<List<String>, List<Double>> measurementsMap = builder.createMeasurementsMap(filteredSubtrees, testcase);
        StackTraceTreeNode mergedTree = StackTraceTreeBuilder.mergeTrees(filteredSubtrees);


        builder.addLocalMeasurements(mergedTree, measurementsMap, "11111");

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testMoreVms() {
        String testcase = "testMe()";
        List<File> jfrs = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            jfrs.add(new File(resourcesDir + "/1111_" + i + ".jfr"));
        }

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 5, testcase);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }
}
