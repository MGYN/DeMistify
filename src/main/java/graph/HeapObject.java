package graph;

import backwardslicing.BackwardContext;
import base.StmtPoint;
import order.SortOrder;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.AssignStmt;
import utility.Logger;

import java.util.*;

public class HeapObject implements IDGNode {
    DGraph dg;
    HashSet<SootField> field;
    SootField sootField;
    SootMethod sootMethod;
    AssignStmt assignStmt;
    boolean inited = false;
    boolean solved = false;
    ArrayList<ValuePoint> vps;
    HashSet<ValuePoint> solvedVps = new HashSet<ValuePoint>();
    private static HashMap<Integer, SootMethod> function = new HashMap<Integer, SootMethod>();
    ArrayList<HashMap<Integer, HashMap<Integer, String>>> result = new ArrayList<HashMap<Integer, HashMap<Integer, String>>>();

    private HeapObject(DGraph dg, SootField sootField, SootMethod sootMethod, HashSet<SootField> field, AssignStmt assignStmt) {
        this.dg = dg;
        this.sootField = sootField;
        this.sootMethod = sootMethod;
        this.assignStmt = assignStmt;
        this.field = field;
    }

    @Override
    public Set<IDGNode> getDependents() {
        // TODO Auto-generated method stub

        HashSet<IDGNode> dps = new HashSet<IDGNode>();
        for (ValuePoint vp : vps) {
            dps.add(vp);
        }
        return dps;

    }

    @Override
    public int getUnsovledDependentsCount() {
        // TODO Auto-generated method stub
        int count = 0;
        for (IDGNode vp : getDependents()) {
            if (!vp.hasSolved()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean hasSolved() {
        // TODO Auto-generated method stub
        return solved;
    }

    @Override
    // 数值分析
    public void solve() {
        // TODO Auto-generated method stub
        solved = true;
        Logger.print("[HEAP SOLVE]" + sootField);
        Logger.print("[SOLVING ME]" + this.hashCode());

        for (ValuePoint vp : vps) {
            ArrayList<HashMap<Integer, HashMap<Integer, String>>> vpResult = vp.getResult();
            for (HashMap<Integer, HashMap<Integer, String>> res : vpResult) {
                if (res.containsKey(-1)) {
                    result.add(res);
                }
            }
        }
    }

    @Override
    public boolean canBePartiallySolve() {
        boolean can = false;
        for (ValuePoint vp : vps) {
            if (!solvedVps.contains(vp) && vp.hasSolved()) {
                solvedVps.add(vp);
                can = true;
                for (HashMap<Integer, HashMap<Integer, String>> res : vp.getResult()) {
                    if (res.containsKey(-1)) {
                        result.add(res);
                    }
                }
            }
        }
        if (can) {
            solved = true;
        }
        return can;
    }

    @Override
    public List<BackwardContext> initIfHavenot() {
        // TODO Auto-generated method stub
        vps = new ArrayList<ValuePoint>();
        ValuePoint tmp;
        List<BackwardContext> bcs = new ArrayList<BackwardContext>();
        List<StmtPoint> sps = StmtPoint.findFieldSetter(sootField, sootMethod, field, assignStmt, true, true);
        if (sps.isEmpty())
            sps = StmtPoint.findFieldSetter(sootField, sootMethod, field, assignStmt, true, false);
        if (sps.isEmpty())
            sps = StmtPoint.findFieldSetter(sootField, sootMethod, field, assignStmt, false, false);
        for (StmtPoint sp : sps) {
            // 在BackwardContext中 tmp = ((JAssignStmt) currentInstruction).getRightOp();-1的作用
            tmp = new ValuePoint(dg, sp.getMethod_location(), sp.getBlock_location(), sp.getInstruction_location(),
                    sp.getRegIndex(), sp.getField());
            bcs.addAll(tmp.initIfHavenot());

//			vps.add(tmp);
        }
//		Logger.print("[HEAP INIT]" + sootField + " " + StmtPoint.findSetter(sootField).size());
        inited = true;
        return bcs;
    }

    public ArrayList<SootMethod> sortMethod() {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        for (SootMethod stmd : CallGraph.getOrder(sootField)) {
            int num = SortOrder.getNumber(stmd);
            if (num > 0) {
                function.put(num, stmd);
                arr.add(num);
            }
        }
        arr.sort(Comparator.naturalOrder());
        ArrayList<SootMethod> method = new ArrayList<SootMethod>();
        for (Integer i : arr) {
            method.add(function.get(i));
        }
        return method;
    }

    @Override
    public boolean inited() {
        // TODO Auto-generated method stub
        return inited;
    }

    @Override
    public ArrayList<HashMap<Integer, HashMap<Integer, String>>> getResult() {
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sootField == null) ? 0 : sootField.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HeapObject other = (HeapObject) obj;
        if (sootField == null) {
            if (other.sootField != null)
                return false;
        } else if (!sootField.equals(other.sootField))
            return false;
        return true;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        if (!inited)
            return super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append("===========================");
        sb.append(this.hashCode());
        sb.append("===========================\n");
        sb.append("Field: " + sootField + "\n");
        sb.append("Solved: " + hasSolved() + "\n");
        sb.append("Depend: ");
        for (IDGNode var : this.getDependents()) {
            sb.append(var.hashCode());
            sb.append(", ");
        }
        sb.append("\n");
        sb.append("ValueSet: \n");
        for (HashMap<Integer, HashMap<Integer, String>> resl : result) {
            sb.append("  ");
            for (int i : resl.keySet()) {
                sb.append(" |" + i + ":");
                for (int j : resl.get(i).keySet()) {
                    sb.append(resl.get(i).get(j) + ",");
                }

            }
            sb.append("\n");
        }

        return sb.toString();
    }

    static HashMap<String, HeapObject> hos = new HashMap<String, HeapObject>();

    public static HeapObject getInstance(DGraph dg, SootField sootField, SootMethod sootMethod,
                                         HashSet<SootField> field, AssignStmt assignStmt) {
        String str = sootField.toString();
        if (!hos.containsKey(str)) {
            hos.put(str, new HeapObject(dg, sootField, sootMethod, field, assignStmt));
        }
        return hos.get(str);
    }

    public static void reset() {
        hos.clear();
        function.clear();
    }
}
