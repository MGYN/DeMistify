package order;

import base.GlobalStatistics;
import soot.SootMethod;
import utility.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class collectSort {
    public HashSet<SootMethod> borderMethod;
    public ArrayList<SootMethod> order = new ArrayList<>();
    static List<String> content = new ArrayList<String>();

    public collectSort() {

    }

    public ArrayList<SootMethod> getSort(ArrayList<orderNode> rootOrder, boolean naturalOrder, HashSet<SootMethod> interestMethod) {
        borderMethod = interestMethod;
        for (orderNode root : rootOrder) {
            process(root, naturalOrder);
        }
        return order;
    }

    public void process(orderNode pro, boolean naturalOrder) {
        try {
            if (pro.isSolve())
                return;
            pro.solved();
            SootMethod proM = pro.getTn().getValue();
            if (borderMethod.contains(proM) && !order.contains(proM)) {
                order.add(proM);
            }
            List<orderNode> nextEqual = new ArrayList<>(pro.getNextEqual());
            if (!naturalOrder)
                Collections.reverse(nextEqual);
            if (nextEqual.size() > 1)
                GlobalStatistics.getInstance().countBranchNext();
            for (orderNode next : nextEqual) {
                process(next, naturalOrder);
            }

            orderNode next = pro.getNext();
            if (next != null) {
                process(next, naturalOrder);
            }
            for (orderNode child : pro.getChildEqual()) {
                process(child, naturalOrder);
            }
            orderNode child = pro.getChild();
            if (child != null) {
                process(child, naturalOrder);
            }
        } catch (Exception e) {
            Logger.printE(pro.getTn().getValue() + " process is error!--------------------");
        }
    }
}
