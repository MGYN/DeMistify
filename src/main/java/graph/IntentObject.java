package graph;

import backwardslicing.BackwardContext;
import base.StmtPoint;
import soot.SootClass;
import soot.SootMethod;
import utility.Logger;

import java.util.*;

public class IntentObject implements IDGNode {
    DGraph dg;

    SootClass sootClass;
    String key;
    GetIntentEnum getIntentEnum;
    boolean solved;
    boolean inited;
    ArrayList<ValuePoint> vps;
    HashSet<ValuePoint> solvedVps = new HashSet<>();
    private static HashMap<Integer, SootMethod> function = new HashMap<>();
    ArrayList<HashMap<Integer, HashMap<Integer, String>>> result = new ArrayList<>();

    private IntentObject(DGraph dg, SootClass sootClass, String key, GetIntentEnum getIntentEnum) {
        this.dg = dg;
        this.sootClass = sootClass;
        this.key = key;
        this.getIntentEnum = getIntentEnum;
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
    // ÊýÖµ·ÖÎö
    public void solve() {
        // TODO Auto-generated method stub
        solved = true;
//        Logger.print("[Intent SOLVE]" + sootField);
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
        vps = new ArrayList<>();
        ValuePoint tmp;
        List<BackwardContext> bcs = new ArrayList<>();
        List<StmtPoint> sps = StmtPoint.findIntentSetter(sootClass,key,getIntentEnum);
        for (StmtPoint sp : sps) {
            tmp = new ValuePoint(dg, sp.getMethod_location(), sp.getBlock_location(), sp.getInstruction_location(),
                    sp.getRegIndex(), sp.getField());
            bcs.addAll(tmp.initIfHavenot());
        }
        inited = true;
        return bcs;
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
        result = prime * result + ((sootClass == null) ? 0 : sootClass.hashCode());
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
        IntentObject other = (IntentObject) obj;
        if (sootClass == null) {
            if (other.sootClass != null)
                return false;
        } else if (!sootClass.equals(other.sootClass))
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
//        sb.append("Field: " + sootField + "\n");
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

    static HashMap<SootClass, IntentObject> hos = new HashMap<>();

    public static IntentObject getInstance(DGraph dg, SootClass sootClass, String key, GetIntentEnum getIntentEnum) {
        if (!hos.containsKey(sootClass)) {
            hos.put(sootClass, new IntentObject(dg, sootClass,key,getIntentEnum));
        }
        return hos.get(sootClass);
    }

}
