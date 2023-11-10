package order;


import order.tree.MethodTreeNode;

import java.util.ArrayList;
import java.util.HashSet;

public class orderNode {
    private final ArrayList<orderNode> childEqual = new ArrayList<>();
    private final HashSet<orderNode> con = new HashSet<>();
    private final ArrayList<orderNode> nextEqual = new ArrayList<>();
    // 自己内部的函数
    private orderNode child = null;
    // 自己外部的函数
    private orderNode next = null;
    private final MethodTreeNode tn;
    private boolean isSolve = false;
    private boolean interfacePro = false;

    public orderNode(MethodTreeNode tn) {
        this.tn = tn;
    }

    public void setChildEqual(orderNode childEqual) {
        this.childEqual.add(childEqual);
    }

    public void setNextEqual(orderNode nextEqual) {
        if (this.equals(nextEqual))
            return;
        this.nextEqual.add(nextEqual);
    }

    public void setNext(orderNode next) {
        if (this.equals(next))
            return;
        con.add(next);
        next.setCon(this);
        if (this.nextEqual.contains(next))
            return;
        if (this.next == null)
            this.next = next;
        else if (!this.next.equals(next)) {
            setNextEqual(this.next);
            setNextEqual(next);
            this.next = null;
        }
    }

    public void setChild(orderNode child) {
        if (this.equals(child))
            return;
        con.add(child);
        child.setCon(this);
        if (this.nextEqual.contains(child) || this.childEqual.contains(child))
            return;
        if (this.child == null)
            this.child = child;
        else if (!this.child.equals(child)) {
            setChildEqual(this.child);
            setChildEqual(child);
            this.child = null;
        }
    }

    public ArrayList<orderNode> getChildEqual() {
        return childEqual;
    }

    public ArrayList<orderNode> getNextEqual() {
        return nextEqual;
    }

    public MethodTreeNode getTn() {
        return tn;
    }

    public orderNode getChild() {
        return child;
    }

    public orderNode getNext() {
        return next;
    }

    public boolean isSolve() {
        return isSolve;
    }

    public void setCon(orderNode con) {
        this.con.add(con);
    }

    public void solved() {
        isSolve = true;
    }

    public boolean isInterfacePro() {
        return interfacePro;
    }

    public void setInterfacePro(boolean extraPro) {
        this.interfacePro = extraPro;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        if (this.getChild() != null)
            s.append("  {child: ").append(this.getChild().getTn().getValue()).append("}  ");
        if (this.getNext() != null)
            s.append("  {next: ").append(this.getNext().getTn().getValue()).append("}  ");
        s.append("  {ChildEqual: ");
        for(orderNode on : this.getChildEqual()){
            s.append(on.getTn().getValue());
            s.append(" , ");
        }
        s.append("}  ");
        s.append("  {NextEqual: ");
        for(orderNode on : this.getNextEqual()){
            s.append(on.getTn().getValue());
            s.append(" , ");
        }
        s.append("}  ");
        return s.toString();
    }
}