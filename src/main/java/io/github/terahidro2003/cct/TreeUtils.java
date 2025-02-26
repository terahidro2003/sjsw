package io.github.terahidro2003.cct;

import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class TreeUtils {
    public static final Logger log = LoggerFactory.getLogger(TreeUtils.class);

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

    public static List<StackTraceTreeNode> filterMultiple(String searchableContent,
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

    public static StackTraceTreeNode filterJvmNodes(StackTraceTreeNode root) {
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

    public static StackTraceTreeNode filterJvmNodesRecursive(StackTraceTreeNode callee, List<String> exclude) {
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
        callee.setChildren(newChildren);
        return callee;
    }

    public static StackTraceTreeNode mergeTrees(List<StackTraceTreeNode> trees) {
        if (trees == null || trees.isEmpty()) {
            log.warn("Supplied tree list was null or empty");
            return null;
        }

        log.info("Merging {} amount of subtrees", trees.size());

        trees = new ArrayList<StackTraceTreeNode>(trees);

        trees = trees.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

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
            List<StackTraceTreeNode> toDelete = trees.stream()
                    .filter(t -> !Objects.equals(t.getPayload().getMethodName(), finalPreviousSignature))
                    .collect(Collectors.toCollection(ArrayList::new));
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

        firstTree.setChildren(new ArrayList<>(mergedChildren.values()));
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

        if (!node1.getPayload().getMethodName().equals(node2.getPayload().getMethodName())) {
            return;
        }

        removeMeasurements(node1);
        removeMeasurements(node2);

        if (!mergedChildren.containsKey(node1.getPayload().getMethodName())) {
            mergedChildren.put(node1.getPayload().getMethodName(), node1);
        }

        if (!mergedChildren.containsKey(node2.getPayload().getMethodName())) {
            mergedChildren.put(node2.getPayload().getMethodName(), node2);
        }
    }

    private static void removeMeasurements(StackTraceTreeNode root) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            currentNode.setMeasurements(new HashMap<>());
            currentNode.resetVmMeasurements();

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }
}
