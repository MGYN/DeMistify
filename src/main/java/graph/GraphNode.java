package graph;

import soot.SootMethod;

import java.util.HashSet;

public interface GraphNode<T> {
    void addCallBy(T clss);

    void addCallTo(T clss);

    HashSet<T> getCallBy();

    HashSet<T> getCallTo();
}
