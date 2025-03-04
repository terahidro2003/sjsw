package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.cct.builder.IterativeContextTreeBuilder;
import io.github.terahidro2003.cct.builder.VmContextTreeBuilder;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static io.github.terahidro2003.cct.builder.IterativeContextTreeBuilder.extractVmNumber;

public class TreeBuildingTest {

    final File resourcesDir = new File("src/test/resources");

    @Test
    @DisplayName("[VM] 2 VMs")
    public void testVmSamplingTree() {
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

        assertTree(mergedTree, 2, "1111", false);
    }

    @Test
    @DisplayName("[VM] 5 VMs")
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

        assertTree(mergedTree, 5, "1111", false);
    }

    @Test
    @DisplayName("[VM] 20 VMs")
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

        assertTree(mergedTree, 20, "1111", false);
    }

    @Test
    @DisplayName("[Iterative] 2 VMs")
    public void testIterativeTree() throws IOException {
        String testcase = "testMe()";
        File folder = new File(resourcesDir + "/iterativeSamples");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));

        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "55bbfafd67ee1f7dc721ea945714a324708787c6", testcase, false, false, 0);

        System.out.println();
        System.out.println();
        mergedTree.printTree();

        assertTree(mergedTree, 2, "55bbfafd67ee1f7dc721ea945714a324708787c6", true);
        // assert serialization results
    }

    private void assertTree(StackTraceTreeNode mergedTree, int vms, String commit, boolean iterativeSampling) {
        Assertions.assertNotNull(mergedTree);

        if(!iterativeSampling) {
            Assertions.assertEquals(vms, mergedTree.getMeasurements().get(commit).size());
            Assertions.assertTrue(mergedTree.getVmMeasurements().isEmpty());
        } else {
            Assertions.assertTrue(mergedTree.getMeasurements().isEmpty());
            Assertions.assertNotNull(mergedTree.getVmMeasurements().get(commit));
            Assertions.assertEquals(vms, mergedTree.getVmMeasurements().get(commit).size());
            Assertions.assertFalse(mergedTree.getVmMeasurements().get(commit).get(0).getMeasurements().isEmpty());
        }

        Assertions.assertFalse(mergedTree.getChildren().isEmpty());
        Assertions.assertNotNull(mergedTree.getChildren().get(0));
        Assertions.assertFalse(mergedTree.getParentMethodNames().isEmpty());
        Assertions.assertFalse(mergedTree.getPayload().getMethodName().isEmpty());
        Assertions.assertTrue(mergedTree.getPayload().getVm() >= 0);
    }

    @Test
    public void testFilenamePatterRecognition() {
        int vm = extractVmNumber("checked_sjsw_partial_vm_1_iteration_2_commit_55bbfafd67ee1f7dc721ea945714a324708787c6.jfr");
        Assertions.assertEquals(vm, 1);
    }


}
