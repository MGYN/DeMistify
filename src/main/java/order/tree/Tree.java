package order.tree;

import soot.SootMethod;

import java.util.ArrayList;

public interface Tree<T> {
    void structure(SootMethod borderMethod, ArrayList<SootMethod> callTrace);

    T getNode(SootMethod smthd);
//    void removeChild(T smthd);

}
