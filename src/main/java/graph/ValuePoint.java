package graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import backwardslicing.BackwardContext;
import backwardslicing.BackwardController;
import base.StmtPoint;
import forwardexec.SimulateEngine;
import utility.Logger;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;

public class ValuePoint implements IDGNode {

    DGraph dg;

    SootMethod method_location;
    ArrayList<SootMethod> method;
    Block block_location;
    Unit instruction_location;
    HashSet<SootField> field;
    HashSet<Integer> target_regs = new HashSet<Integer>();
    List<BackwardContext> bcs = null;
    HashSet<BackwardContext> solvedBCs = new HashSet<BackwardContext>();

    Object appendix = "";

    ArrayList<HashMap<Integer, HashMap<Integer, String>>> result = new ArrayList<HashMap<Integer, HashMap<Integer, String>>>();

    boolean inited = false;


    boolean solved = false;

    public ValuePoint(DGraph dg, SootMethod method_location, Block block_location, Unit instruction_location,
                      List<Integer> regIndex, HashSet<SootField> field) {
        this.dg = dg;
        this.method_location = method_location;
        this.block_location = block_location;
        this.instruction_location = instruction_location;
        this.field = field;
        for (int i : regIndex) {
            target_regs.add(i);
        }
//		dg.addNode(this);
    }

    public DGraph getDg() {
        return dg;
    }

    public List<BackwardContext> getBcs() {
        return bcs;
    }

    public SootMethod getMethod_location() {
        return method_location;
    }

    public Block getBlock_location() {
        return block_location;
    }

    public Unit getInstruction_location() {
        return instruction_location;
    }

    public HashSet<SootField> getField() {
        return field;
    }

    public ArrayList<SootMethod> getMethod() {
        return method;
    }

    public Set<Integer> getTargetRgsIndexes() {
        return target_regs;
    }

    public void setAppendix(Object str) {
        appendix = str;
    }

    @Override
    public Set<IDGNode> getDependents() {
        // TODO Auto-generated method stub

        HashSet<IDGNode> dps = new HashSet<IDGNode>();
        for (BackwardContext bc : bcs) {
            for (IDGNode node : bc.getDependentHeapObjects()) {
                dps.add(node);
            }
        }
        return dps;
    }

    @Override
    public int getUnsovledDependentsCount() {
        // TODO Auto-generated method stub
        int count = 0;
        for (IDGNode node : getDependents()) {
            if (!node.hasSolved()) {
                count++;
            }
        }
        Logger.print(this.hashCode() + "[]" + count + " " + bcs.size());
        return count;
    }

    @Override
    public boolean hasSolved() {

        return solved;
    }
    public void setSolved(boolean solved) {
        this.solved = solved;
    }
    @Override
    public boolean canBePartiallySolve() {
        boolean can = false;
        boolean dsolved;
        SimulateEngine tmp;
        for (BackwardContext bc : bcs) {
            if (!solvedBCs.contains(bc)) {
                dsolved = true;
                for (HeapObject ho : bc.getDependentHeapObjects()) {
                    if (!ho.hasSolved()) {
                        dsolved = false;
                        break;
                    }
                }
                if (dsolved) {
                    solvedBCs.add(bc);
                    can = true;
                    tmp = new SimulateEngine(dg, bc);
                    tmp.simulate();
//					mergeResult(bc, tmp);
                }
            }
        }
        if (can) {
            solved = true;
        }

        return can;
    }

    @Override
    public void solve() {
        if (hasSolved()) {
            Logger.print("[SOLVED ME]" + this.instruction_location + this.hashCode());
            return;
        }
        solved = true;
        Logger.print("[SOLVING ME]" + this.instruction_location + this.hashCode());
        SimulateEngine tmp;
        for (BackwardContext var : this.getBcs()) {
            tmp = new SimulateEngine(dg, var);
            tmp.simulate();
            tmp.writeJavaScript(var.hashCode());
        }
    }

