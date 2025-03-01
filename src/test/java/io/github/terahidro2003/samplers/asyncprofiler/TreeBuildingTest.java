package io.github.terahidro2003.samplers.asyncprofiler;

import io.github.terahidro2003.cct.builder.ExecutionSampleTreeBuilder;
import io.github.terahidro2003.cct.builder.IterativeContextTreeBuilder;
import io.github.terahidro2003.cct.builder.VmContextTreeBuilder;
import io.github.terahidro2003.cct.jfr.ExecutionSample;
import io.github.terahidro2003.cct.jfr.Method;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.File;
import java.io.IOException;
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
    public void testIterativeTree() throws IOException {
        String testcase = "testMe()";
        File folder = new File(resourcesDir + "/iterativeSamples");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));

        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "55bbfafd67ee1f7dc721ea945714a324708787c6", testcase, false, false, 0);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testIterativeTreeMaxi() throws IOException {
        String testcase = "testMe()";
        File folder = new File("\\\\wsl.localhost\\Ubuntu-22.04\\home\\hellstone\\typ\\data\\juozas_test_project_peass\\logs\\rcaLogs\\7556a2d6c9b729ab6f04f26bc8ed0aaee2c06dec\\de.dagere.peass.MainTest\\testMe\\0\\sjsw-results\\measurement_3de635a0-28d6-44a9-9b89-011cf51ad413");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
        jfrs = jfrs.subList(0, 50);

        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "a8f0722f92d9e089d8d856f33313c0c2c2572f10", testcase, false, false, 0);

        System.out.println();
        System.out.println();
        mergedTree.printTree();
    }

    @Test
    public void testIterativeTreeMaxiParallel() throws IOException {
        String testcase = "testMe()";
        File folder = new File("\\\\wsl.localhost\\Ubuntu-22.04\\home\\hellstone\\typ\\data\\juozas_test_project_peass\\logs\\rcaLogs\\7556a2d6c9b729ab6f04f26bc8ed0aaee2c06dec\\de.dagere.peass.MainTest\\testMe\\0\\sjsw-results\\measurement_3de635a0-28d6-44a9-9b89-011cf51ad413");
        List<File> jfrs = Arrays.asList(Objects.requireNonNull(folder.listFiles()));

        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        StackTraceTreeNode mergedTree = builder.buildTree(jfrs, "a8f0722f92d9e089d8d856f33313c0c2c2572f10", testcase, false, true, 0);

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


    private List<String> path1a = new ArrayList<>(List.of("org.example.testing", "org.example.methodA", "org.example.methodB"));
    private List<String> path1b = new ArrayList<>(List.of("org.example.testing", "org.example.methodA", "org.example.someOtherMethod<init>", "org.example.someOtherMethod", "libjvm.so.Runtime1::counter_overflow()"));
    private List<String> path2 = new ArrayList<>(List.of("org.example.testing", "org.example.methodB"));
    private Map<String, List<Double>> mockMeasurementsPath = new HashMap<>();

    private void initMeasurements() {
        mockMeasurementsPath.put("org.example.testing", List.of(520.0, 580.0, 745.0, 587.0, 147.0));
        mockMeasurementsPath.put("org.example.methodA", List.of(420.0, 580.0, 745.0, 587.0, 147.0));
        mockMeasurementsPath.put("org.example.methodB", List.of(100.0, 580.0, 745.0, 587.0, 147.0));
        mockMeasurementsPath.put("org.example.someOtherMethod<init>", List.of(520.0, 580.0, 745.0, 587.0, 147.0));
        mockMeasurementsPath.put("org.example.someOtherMethod", List.of(520.0, 580.0, 745.0, 587.0, 147.0));
        mockMeasurementsPath.put("libjvm.so.Runtime1::counter_overflow()", List.of(520.0, 580.0, 745.0, 587.0, 147.0));
    }

    @Test
    public void testIterativeTreeIntegrity() {
        String testcase = "testMe()";
        String commit = "a8f0722f92d9e089d8d856f33313c0c2c2572f10";
        int iterations = 5;
        reverseStacktraces();
//        IterativeContextTreeBuilder builder = new IterativeContextTreeBuilder();
        prepareFakeTree(List.of(path1a, path1b), commit, 2);
    }

    private void reverseStacktraces() {
        Collections.reverse(path1a);
        Collections.reverse(path1b);
        Collections.reverse(path2);
    }

    private StackTraceTreeNode prepareFakeTree(List<List<String>> pathsAsMethodNames, String commit, int vms) {
        List<ExecutionSample> samples = new ArrayList<>();
        pathsAsMethodNames.forEach(path -> {
            samples.add(getMockExecutionSample(path));
        });

        ExecutionSampleTreeBuilder treeBuilder = new ExecutionSampleTreeBuilder();
        StackTraceTreeNode tree = treeBuilder.buildFromExecutionSamples(samples);
        addFakeMeasurements(tree, commit, vms);

        return tree;
    }

    private void addFakeMeasurements(StackTraceTreeNode root, String commit, int vms) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            int randomAmountofSamples = RandomUtils.nextInt(1, 1000);
            for (int i = 0; i < vms; i++) {
                int amount = randomAmountofSamples * RandomUtils.nextInt(1, 5);
                currentNode.addMeasurement(commit, (double) amount);
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    private ExecutionSample getMockExecutionSample(List<String> stacktraceAsString) {
        List<Method> stacktrace = new ArrayList<>();
        stacktraceAsString.forEach(s -> {
            Method method = new Method();
            method.setMethodName(s);
            method.setMethodModifier("public void");
            method.setMethodDescriptor("int, long");
            stacktrace.add(method);
        });
        ExecutionSample sample = new ExecutionSample();
        sample.setStackTrace(stacktrace);
        return sample;
    }

    @Test
    public void testFilenamePatterRecognition() {
        int vm = extractVmNumber("checked_sjsw_partial_vm_1_iteration_2_commit_55bbfafd67ee1f7dc721ea945714a324708787c6.jfr");
        Assertions.assertEquals(vm, 1);
    }


}
