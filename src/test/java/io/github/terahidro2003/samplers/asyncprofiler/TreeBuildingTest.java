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
        StackTraceTreeNode bat = processor.getTreeFromJfr(jfrs);

        StackTraceTreeBuilder builder = new StackTraceTreeBuilder();
        List<StackTraceTreeNode> filteredSubtrees = builder.filterMultiple(testcase, bat, false);
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
}
