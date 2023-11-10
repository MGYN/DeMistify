package order.tree;

import soot.SootClass;
import soot.SootMethod;

import java.util.HashSet;

public class ClassTreeNode implements TreeNode<SootClass, ClassTreeNode> {
    private SootClass cls;
    private HashSet<ClassTreeNode> child;
    private HashSet<SootClass> leaves;
    private ClassTreeNode parent = null;
    private boolean branch = false;
    private boolean solved = false;
    public ClassTreeNode(SootClass cls) {
        this.cls = cls;
        this.child = new HashSet<>();
        this.leaves = new HashSet<>();
    }
    @Override
    public SootClass getValue() {
        return cls;
    }

    @Override
    public void setValue(SootClass value) {
        this.cls = value;
    }

    @Override
    public HashSet<ClassTreeNode> getChild() {
        return child;
    }

    @Override
    public void setChild(ClassTreeNode child) {
        this.child.add(child);
        if (this.child.size() > 1)
            this.branch = true;
    }

    @Override
    public ClassTreeNode getParent() {
        return parent;
    }

    @Override
    public void setParent(ClassTreeNode parent) {
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
}
