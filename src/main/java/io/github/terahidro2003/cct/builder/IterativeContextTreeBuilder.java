package io.github.terahidro2003.cct.builder;

import io.github.terahidro2003.cct.SamplerResultsProcessor;
import io.github.terahidro2003.cct.TreeUtils;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import io.github.terahidro2003.cct.result.VmMeasurement;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class IterativeContextTreeBuilder extends StackTraceTreeBuilder {

    public StackTraceTreeNode buildTree(List<File> jfrs, String commit, String testcase, boolean filterJvmNativeNodes,
                                        boolean parallelProcessing, int maxThreads) {
        log.info("Building tree for testcase method: {}", testcase);
        if (jfrs.isEmpty()) {
            throw new RuntimeException("JFR files cannot be empty");
        }

        // Gather only JFR files containing a commit hash in the filename
        jfrs = jfrs.stream().filter(jfr -> jfr.getName().contains(commit) && jfr.getName().endsWith(".jfr"))
                .collect(Collectors.toCollection(ArrayList::new));
//        jfrs = jfrs.subList(0, 2000);
        log.info("Filtered JFRs for tree generation: {}", jfrs);

        SamplerResultsProcessor processor = new SamplerResultsProcessor();
        final List<StackTraceTreeNode> vmTrees = new ArrayList<>();
        StackTraceTreeNode mergedTree = null;

        if(parallelProcessing) {
            int hardwareCoreCount = Runtime.getRuntime().availableProcessors();
            runParallelVm(jfrs, testcase, vmTrees, mergedTree, commit, maxThreads > 0 ? maxThreads : hardwareCoreCount, filterJvmNativeNodes);
        } else {
            for (int i = 0; i<jfrs.size(); i++) {
                log.info("Building local tree for index: {} from JFR file: {}", i, jfrs.get(i).getName());
                StackTraceTreeNode vmTree = buildVmTree(jfrs.get(i), processor, testcase);

                // filters out common JVM and native method call nodes from all retrieved subtrees
                if(filterJvmNativeNodes) {
                    vmTree = TreeUtils.filterJvmNodes(vmTree);
                }

                Map<List<String>, List<VmMeasurement>> measurementsMap
                        = createMeasurementsMap(List.of(vmTree), testcase, false);

                vmTrees.add(mergedTree);
                vmTrees.add(vmTree);
                mergedTree = TreeUtils.mergeTrees(vmTrees);
                vmTrees.clear();
                addLocalMeasurements(mergedTree, measurementsMap, commit, false);
            }
        }

        String folderPath = jfrs.get(0).getParentFile().getAbsolutePath();
        File output =
                new File(folderPath + File.separator + testcase + "-" + commit + "-" + UUID.randomUUID() + ".json");
        log.info("Writing merged tree to a file at location {}", output.getAbsolutePath());
        TreeUtils.writeCCTtoFile(mergedTree, output);
        return mergedTree;
    }

    public static int extractVmNumber(String filename) {
        Pattern pattern = Pattern.compile("_vm_(\\d+)_");
        Matcher matcher = pattern.matcher(filename);
        matcher.find();
        return Integer.parseInt(matcher.group(1));
    }

    private void runParallelVm(List<File> jfrs, String testcase, List<StackTraceTreeNode> vmTrees,
                               StackTraceTreeNode mergedTree, String commit, int maxThreads, boolean filterJvmNativeNodes) {
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);

        try {
            List<Callable<StackTraceTreeNode>> tasks = new ArrayList<>();
            for (File jfr : jfrs) {
                tasks.add(() -> {
                    SamplerResultsProcessor processor = new SamplerResultsProcessor();
                    log.info("Building local tree for JFR file: {}", jfr.getName());
                    StackTraceTreeNode tree = buildVmTree(jfr, processor, testcase);
                    return filterJvmNativeNodes ? TreeUtils.filterJvmNodes(tree) : tree;
                });
            }

            List<Future<StackTraceTreeNode>> futures = executorService.invokeAll(tasks);
            List<StackTraceTreeNode> collectedTrees = new ArrayList<>();

            for (Future<StackTraceTreeNode> future : futures) {
                try {
                    StackTraceTreeNode vmTree = future.get();
                    if (vmTree != null) {
                        collectedTrees.add(vmTree);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Error processing JFR file", e);
                    Thread.currentThread().interrupt();
                }
            }

            if (!collectedTrees.isEmpty()) {
                Map<List<String>, List<VmMeasurement>> measurementsMap = createMeasurementsMap(collectedTrees, testcase, false);
                synchronized (mergedTree) {
                    mergedTree = TreeUtils.mergeTrees(collectedTrees);
                    addLocalMeasurements(mergedTree, measurementsMap, commit, false);
                    vmTrees.addAll(collectedTrees);
                }
            }
        } catch (InterruptedException e) {
            log.error("Task execution interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            executorService.shutdown();
        }
    }


    public synchronized StackTraceTreeNode buildVmTree(File jfr, SamplerResultsProcessor processor, String testcase) {
        StackTraceTreeNode bat = processor.getTreeFromJfr(List.of(jfr));
        String filename = jfr.getName();
        int vm = extractVmNumber(filename);

        // retrieves subtrees that share the same parent node
        log.info("Filtering and retrieving diverted testcase method subtrees");
        List<StackTraceTreeNode> filteredSubtrees = TreeUtils.filterMultiple(testcase, bat, false);
        var mergedTree = TreeUtils.mergeTrees(filteredSubtrees);
        mergedTree.getPayload().setVm(vm);
        return mergedTree;
    }

    public synchronized void addLocalMeasurements(StackTraceTreeNode bat, Map<List<String>, List<VmMeasurement>> measurementsMap, String identifier, boolean flag) {
        if (bat == null || measurementsMap == null) {
            throw new IllegalArgumentException("BAT and measurements map cannot be null");
        }

        for (Map.Entry<List<String>, List<VmMeasurement>> entry : measurementsMap.entrySet()) {
            List<String> signatures = entry.getKey();
            List<VmMeasurement> weights = entry.getValue();

            var result = TreeUtils.search(signatures, bat);
            if (result != null) {
                for (VmMeasurement weight : weights) {
                    result.addMeasurement(identifier, weight);
                }
            }
        }
    }

    public Map<List<String>, List<VmMeasurement>> createMeasurementsMap(List<StackTraceTreeNode> localTrees,
                                                                        String testcaseSignature, boolean flag) {
        Map<List<String>, List<VmMeasurement>> measurementsMap = new HashMap<>();
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
                    measurementsMap.put(parentSignatures, new ArrayList<VmMeasurement>());
                    currentNode.setMeasurements(new HashMap<>());
                }

                measurementsMap.get(parentSignatures)
                        .add(new VmMeasurement(new ArrayList<Double>(List.of(currentNode.getInitialWeight())), localTree.getPayload().getVm()));

                for (StackTraceTreeNode child : currentNode.getChildren()) {
                    if (child != null) {
                        stack.push(child);
                    }
                }
            }
        }

        return measurementsMap;
    }
}
