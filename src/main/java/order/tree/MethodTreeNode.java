package order.tree;

import soot.SootMethod;

import java.util.HashSet;

public class MethodTreeNode implements TreeNode<SootMethod, MethodTreeNode> {
    private SootMethod smthd;
    private final HashSet<MethodTreeNode> child;
    private final HashSet<SootMethod> leaves;
    private MethodTreeNode parent = null;
    private boolean branch = false;
    private boolean solved = false;

    public MethodTreeNode(SootMethod smthd) {
        this.smthd = smthd;
        this.child = new HashSet<>();
        this.leaves = new HashSet<>();
    }

    @Override
    public SootMethod getValue() {
        return smthd;
    }

    @Override
    public void setValue(SootMethod smthd) {
        this.smthd = smthd;
    }

    @Override
    public HashSet<MethodTreeNode> getChild() {
        return child;
    }

    @Override
    public void setChild(MethodTreeNode child) {
        this.child.add(child);
        if (this.child.size() > 1)
            this.branch = true;
    }

    @Override
    public MethodTreeNode getParent() {
        return parent;
    }

    @Override
    public void setParent(MethodTreeNode parent) {
        this.parent = parent;
    }

    @Override
    public boolean hasParent() {
        return parent != null;
    }

    @Override
    public boolean hasBranch() {
        return branch;
    }

    @Override
    public boolean isLeave() {
        return this.leaves.size() == 0;
    }

    @Override
    public boolean noChild() {
        return this.child.size() == 0;
    }

    @Override
    public boolean isSolved() {
        return solved;
    }

    @Override
    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public HashSet<SootMethod> getLeaves() {
        return leaves;
    }

    public void setLeaves(SootMethod leaves) {
        this.leaves.add(leaves);
    }
}
