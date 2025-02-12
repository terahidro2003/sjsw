package io.github.terahidro2003.result.tree.builder;

import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.result.tree.StackTraceTreePayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class StackTraceTreeBuilder {
    public static final Logger log = LoggerFactory.getLogger(StackTraceTreeBuilder.class);
    final StackTraceTreeNode root;

    public StackTraceTreeBuilder() {
        /*
         "root" is not really a method, but we assume it is
         since there are calls from GC, and asprof java agent that do not originate from "main" method
         such "not main" calls have considerable overhead (from 5 to 15%)
         such situation and its influence on overhead needs to be investigated
         */
        this.root = new StackTraceTreeNode(null, new ArrayList<>(), new StackTraceTreePayload("root"));
    }
}
