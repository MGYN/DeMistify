package order.tree;

import main.BorderClass;
import order.OrderMethod;
import order.SortOrder;
import order.collectSort;
import order.orderNode;
import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGGraphType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

public class SortTree {
    MethodTree mt;
    ArrayList<orderNode> rootOrder = new ArrayList<>();
    Hashtable<MethodTreeNode, orderNode> nodeTable = new Hashtable<>();
    Hashtable<SootMethod, orderNode> viewNode = new Hashtable<>();
    orderNode curOrderNode = null;
    HashSet<SootMethod> interestMethod = new HashSet<>();

    public SortTree(MethodTree mt, int depth,HashSet<SootMethod> borderMethod) throws StackOverflowError{
        this.mt = mt;
        interestMethod.addAll(borderMethod);
        if (depth > 0)
            doDepth();
    }

    public void doDepth() throws StackOverflowError{
        mt.reset();
        interestMethod.clear();
        ArrayList<SootMethod> remove = new ArrayList<>();
        for (SootMethod smthd : mt.getNodes().keySet()) {
            if (!mt.hasNode(smthd))
                continue;
            if (mt.getNode(smthd).noChild()) {
                try {
                    SootMethod parent = mt.getNode(smthd).getParent().getValue();
                    interestMethod.add(parent);
                    remove.add(parent);
                } catch (Exception e) {
                }
            }
        }
        for (SootMethod sootMethod : remove)
            if (mt.hasNode(sootMethod))
                mt.removeChild(sootMethod);

    }

    public ArrayList<SootMethod> getRootOrder(boolean naturalOrder) {
        ArrayList<SootMethod> preOrder = new ArrayList<>();
        for (MethodTreeNode root : mt.getRoots()) {
            preOrder.add(root.getValue());
        }
        SortOrder.sortMethods(preOrder, true);
        for (SootMethod smthd : preOrder) {
            MethodTreeNode root = mt.getNode(smthd);
            rootOrder.add(getOrderNode(root));
            branch(root);
        }
        return new collectSort().getSort(rootOrder, naturalOrder, interestMethod);
    }

    public void branch(MethodTreeNode preTreeNode) {
        if (preTreeNode.isSolved())
            return;
        preTreeNode.setSolved(true);
        // 记录在函数中的调用child，有的child可能没被该函数调用
        HashSet<SootMethod> solvedChildren = new HashSet<>();
        HashSet<SootMethod> children = new HashSet<>();
//        if (preTreeNode.getValue().getSignature().equals("<com.camera.mi9.function.main.ui.SplashCamMixActivity$1: void onClick(android.view.View)>"))
//            System.out.println();
        if (preTreeNode.hasBranch()) {
            preTreeNode.setSolved(true);
            for (MethodTreeNode child : preTreeNode.getChild())
                children.add(child.getValue());
            Body body = preTreeNode.getValue().retrieveActiveBody();
            CFGGraphType graphtype = CFGGraphType.getGraphType("BRIEFBLOCKGRAPH");
            DirectedGraph<Block> graph = (DirectedGraph<Block>) graphtype.buildGraph(body);
            orderNode preOrderNode = getOrderNode(preTreeNode);
            this.curOrderNode = preOrderNode;
            for (Block b : graph.getHeads()) {
                analysis(graph, b, children, preOrderNode, new HashSet<Block>(), solvedChildren);
            }
        }
        for (MethodTreeNode child : preTreeNode.getChild()) {
            if (!solvedChildren.contains(child.getValue())) {
                orderNode tmp = getOrderNode(preTreeNode);
                tmp.setNext(getOrderNode(child));
            }
            branch(child);
        }
    }


    public void analysis(DirectedGraph<Block> graph, Block block, HashSet<SootMethod> children, orderNode preOrderNode, HashSet<Block> solved, HashSet<SootMethod> solvedChildren) {
        orderNode tmp = preOrderNode;
        Unit unit = block.getHead();
        boolean exit = true;
        do {
            if (((Stmt) unit).containsInvokeExpr()) {
                SootMethod callee = ((Stmt) unit).getInvokeExpr().getMethod();
                if (children.contains(callee)) {
                    solvedChildren.add(callee);
                    tmp = getOrderNode(mt.getNode(callee));
                    if (this.curOrderNode.equals(preOrderNode))
                        preOrderNode.setNext(tmp);
                    else
                        preOrderNode.setChild(tmp);
                } else {
                    ArrayList<SootMethod> newCaller = OrderMethod.getProMethod(callee, children);
                    solvedChildren.addAll(newCaller);
                    for (SootMethod nc : newCaller) {
//                        原来是这个：callee.getDeclaringClass().isInterface()
                        if (callee.isAbstract()) {
                            tmp = getOrderNode(mt.getNode(callee));
                            tmp.setInterfacePro(true);
                            tmp.setNext(getOrderNode(mt.getNode(nc)));
                        } else
                            tmp = getOrderNode(mt.getNode(nc));
                        if (this.curOrderNode.equals(preOrderNode))
                            preOrderNode.setNext(tmp);
                        else
                            preOrderNode.setChild(tmp);
                    }
                }
            }
            if (!block.getTail().equals(unit))
                unit = block.getSuccOf(unit);
            else
                exit = false;
        } while (exit);
//        ArrayList<Block> sorted = sortSuccsOf(graph.getSuccsOf(block));
        for (Block succof : graph.getSuccsOf(block)) {
            if (succof.getIndexInMethod() > block.getIndexInMethod()) {
                // 避免循环问题
                if (graph.getSuccsOf(block).size() == 1 && solved.contains(succof))
                    continue;
                solved.add(succof);
                analysis(graph, succof, children, tmp, solved, solvedChildren);
            }
        }
    }

    public orderNode getOrderNode(MethodTreeNode tn) {
        if (!nodeTable.containsKey(tn)) {
            nodeTable.put(tn, new orderNode(tn));
            if (BorderClass.getBorderMethod().contains(tn.getValue()))
                viewNode.put(tn.getValue(), nodeTable.get(tn));
        }
        return nodeTable.get(tn);
    }

    // 这里是针对处理，可能还会存在问题
    public ArrayList<Block> sortSuccsOf(List<Block> cur) {
        Hashtable<Integer, Block> tmp = new Hashtable<>();
        ArrayList<Integer> index = new ArrayList<>();
        ArrayList<Block> result = new ArrayList<>();
        for (Block b : cur) {
            index.add(b.getIndexInMethod());
            tmp.put(b.getIndexInMethod(), b);
        }
        index.sort(Comparator.naturalOrder());
        for (Integer integer : index)
            result.add(tmp.get(integer));
        return result;
    }
}
