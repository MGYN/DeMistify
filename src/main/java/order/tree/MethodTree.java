package order.tree;

import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class MethodTree implements Tree<MethodTreeNode> {
    private Hashtable<SootMethod, MethodTreeNode> nodes = new Hashtable<>();
    private HashSet<MethodTreeNode> roots = new HashSet<>();

    public MethodTree() {
    }
    @Override
    public void structure(SootMethod borderMethod, ArrayList<SootMethod> callTrace) {
        MethodTreeNode root = getNode(callTrace.get(0));
        root.setLeaves(borderMethod);
        if (!root.hasParent())
            roots.add(root);
        MethodTreeNode tmp = root;
        callTrace.remove(0);
        for (SootMethod callee : callTrace) {
            MethodTreeNode node = getNode(callee);
            node.setLeaves(borderMethod);
            tmp.setChild(node);
            node.setParent(tmp);
            tmp = node;
        }
    }
    @Override
    public MethodTreeNode getNode(SootMethod smthd) {
        if (nodes.containsKey(smthd))
            return nodes.get(smthd);
        else {
            MethodTreeNode node = new MethodTreeNode(smthd);
            nodes.put(smthd, node);
            return node;
        }
    }

    public void removeChild(SootMethod smthd) throws StackOverflowError {
        MethodTreeNode par = nodes.get(smthd).getParent();
        ArrayList<SootMethod> needToRemove = dfs(nodes.get(smthd), 0);
        for (SootMethod remove : needToRemove)
            nodes.remove(remove);
        MethodTreeNode node = new MethodTreeNode(smthd);
        node.setParent(par);
        nodes.put(smthd, node);
    }

    public boolean hasNode(SootMethod smthd) {
        return nodes.containsKey(smthd);
    }

    public HashSet<MethodTreeNode> getRoots() {
        return roots;
    }

    public Hashtable<SootMethod, MethodTreeNode> getNodes() {
        return nodes;
    }

    public void setRoots(HashSet<MethodTreeNode> roots) {
        this.roots = roots;
    }

    public void setNodes(Hashtable<SootMethod, MethodTreeNode> nodes) {
        this.nodes = nodes;
    }

    public ArrayList<SootMethod> dfs(MethodTreeNode tn, int depth) throws StackOverflowError {
        if (depth > 50)
            throw new StackOverflowError();
        ArrayList<SootMethod> result = new ArrayList<>();
        result.add(tn.getValue());
        for (MethodTreeNode child : tn.getChild()) {
            if (!tn.getParent().equals(child))
                result.addAll(dfs(child, depth + 1));
        }
        return result;
    }

    public MethodTree reset() {
        for (SootMethod smthd : this.nodes.keySet()) {
            this.nodes.get(smthd).setSolved(false);
        }
        return this;
    }
}
