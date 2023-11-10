package order.tree;

import soot.SootMethod;

import java.util.HashSet;

public interface TreeNode<T,E> {
    T getValue();

    void setValue(T value);

    HashSet<E> getChild();

    void setChild(E child);

    E getParent();

    void setParent(E parent);

    boolean hasParent();

    boolean hasBranch();

    boolean isLeave();

    boolean noChild();

    boolean isSolved();

    void setSolved(boolean solved);
}
