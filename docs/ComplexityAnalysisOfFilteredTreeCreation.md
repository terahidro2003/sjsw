# Computational Complexity Analysis of SJSW Tree Creation

## Traversals Performed during Building and Filtering
1. Building a StacktraceTreeModel: probably O(s), where s - number of samples
2. Converting StacktraceTreeModel to StackTraceTreeNode: 1 tree (unfiltered) traversal 
3. Adding local measurements: vm^2 tree traversals
4. Filtering possibly multiple testcase method trees: 1 tree traversal
5. Filtering *some* JVM and native methods: number of filtered trees * 1 filtered tree traversal
6. Creating measurement map: amount of filtered trees * 1 traversal of its own
7. Merging filtered trees: filteredTrees * amountOfRootChildren^2

This makes the total amount of classic tree traversals (i.e. traversing the tree itself) 
with phases responsible for 1 and 7 excluded:

$1 + vm*2 + 1 + 2 + 2 = vm*2 + 6$

With the minimal VM count of $2$:
$4 + 6$ = 10 tree traversals

With optimal number of $1000$ VMs:
$2000 + 6$ = 2006 tree traversals