    @Override
    public boolean inited() {
        return inited;
    }

    @Override
    // 对每一个vps，如果没初始化就初始化
    public List<BackwardContext> initIfHavenot() {
        inited = true;
        bcs = BackwardController.getInstance().doBackWard(this, dg);
        return bcs;
    }

    @Override
    public ArrayList<HashMap<Integer, HashMap<Integer, String>>> getResult() {
        return result;
    }

    public static List<ValuePoint> find(DGraph dg, String signature, List<Integer> regIndex) {
        List<ValuePoint> vps = new ArrayList<ValuePoint>();
        // 获取了当前函数的调用位置，每个位置包含了当前函数的unit、block、method
        List<StmtPoint> sps = StmtPoint.findCaller(signature, regIndex, new HashSet<SootField>(), true);
        if (sps.isEmpty())
            sps = StmtPoint.findCaller(signature, regIndex, new HashSet<SootField>(), false);
        // sps对应了每一个我们找到的method_location、block_location、instruction_location
        ValuePoint tmp;
        /*
         * if(sps==null) { return null; }
         */
        for (StmtPoint sp : sps) {
            // 对每一组函数调用，new一个ValuePoint，添加到vps后面处理
            tmp = new ValuePoint(dg, sp.getMethod_location(), sp.getBlock_location(), sp.getInstruction_location(),
                    regIndex, sp.getField());
            dg.addNode(tmp);
            vps.add(tmp);
            break;
            // }
        }
        return vps;
    }

    public void print() {
        System.out.println("===============================================================");
        System.out.println("Class: " + method_location.getDeclaringClass().toString());
        System.out.println("Method: " + method_location.toString());
        System.out.println("Bolck: ");
        block_location.forEach(u -> {
            System.out.println("       " + u);
        });
        target_regs.forEach(u -> {
            System.out.println("              " + u);
        });

    }

    public String toString() {
        if (!inited)
            return super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append("===========================");
        sb.append(this.hashCode());
        sb.append("===========================\n");
        sb.append("Class: " + method_location.getDeclaringClass().toString() + "\n");
        sb.append("Method: " + method_location.toString() + "\n");
        sb.append("Target: " + instruction_location.toString() + "\n");
        sb.append("Solved: " + hasSolved() + "\n");
        sb.append("Depend: ");
        for (IDGNode var : this.getDependents()) {
            sb.append(var.hashCode());
            sb.append(", ");
        }
        sb.append("\n");
        sb.append("BackwardContexts: \n");
        BackwardContext tmp;
        for (int i = 0; i < this.bcs.size(); i++) {
            tmp = this.bcs.get(i);
            sb.append("  " + i + "\n");
            for (Stmt stmt : tmp.getExecTrace()) {
                sb.append("    " + stmt + "\n");
            }
            // sb.append(" i:");
            // for (Value iv : tmp.getIntrestedVariable()) {
            // sb.append(" " + iv + "\n");
            // }
        }
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

    public JSONObject toJson() {
        JSONObject js = new JSONObject();
        JSONObject tmp;
        for (HashMap<Integer, HashMap<Integer, String>> var : this.getResult()) {
            tmp = new JSONObject();
            for (int i : var.keySet()) {
                for (int j : var.get(i).keySet()) {
                    tmp.append(i + "", var.get(i).get(j));
                }

            }
            js.append("ValueSet", tmp);
        }
        if (bcs != null)
            for (BackwardContext bc : bcs) {
                js.append("BackwardContexts", bc.toJson());
            }
        js.put("hashCode", this.hashCode() + "");
        js.put("SootMethod", this.getMethod_location().toString());
        js.put("Block", this.getBlock_location().hashCode());
        js.put("Unit", this.getInstruction_location());
        js.put("UnitHash", this.getInstruction_location().hashCode());
        js.put("appendix", appendix);

        return js;
    }
}
