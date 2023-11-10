package order;

import base.GlobalStatistics;
import main.AnalysisConfig;
import main.BorderClass;
import order.tree.ClassTree;
import order.tree.MethodTree;
import order.tree.SortTree;
import soot.SootClass;
import soot.SootMethod;
import utility.FileUtility;
import utility.InstanceUtility;
import utility.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;

public class SortOrder {
    static ArrayList<SootMethod> solvedMethods = OrderMethod.getSolvedMethods();
    private Hashtable<SootMethod, ArrayList<SootMethod>> borderMethodCallTrace;
    MethodTree mt;
    ClassTree ct;
    GlobalStatistics instance;
    AnalysisConfig config = AnalysisConfig.getInstance();
    private ArrayList<SootMethod> sortedOrder = new ArrayList<>();

    public SortOrder(OrderMethod orderMethod, GlobalStatistics gs) {
        borderMethodCallTrace = orderMethod.getBorderMethodCallTrace();
        instance = gs;
    }

    public void constructCallTree() {
        mt = new MethodTree();
        for (SootMethod stmd : BorderClass.getBorderMethod()) {
            int num = getNumber(stmd);
            if (num > 0) {
                try {
                    System.out.println("borderMethodCallTrace      " + stmd);
                    System.out.println(borderMethodCallTrace.get(stmd));
                    mt.structure(stmd, borderMethodCallTrace.get(stmd));
                } catch (NullPointerException e) {
                    Logger.printE(stmd + " structure is error!--------------------");
                }
            } else {
                instance.countDeathCodeMethod();
                Logger.printW(stmd + " has no Caller!--------------------");
            }
        }
    }

    public boolean getSort(int depth) throws CloneNotSupportedException {
        ct = new ClassTree();
        ArrayList<SootMethod> orderedMethod;
        ArrayList<String> result = new ArrayList<>();
        try {
            SortTree st = new SortTree(mt, depth, BorderClass.getBorderMethod());
            orderedMethod = st.getRootOrder(config.isNaturalOrder());
            sortedOrder.clear();
        } catch (StackOverflowError | Exception s) {
            return false;
        }
        if (orderedMethod.size() == 0)
            return false;
        if (depth > 0 && config.isDoLayerClass()) {
            for (SootMethod i : orderedMethod) {
                ct.structure(i, borderMethodCallTrace.get(i));
            }
            return doClassLayer(depth);
        } else {
            for (SootMethod i : orderedMethod) {
                result.add(i.getSignature());
                sortedOrder.add(i);
            }
            instance.countSortedMethod(result.size());
            FileUtility.wf_s(config.getApkDir() + config.getNaturalOrderString() + depth + "_sort.txt", result, false);
            return true;
        }

    }

    public static int getNumber(SootMethod meth) {
        return solvedMethods.indexOf(meth);
    }

    public static ArrayList<SootMethod> sortMethods(ArrayList<SootMethod> smths, boolean naturalOrder) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        Hashtable<Integer, SootMethod> function = new Hashtable<Integer, SootMethod>();
        for (SootMethod stmd : smths) {
            int num = getNumber(stmd);
            if (num >= 0) {
                function.put(num, stmd);
                arr.add(num);
            }
        }
        if (naturalOrder)
            arr.sort(Comparator.naturalOrder());
        else
            arr.sort(Comparator.reverseOrder());
        ArrayList<SootMethod> result = new ArrayList<SootMethod>();
        for (Integer i : arr) {
            result.add(function.get(i));
        }
        return result;
    }

    public ArrayList<SootMethod> getSortedOrder() {
        return sortedOrder;
    }

    public void setInstance(GlobalStatistics gs) {
        instance = gs;
    }

    public boolean doClassLayer(int depth) {
        HashSet<SootMethod> borderMethod = new HashSet<>();
        MethodTree testmt = new MethodTree();
        instance.resetDeathCodeMethod();
        for (SootClass cls : ct.getTerminalClass()) {
            GlobalStatistics.getInstance().addBorderClass();
            for (SootMethod stmd : cls.getMethods()) {
                if (BorderClass.isInBorder(stmd, ct.getTerminalClass()))
                    continue;
                if (InstanceUtility.isCallField(stmd))
                    continue;
                borderMethod.add(stmd);
                int num = getNumber(stmd);
                if (num > 0) {
                    try {
                        System.out.println("borderMethodCallTrace      " + stmd);
                        System.out.println(borderMethodCallTrace.get(stmd));
                        testmt.structure(stmd, borderMethodCallTrace.get(stmd));
                    } catch (NullPointerException e) {
                        Logger.printE(stmd + " structure is error!--------------------");
                    }
                } else {
                    instance.countDeathCodeMethod();
                    Logger.printW(stmd + " has no Caller!--------------------");
                }
            }
        }
        if (borderMethod.isEmpty())
            return false;
        GlobalStatistics.getInstance().countBorderMethod(borderMethod.size());
        ArrayList<SootMethod> orderedMethod;
        ArrayList<String> result = new ArrayList<>();
        try {
            SortTree st = new SortTree(testmt, 0, borderMethod);
            orderedMethod = st.getRootOrder(config.isNaturalOrder());
        } catch (StackOverflowError | Exception s) {
            return false;
        }
        for (SootMethod i : orderedMethod) {
            result.add(i.getSignature());
            sortedOrder.add(i);
        }
        instance.countSortedMethod(result.size());
        FileUtility.wf_s(config.getApkDir() + config.getNaturalOrderString() + depth + "_sort.txt", result, false);
        return true;
    }
}
