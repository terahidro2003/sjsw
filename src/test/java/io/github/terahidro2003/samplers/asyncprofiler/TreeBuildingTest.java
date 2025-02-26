package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.cct.builder.IterativeContextTreeBuilder;
import io.github.terahidro2003.cct.builder.VmContextTreeBuilder;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.util.*;

import static io.github.terahidro2003.cct.builder.IterativeContextTreeBuilder.extractVmNumber;

@Execution(ExecutionMode.CONCURRENT)
public class TreeBuildingTest {

    final File resourcesDir = new File("src/test/resources");

    @Test
    public void test() {
        String testcase = "testMe()";
        List<File> jfrs = List.of(
                new File(resourcesDir + "/1111_1.jfr"),
                new File(resourcesDir + "/1111_2.jfr")
        );

        VmContextTreeBuilder builder = new VmContextTreeBuilder();

        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 2, testcase, false);

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

        VmContextTreeBuilder builder = new VmContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 5, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testIterativeTree() {
        String testcase = "testMe()";
        File folder = new File(resourcesDir + "/iterativeSamples");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));

        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "55bbfafd67ee1f7dc721ea945714a324708787c6", testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void test20Vms() {
        String testcase = "testMe()";
        List<File> jfrs = new ArrayList<>();
        for (int i = 1; i < 21; i++) {
            jfrs.add(new File(resourcesDir + "/1111 (" + i + ").jfr"));
        }

        VmContextTreeBuilder builder = new VmContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "1111", 20, testcase, false);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testFilenamePatterRecognition() {
        int vm = extractVmNumber("checked_sjsw_partial_vm_1_iteration_2_commit_55bbfafd67ee1f7dc721ea945714a324708787c6.jfr");
        Assertions.assertEquals(vm, 1);
    }
}
