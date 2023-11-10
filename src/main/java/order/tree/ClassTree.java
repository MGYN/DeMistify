package order.tree;

import soot.SootClass;
import soot.SootMethod;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class ClassTree implements Tree<ClassTreeNode> {
    private Hashtable<SootClass, ClassTreeNode> nodes = new Hashtable<>();
    private HashSet<ClassTreeNode> roots = new HashSet<>();

    public ClassTree() {
    }

    @Override
    public void structure(SootMethod borderMethod, ArrayList<SootMethod> callTrace) {
        ClassTreeNode root = getNode(callTrace.get(0));
        if (!root.hasParent())
            roots.add(root);
        ClassTreeNode src = root;
        callTrace.remove(0);
        for (SootMethod callee : callTrace) {
            ClassTreeNode des = getNode(callee);
            if (!isCircle(src, des) && !des.getValue().equals(src.getValue())) {
                src.setChild(des);
                des.setParent(src);
            }
            src = des;
            if (callee.getDeclaringClass().equals(borderMethod.getDeclaringClass()) || src.isSolved()) {
                src.setSolved(true);
                break;
            }
        }
    }

    @Override
    public ClassTreeNode getNode(SootMethod smthd) {
        SootClass cls = smthd.getDeclaringClass();
        if (nodes.containsKey(cls))
            return nodes.get(cls);
        else {
            ClassTreeNode node = new ClassTreeNode(cls);
            nodes.put(cls, node);
            return node;
        }
    }

    public ArrayList<SootClass> dfs(ClassTreeNode ctn) {
        ArrayList<SootClass> result = new ArrayList<>();
        if (!ctn.isSolved()) {
            for (ClassTreeNode child : ctn.getChild()) {
                result.addAll(dfs(child));
            }
        } else
            result.add(ctn.getValue());
        return result;
    }

    public HashSet<SootClass> getTerminalClass() {
        HashSet<SootClass> terminalClass = new HashSet<>();
        for (ClassTreeNode root : roots)
            terminalClass.addAll(dfs(root));
        return terminalClass;
    }

    public boolean isCircle(ClassTreeNode des, ClassTreeNode src) {
        boolean flag;
        for (ClassTreeNode child : src.getChild()) {
            if (child.equals(des))
                return true;
            else
                flag = isCircle(des, child);
            if (flag)
                return true;
        }
        return false;
    }
}
