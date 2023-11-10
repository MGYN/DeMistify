package backwardslicing;

import base.GlobalStatistics;
import base.ParameterTransferStmt;
import base.StmtPoint;
import forwardexec.StmtPath;
import graph.CallGraph;
import graph.DGraph;
import graph.GetIntentEnum;
import graph.HeapObject;
import graph.IntentObject;
import graph.ValuePoint;
import main.BorderClass;
import order.OrderMethod;
import org.json.JSONObject;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewExpr;
import soot.jimple.internal.JimpleLocal;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import utility.BlockGenerator;
import utility.EnumUtility;
import utility.InstanceUtility;
import utility.Logger;
import utility.OtherUtility;

import java.util.*;

public class BackwardContext extends AbstractStmtSwitch implements StmtPath, ICollecter {

    ValuePoint startPoint;
    DGraph dg;

    ArrayList<SootMethod> methodes;
    ArrayList<Block> blockes;
    Unit currentInstruction;
    HashSet<SootField> field;
    HashSet<Value> intrestedVariable;
    ArrayList<Stmt> execTrace;
    ArrayList<Value> fieldTraced;
    ArrayList<String> fieldTracedSelf;
    ArrayList<Block> analysedBlock;

    HashSet<HeapObject> dependentHeapObjects;
    Stack<CallStackItem> callStack;
    boolean finished = false;
    int depth = 0;

    @SuppressWarnings("unchecked")
    public BackwardContext(BackwardContext oldBc) {
        startPoint = oldBc.getStartPoint();
        dg = oldBc.getDg();
        depth = oldBc.getDepth();
        methodes = (ArrayList<SootMethod>) oldBc.getMethodes().clone();
        blockes = (ArrayList<Block>) oldBc.getBlockes().clone();
        currentInstruction = oldBc.getCurrentInstruction();
        field = oldBc.getField();
        intrestedVariable = (HashSet<Value>) oldBc.getIntrestedVariable().clone();
        execTrace = (ArrayList<Stmt>) oldBc.getExecTrace().clone();
        fieldTraced = (ArrayList<Value>) oldBc.getFieldTraced().clone();
        fieldTracedSelf = (ArrayList<String>) oldBc.getFieldTracedSelf().clone();
        analysedBlock = (ArrayList<Block>) oldBc.getAnalyzedBlock().clone();
        dependentHeapObjects = (HashSet<HeapObject>) oldBc.getDependentHeapObjects().clone();
        callStack = (Stack<CallStackItem>) oldBc.getCallStack().clone();

    }

    public BackwardContext(ValuePoint startPoint, DGraph dg) {
        this.startPoint = startPoint;
        this.dg = dg;

        methodes = new ArrayList<>();
        methodes.add(0, startPoint.getMethod_location());

        blockes = new ArrayList<>();
        blockes.add(0, startPoint.getBlock_location());
        field = startPoint.getField();
        intrestedVariable = new HashSet<>();
        execTrace = new ArrayList<>();
        fieldTraced = new ArrayList<>();
        fieldTracedSelf = new ArrayList<>();
        analysedBlock = new ArrayList<>();
        dependentHeapObjects = new HashSet<>();
        callStack = new Stack<>();

        currentInstruction = startPoint.getInstruction_location();
        Stmt stmt = (Stmt) currentInstruction;
        doInnerField(startPoint.getMethod_location(), stmt);
        execTrace.add(0, stmt);
        System.out.println("Start ---  " + currentInstruction);
        // init
        processStmt(stmt);
        if (stmt.containsInvokeExpr()) {
            if (!EnumUtility.isIncludePut(stmt.getInvokeExpr().getMethod().getSignature())) {
                Value base = InstanceUtility.getBaseInvoke(stmt);
                if (base != null)
                    this.addIntrestedVariable(base);
            }
        }
    }

    public boolean backWardHasFinished() {
        // return intrestedVariable.size() == 0;
        return finished || intrestedVariable.size() == 0;
    }

    public List<BackwardContext> oneStepBackWard() {
        Unit nextInstrct = this.getCurrentBlock().getPredOf(currentInstruction);
        if (nextInstrct != null) {
            return oneStepBackWard(nextInstrct);
        } else {
            List<BackwardContext> newBc = new ArrayList<>();
            CompleteBlockGraph cbg = BlockGenerator.getInstance()
                    .generate(this.getCurrentMethod().retrieveActiveBody());
            // 如果该块在当前函数的块头
            if (cbg.getHeads().contains(this.getCurrentBlock())) {
                GlobalStatistics.getInstance().countBackWard2Caller();
                if (this.getCallStack().isEmpty()) {
                    boolean allisParameterRef = true;
                    String ostr = "";
                    for (Value var : this.getIntrestedVariable()) {
                        ostr += var + ",";
                        if (!(var instanceof ParameterRef)) {
                            allisParameterRef = false;
                        }
                    }
                    if (!allisParameterRef) {
                        Logger.printW(String.format("[%s] [Not all the interesteds are ParameterRef]: %s",
                                this.hashCode(), ostr));
                        finished = true;
                        return newBc;
                    }
                    return oneStepBackWard2Caller();
                } else {
                    // 去到调用该函数的地方
                    getBackFromACall();
                    return newBc;
                }
            } else {
                // 如果该块不是函数头，则将所有前置块加入列表
                List<Block> bs = new ArrayList<>();
                bs.addAll(cbg.getPredsOf(this.getCurrentBlock()));
                try {
                    if (bs.size() == 0) {
                        Logger.printW(
                                String.format("[%s] [No PredsOf]: %s", this.hashCode(), this.getCurrentInstruction()));
                        finished = true;
                        return newBc;
                    }
                } catch (Exception e) {
                    System.out.println(e);
                }
                Block tmp = this.getCurrentBlock();
                // 这里存在些问题，容易吞掉某些block
                for (int index = 1; index <= bs.size(); index++) {
                    if (this.getCurrentBlock().getIndexInMethod() > bs.get(bs.size() - index).getIndexInMethod()) {
                        this.setCurrentBlock(bs.get(bs.size() - index));
                        break;
                    }
                }
                if (tmp.equals(this.getCurrentBlock())) {
                    this.setCurrentBlock(bs.get(0));
                }
//                this.setCurrentBlock(bs.get(0));
                // bs是当前块的前向所有块
                // xunhuan
//                for (Block tb : bs) {
//                    if (tb == this.getCurrentBlock()
//                            || tb.getIndexInMethod() > this.getCurrentBlock().getIndexInMethod())
//                        continue;
//                    tmp = this.clone();
//                    tmp.setCurrentBlock(tb);
//                    newBc.addAll(tmp.oneStepBackWard(tb.getTail()));
//                    newBc.add(tmp);
//                    break;
//                }
                newBc.addAll(this.oneStepBackWard(this.getCurrentBlock().getTail()));
                return newBc;
            }
        }
    }


    public List<BackwardContext> oneStepBackWard(Unit nextInstrct) {
        List<BackwardContext> newBc = new ArrayList<BackwardContext>();
        currentInstruction = nextInstrct;
        boolean containsIntrestedThings = false;
        Stmt stmt = (Stmt) this.currentInstruction;
        // 这里到底要不要UseAndDef？
        for (ValueBox box : currentInstruction.getUseAndDefBoxes()) {
            if (intrestedVariable.contains(box.getValue())) {
                if (stmt.containsInvokeExpr()) {
                    if (stmt.getInvokeExpr().getArgs().contains(box.getValue())) {
                        if (stmt instanceof AssignStmt) {
                            if (((AssignStmt) this.currentInstruction).getLeftOp().equals(box.getValue())) {
                                containsIntrestedThings = true;
                                break;
                            }
                        }
                        continue;
                    }
                }
                if (stmt instanceof AssignStmt) {
                    AssignStmt as = (AssignStmt) stmt;
                    // $r8 = staticinvoke <visidon.Lib.VerificationAPI: visidon.Lib.VerificationAPI$c c(visidon.Lib.b)>($r2);
                    // $i0 = $r2.<visidon.Lib.b: int c>;
                    // is not expect
                    if(as.getRightOp() instanceof InstanceFieldRef){
                        Value base = InstanceUtility.getBaseField(stmt);
                        if (base==box.getValue())
                            continue;
                    }
//                    if (as.getUseBoxes().contains(box) && as.getRightOp() instanceof Local)
//                        continue;
                }
                containsIntrestedThings = true;

                break;
                // 判断如果是数组类型，如$r2[8] = 1.0F，box.getValue()=$r2[8]，而((ArrayRef)
                // box.getValue()).getBase())=$r2
            } else if (box.getValue() instanceof ArrayRef
                    && intrestedVariable.contains(((ArrayRef) box.getValue()).getBase())) {
                containsIntrestedThings = true;

                break;
            } else if (box.getValue() instanceof InstanceFieldRef) {
                for (Value ins : intrestedVariable) {
                    if (ins.toString().equals(box.getValue().toString())) {
                        containsIntrestedThings = true;
                        break;
                    }
                }
            }

        }
//        if (containsIntrestedThings) {
//            if (stmt.containsInvokeExpr()) {
//                SootMethod stmd= stmt.getInvokeExpr().getMethod();
//                if (stmd.getReturnType().toString().equals("android.os.IBinder") && stmd.getParameterCount()==1 && stmd.getParameterType(0).toString().equals("java.lang.String")) {
//                    String str = stmt.getInvokeExpr().getArg(0).toString();
//                    if (InstanceUtility.getSootClass(str) == null)
//                        containsIntrestedThings = false;
//                }
//            }
//        }
        // 解决存在$r1 = new a(); $r2 = $r1; $r2.init()
        if (!containsIntrestedThings) {
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr().getMethod().isConstructor()) {
                Value base = ((SpecialInvokeExpr) stmt.getInvokeExpr()).getBase();
                for (Value value : intrestedVariable) {
                    if (base.getType().equals(value.getType())) {
                        ParameterTransferStmt tmp = new ParameterTransferStmt(value, base);
                        this.getExecTrace().add(0, tmp);
                        intrestedVariable.remove(value);
                        containsIntrestedThings = true;
                        break;
                    }
                }
            }
        }
        String ostr = this.getIntrestedVariableString();
        Logger.printI(String.format("[%s] [Next Ins]: %s (%s)", this.hashCode(), currentInstruction,
                containsIntrestedThings ? "Y" : "N"));
        if (!containsIntrestedThings) {
            return newBc;
        }
        this.clear();
        // 调用JAssignStmt的apply(),调用StmtSwitch的caseAssignStmt，该类实现caseAssignStmt接口
        stmt.apply(this);
        newBc.addAll(this.retrieve());
        this.clear();
        String nstr = this.getIntrestedVariableString();
        Logger.printI(String.format("                 %s -> %s ", ostr, nstr));
        return newBc;
    }

    public List<BackwardContext> oneStepBackWard2Caller() {
        List<BackwardContext> newBc = new ArrayList<>();
        List<StmtPoint> sps = StmtPoint.findCaller(this.getCurrentMethod().toString(), Collections.singletonList(-1),
                this.getField(), true);
        if (sps.isEmpty())
            sps = StmtPoint.findCaller(this.getCurrentMethod().toString(), Collections.singletonList(-1),
                    this.getField(), false);
        if (sps.size() <= 0) {
            Logger.printW(String.format("[%s] [No Caller]: %s ", this.hashCode(), this.getCurrentMethod().toString()));
            finished = true;
            return newBc;
        }
        // 此处只追溯一个
        sps = sps.subList(0, 1);
        if (this.getCurrentMethod().equals(sps.get(0).getMethod_location())) {
            Logger.printI("this is loop");
            finished = true;
            return newBc;
        }
        this.countDepth();
        int len = sps.size();
        for (int i = 1; i < len; i++) {
            newBc.add(0, this.clone());
        }
        newBc.add(0, this);

        BackwardContext tmpBC;
        StmtPoint tmpSP;
        for (int i = 0; i < len; i++) {
            tmpBC = newBc.get(i);
            tmpSP = sps.get(i);

            tmpBC.oneStepBackWard2Caller(tmpSP);
        }
        newBc.remove(0);

        return newBc;
    }

    public void oneStepBackWard2Caller(StmtPoint tmpSP) {
        this.setCurrentMethod(tmpSP.getMethod_location());
        this.setCurrentBlock(tmpSP.getBlock_location());
        this.setCurrentInstruction(tmpSP.getInstruction_location());

        String ostr = this.getIntrestedVariableString();
        Logger.printI(String.format("[%s] [Next Ins]: %s (caller:%s)", this.hashCode(), this.getCurrentInstruction(),
                this.getCurrentMethod()));
        HashMap<Integer, Value> regs = new HashMap<Integer, Value>();
        for (Value var : this.getIntrestedVariable()) {
            regs.put(((ParameterRef) var).getIndex(), var);
        }
        this.getIntrestedVariable().clear();

        InvokeExpr inve = ((Stmt) tmpSP.getInstruction_location()).getInvokeExpr();
        ParameterTransferStmt tmp;
        for (int j : regs.keySet()) {
            tmp = new ParameterTransferStmt(regs.get(j), inve.getArg(j));
            this.getExecTrace().add(0, tmp);
            if (inve.getArg(j) instanceof Constant) {
                // do not have to taint
                GlobalStatistics.getInstance().countConstantParameterTrace();
            } else {
                this.addIntrestedVariable(inve.getArg(j));
            }
        }
        String nstr = this.getIntrestedVariableString();
        Logger.printI(String.format("                 %s -> %s ", ostr, nstr));
    }

    public void getBackFromACall() {
        CallStackItem citem = this.getCallStack().pop();
        Stmt retStmt = (Stmt) citem.getCurrentInstruction();
        Value opsite;
        this.countDepth();
        for (Value param : this.getCurrentMethod().getActiveBody().getParameterRefs()) {
            if (this.getIntrestedVariable().contains(param)) {
                // 获得当前的参数，如果该参数在之前的兴趣列表里
                opsite = retStmt.getInvokeExpr().getArg(((ParameterRef) param).getIndex());

                this.removeIntrestedVariable(param);
                // 该函数拥有我们的目的参数
                if (opsite instanceof Local) {
                    this.addIntrestedVariable(opsite);
                } else if (opsite instanceof Constant) {
                    GlobalStatistics.getInstance().countConstantParameterTrace();
                }
                ParameterTransferStmt tmp = new ParameterTransferStmt(param, opsite);
                this.getExecTrace().add(0, tmp);
            }
        }

        this.setCurrentMethod(citem.getSmethd());
        // Logger.print(this.hashCode() + "back to " + citem.getSmethd());
        this.setCurrentBlock(citem.getBlcok());
        this.setCurrentInstruction(citem.getCurrentInstruction());

    }

    public ValuePoint getStartPoint() {
        return startPoint;
    }

    public DGraph getDg() {
        return dg;
    }

    public int getDepth() {
        return this.depth;
    }

    public void countDepth() {
        this.depth++;
    }

    public SootMethod getCurrentMethod() {
        return getMethodes().get(0);
    }

    public void setCurrentMethod(SootMethod currentMethod) {
        this.getMethodes().add(0, currentMethod);
    }

    public Block getCurrentBlock() {
        return getBlockes().get(0);
    }

    public void setCurrentBlock(Block currentBlock) {
        getBlockes().add(0, currentBlock);
    }

    public ArrayList<SootMethod> getMethodes() {
        return methodes;
    }

    public ArrayList<Block> getBlockes() {
        return blockes;
    }

    public Unit getCurrentInstruction() {
        return currentInstruction;
    }

    public HashSet<SootField> getField() {
        return field;
    }

    public void setCurrentInstruction(Unit currentInstruction) {
        this.currentInstruction = currentInstruction;
    }

    public String getIntrestedVariableString() {
        String ostr = "";
        for (Value var : this.getIntrestedVariable()) {
            ostr += var + ",";
        }
        return ostr;
    }

    public HashSet<Value> getIntrestedVariable() {
        return intrestedVariable;
    }

    public void addIntrestedVariable(Value v) {
        if (v.getType().toString().equals("android.graphics.Bitmap") || v.getType().toString().equals("android.content.res.AssetManager")
                || v.getType().toString().equals("android.content.res.Resources") || v.getType().toString().equals("android.app.Application") || v.getType().toString().equals("android.view.Display")) {
            // only for label terminal parameter
            ParameterTransferStmt tmp = new ParameterTransferStmt(v, v);
            this.getExecTrace().add(0, tmp);
            Logger.printW("This is termination value, no need to trace!");
        } else if (v.toString().equals("r0")) {
            processThisValue(v);
        } else {
            if (!(v.getType().toString().equals("android.content.Context")) && !InstanceUtility.isType(InstanceUtility.getSootClassByType(v.getType()), "Activity")) {
                intrestedVariable.add(v);
            }

        }

    }

    public void removeIntrestedVariable(Value v) {
        if (v instanceof InstanceFieldRef) {
            for (Value ins : intrestedVariable) {
                if (ins.toString().equals(v.toString())) {
                    intrestedVariable.remove(ins);
                    intrestedVariable.add(((InstanceFieldRef) ins).getBase());
                    return;
                }
            }
        } else
            intrestedVariable.remove(v);
    }

    public void addIntrestedVariableIfNotConstant(Value v) {
        if (v instanceof Constant) {
            GlobalStatistics.getInstance().countConstantParameterTrace();
        }
        if (v instanceof Local) {
            intrestedVariable.add(v);
        } else if (OtherUtility.isStrConstant(v)) {
        } else if (OtherUtility.isNumConstant(v)) {
        } else if (v instanceof NullConstant) {
            intrestedVariable.add(v);
            Logger.printI("Variable is null no need to taint ");
        } else {
            Logger.printW(String.format("[%s] [unknow addIntrestedVariableIfNotConstant] %s(%s)", this.hashCode(), v,
                    v.getClass()));
        }
    }

    public ArrayList<Stmt> getExecTrace() {
        return execTrace;
    }

    public ArrayList<Value> getFieldTraced() {
        return fieldTraced;
    }

    public ArrayList<String> getFieldTracedSelf() {
        return fieldTracedSelf;
    }

    public ArrayList<Block> getAnalyzedBlock() {
        return analysedBlock;
    }

    public void printExceTrace() {
        Logger.print("[Start]:" + this.getStartPoint().getInstruction_location());
        for (Stmt var : this.getExecTrace()) {
            Logger.print("             " + var);

        }
    }

    public HashSet<HeapObject> getDependentHeapObjects() {
        return dependentHeapObjects;
    }

    public Stack<CallStackItem> getCallStack() {
        return callStack;
    }


    public BackwardContext clone() {
        BackwardContext tmp = new BackwardContext(this);
        return tmp;
    }

    ////////////////////////////////////////////////////////
    //////////////////////// StmtSwitch/////////////////////

    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        // TODO Auto-generated method stub
        boolean leftisIntrested = this.getIntrestedVariable().contains(stmt.getLeftOp());
        this.removeIntrestedVariable(stmt.getLeftOp());
        this.getExecTrace().add(0, stmt);
        Value right = stmt.getRightOp();
        if (right instanceof InvokeExpr) {
            InvokeExpr tmp = (InvokeExpr) right;
            if (InstanceUtility.isCallField(tmp.getMethod())) {
                this.getExecTrace().remove(0);
                Value field = CallGraph.getCalledField(tmp.getMethod());
                BackwardInstance.putFieldSwitch(right, field);
                if (field != null) {
                    ParameterTransferStmt tmpField = new ParameterTransferStmt(stmt.getLeftOp(), field);
                    this.getExecTrace().add(0, tmpField);
                    traceField(this.getCurrentMethod(), field, stmt.getLeftOp(), stmt);
                }
            }
            // Static 要不要追踪？
            else if (tmp instanceof VirtualInvokeExpr && EnumUtility.isIncludeGet(tmp.getMethod().getSignature())) {
                if (tmp.getArg(0) instanceof StringConstant) {
                    String info = ((StringConstant) tmp.getArg(0)).toString();
                    GetIntentEnum getIntentEnum = EnumUtility.getGetIntentEnumType(tmp.getMethod().getSignature());
                    handleIntent(tmp, this.getCurrentMethod().getDeclaringClass(), info, getIntentEnum);
                } else {
                    Logger.printW("This intent call register");
                }

            } else if (tmp instanceof StaticInvokeExpr) {
                handleInvokeExpr(tmp);
            } else if (BorderClass.getBorderMethod().contains(tmp.getMethod())) {
                this.getExecTrace().remove(this.getCurrentInstruction());
                for (ValuePoint vp : dg.getAllvps()) {
                    if (((Stmt) vp.getInstruction_location()).containsInvokeExpr() &&
                            ((Stmt) vp.getInstruction_location()).getInvokeExpr().getMethod().getSignature().equals(tmp.getMethod().getSignature())) {
                        if (!vp.inited()) {

                            List<BackwardContext> bcs = vp.initIfHavenot();
                            // 这里只是简单的弄了一下了
//			this.getField().remove(sootField);
                            ArrayList<Stmt> tmpStmt = new ArrayList<>();
                            for (BackwardContext tmpB : bcs) {
                                // 试试将field放在最前面
                                tmpStmt.addAll(0, tmpB.getExecTrace());
                            }
                            if (this.getExecTrace().containsAll(tmpStmt)) {
                                this.getExecTrace().removeAll(tmpStmt);
                            }
                            this.getExecTrace().addAll(0, tmpStmt);
                            if ((this.getCurrentInstruction() instanceof AssignStmt) && (tmpStmt.get(tmpStmt.size() - 1) instanceof AssignStmt)) {
                                ParameterTransferStmt tmpP;
                                Value leftop = ((AssignStmt) this.getCurrentInstruction()).getLeftOp();
                                if (leftop instanceof CastExpr)
                                    leftop = ((CastExpr) leftop).getOp();
                                tmpP = new ParameterTransferStmt(leftop, ((AssignStmt) tmpStmt.get(tmpStmt.size() - 1)).getLeftOp());
                                this.getExecTrace().add(0, tmpP);
                            }
                            vp.setSolved(true);
                            break;
                        }
                    }
                }
            }
            //这里弄得应该是access那些函数
//            else if (!tmp.getMethod().getDeclaringClass().isInnerClass() && tmp.getMethod().getDeclaringClass().equals(this.getCurrentMethod().getDeclaringClass())) {
//                System.out.println("yes");
//                handleInvokeExpr(tmp);
//            }
            // 处理builder那里，但不知道会不会出错
            else if (tmp.getMethod().getDeclaringClass().isInnerClass()) {
                handleInvokeExpr(tmp);
            } else if (!diveIntoMethodCall(stmt.getLeftOp(), leftisIntrested, tmp)) {
                handleInvokeExpr(tmp);
            }
        } else if (right instanceof JNewExpr) {
        } else if (right instanceof NewArrayExpr) {
            NewArrayExpr arr = (NewArrayExpr) right;
            if (arr.getSize() instanceof Local) {
                GlobalStatistics.getInstance().countGlobalParameterTrace();
                this.addIntrestedVariable(arr.getSize());
            } else if (arr.getSize() instanceof Constant) {
                GlobalStatistics.getInstance().countConstantParameterTrace();
            }
        } else if (right instanceof FieldRef) {
            // To be concerned
            if (right instanceof InstanceFieldRef) {
                // 如果不是当前函数的field
                if (!((InstanceFieldRef) right).getBase().getType().equals(this.getCurrentMethod().getDeclaringClass().getType())) {
                    Value instance = ((FieldRef) right).getUseBoxes().get(0).getValue();
                    String fieldName = ((FieldRef) right).getField().getSignature();
                    if (fieldName.equals("<android.hardware.Camera$Size: int width>")
                            || fieldName.equals("<android.hardware.Camera$Size: int height>")) {
                    } else if (fieldName.equals("<android.graphics.Point: int x>")
                            || fieldName.equals("<android.graphics.Point: int y>")) {
                    } else {
                        if (instance.getType().equals(((InstanceFieldRef) right).getField().getDeclaringClass().getType()) && this.getCurrentMethod().getDeclaringClass().isInnerClass() && this.getCurrentMethod().getDeclaringClass().getOuterClass().equals(((FieldRef) right).getField().getDeclaringClass())) {
                            this.addIntrestedVariable(right);
                        } else {
//                        this.addIntrestedVariable(instance);
                            /*
                             * 这里待考虑，可能存在问题需要解决
                             * 上面一行是原本代码，下面全都是改过的
                             * 该处改的目的是为了不追踪对象，而是追踪特定的属性
                             */
                            Value newRight = CallGraph.getCalledField((FieldRef) right);
                            // 这里存在空的问题，需要解决
                            if (newRight != null) {
                                this.getExecTrace().remove(0);
                                BackwardInstance.putFieldSwitch(right, newRight);
                                ParameterTransferStmt tmpP;
                                tmpP = new ParameterTransferStmt(stmt.getLeftOp(), newRight);
                                this.getExecTrace().add(0, tmpP);
                            }
                        }
                        traceField(this.getCurrentMethod(), right, stmt.getLeftOp(), stmt);
                    }
                } else {
                    // inner field
                    if (!((InstanceFieldRef) right).getBase().toString().equals("r0")) {
                        this.getExecTrace().remove(0);
                        Value newRight = CallGraph.getCalledField((FieldRef) right);
                        BackwardInstance.putFieldSwitch(right, newRight);
                        ParameterTransferStmt tmpP;
                        tmpP = new ParameterTransferStmt(stmt.getLeftOp(), newRight);
                        this.getExecTrace().add(0, tmpP);
                    }
                    traceField(this.getCurrentMethod(), right, stmt.getLeftOp(), stmt);
                }
            }
        } else if (right instanceof JArrayRef) {
            this.getIntrestedVariable().add(((JArrayRef) right).getBase());
            if (((JArrayRef) right).getIndex() instanceof Local) {
                this.getIntrestedVariable().add(((JArrayRef) right).getIndex());
            } else if (((JArrayRef) right).getIndex() instanceof Constant) {
                GlobalStatistics.getInstance().countConstantParameterTrace();
            }
        } else if (right instanceof JimpleLocal) {
            this.getIntrestedVariable().add(right);
        } else if (right instanceof CastExpr) {
            if (((CastExpr) right).getOp() instanceof Local) {
                this.getIntrestedVariable().add(((CastExpr) right).getOp());
            } else if (((CastExpr) right).getOp() instanceof Constant) {
            }
        } else if (right instanceof AddExpr) {
            calculate(((AddExpr) right).getOp1(), ((AddExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof DivExpr) {
            calculate(((DivExpr) right).getOp1(), ((DivExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof MulExpr) {
            calculate(((MulExpr) right).getOp1(), ((MulExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof SubExpr) {
            calculate(((SubExpr) right).getOp1(), ((SubExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof ShlExpr) {
            calculate(((ShlExpr) right).getOp1(), ((ShlExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof RemExpr) {
            calculate(((RemExpr) right).getOp1(), ((RemExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof OrExpr) {
            calculate(((OrExpr) right).getOp1(), ((OrExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof AndExpr) {
            calculate(((AndExpr) right).getOp1(), ((AndExpr) right).getOp2(), stmt.getLeftOp());
        } else if (right instanceof LengthExpr) {
            this.getIntrestedVariable().add(((LengthExpr) right).getOp());
        } else if (right instanceof NegExpr) {
            this.getIntrestedVariable().add(((NegExpr) right).getOp());
        } else if (right instanceof Constant) {
        } else {
            Logger.printW(String.format("[%s] [Can't Handle caseAssignStmt->RightOp]: %s (%s)", this.hashCode(), stmt,
                    right.getClass()));
        }

    }

    public void calculate(Value op1, Value op2, Value left) {
        if (op1 instanceof Local) {
            this.getIntrestedVariable().add(op1);
        }
        if (op2 instanceof Local) {
            this.getIntrestedVariable().add(op2);
        }
        if (op1 instanceof Constant && op2 instanceof Constant) {
        } else {
        }
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        // TODO Auto-generated method stub
//		if (GlobalStatistics.getInstance().containsTotalTrace((Unit) stmt) && !this.getExecTrace().contains(stmt)) {
//			return;
//		}
        this.getExecTrace().add(0, stmt);
        if (InstanceUtility.isType(stmt.getInvokeExpr().getMethod().getDeclaringClass(), "Activity"))
            return;
        handleInvokeExpr(stmt.getInvokeExpr());
        // super.caseInvokeStmt(stmt);
    }

    public void handleInvokeExpr(InvokeExpr invokExp) {
        for (Value arg : invokExp.getArgs()) {
            GlobalStatistics.getInstance().countGlobalParameterTrace();
            if (arg instanceof Local) {
                this.addIntrestedVariable(arg);
            } else if (arg instanceof Constant) {
                GlobalStatistics.getInstance().countConstantParameterTrace();
            }
        }
        if (invokExp.getMethod().getSignature().equals("<android.content.ContextWrapper: java.io.File getFilesDir()>"))
            return;
        Value base = InstanceUtility.getBaseInvoke(invokExp);
        if (base != null && !Objects.equals(base.toString(), "r0"))
            this.addIntrestedVariable(base);

    }

    @Override
    public void caseIdentityStmt(IdentityStmt stmt) {
        // TODO Auto-generated method stub
        if (this.getIntrestedVariable().contains(stmt.getLeftOp())) {
            this.removeIntrestedVariable(stmt.getLeftOp());
//			if (GlobalStatistics.getInstance().containsTotalTrace((Unit) stmt) && !this.getExecTrace().contains(stmt)) {
//				return;
//			}
            this.getExecTrace().add(0, stmt);
            if (stmt.getRightOp() instanceof ParameterRef) {
                if (this.getCurrentMethod().getName().equals("onPreviewFrame")
                        || ((ParameterRef) stmt.getRightOp()).getType().toString().equals("byte[]")) {
                    ParameterTransferStmt tmp = new ParameterTransferStmt(stmt.getRightOp(), stmt.getRightOp());
                    this.getExecTrace().add(0, tmp);
                } else {
                    this.addIntrestedVariable(stmt.getRightOp());
                }
            } else {
                Logger.printW(String.format("[%s] [Can't Handle caseIdentityStmt->RightOpUnrecognized]: %s (%s)",
                        this.hashCode(), stmt, stmt.getLeftOp().getClass()));
            }
        } else {
            Logger.printW(String.format("[%s] [Can't Handle caseIdentityStmt->LeftOpNotIntrested]: %s (%s)",
                    this.hashCode(), stmt, stmt.getLeftOp().getClass()));
        }
    }

    @Override
    public void defaultCase(Object obj) {
        // TODO Auto-generated method stub
        Logger.printW(String.format("[%s] [Can't Handle]: %s (%s)", this.hashCode(), obj, obj.getClass()));
    }

    // 判断检测存在调用的函数，不满足以上条件时，在该条件下，是否存在return等返回，加入到第二次的回溯中
    public boolean diveIntoMethodCall(Value leftOp, boolean leftisIntrested, InvokeExpr ive) {
        GlobalStatistics.getInstance().countDiveIntoMethodCall();
        // 做检测，是java自带的函数，还是没有body的函数
        if (!ive.getMethod().getDeclaringClass().isApplicationClass()) {
            Logger.printW(String.format("[%s] [This isn't ApplicationClass ]: %s", this.hashCode(), ive));
            return false;
        }
        if (!ive.getMethod().isConcrete())
            return false;
        GlobalStatistics.getInstance().countReturnMethodTrace();
        CallStackItem citem = new CallStackItem(this.getCurrentMethod(), this.getCurrentBlock(),
                this.getCurrentInstruction(), leftOp);
        this.getCallStack().push(citem);
        GlobalStatistics.getInstance().updateMaxCallStack(this.getCallStack().size());

        CompleteBlockGraph cbg = BlockGenerator.getInstance().generate(ive.getMethod().retrieveActiveBody());
        // 该函数的所有出口点和每一个块的出口点判断，此处要的是return
        List<Block> tails = new ArrayList<>();
        for (Block block : cbg.getTails()) {
            if (block.getTail() instanceof ReturnStmt) {
                tails.add(block);
                // only one return
                break;
            }
        }
        if (tails.size() == 0) {
            Logger.printW(String.format("[%s] [All Tail not ReturnStmt]: %s (%s)", this.hashCode(),
                    this.getCurrentInstruction(), this.getCurrentInstruction().getClass()));
        }
        this.getExecTrace().remove(0);
        List<BackwardContext> bcs = new ArrayList<BackwardContext>();
        int len = tails.size();
        // Logger.print(this.hashCode() + "tails.size" + len);

        for (int i = 1; i < len; i++) {
            bcs.add(this.clone());
        }
        bcs.add(0, this);

        BackwardContext tbc;
        Block tblock;
        Stmt rets = null;
        ParameterTransferStmt tmp;
        for (int i = 0; i < len; i++) {
            tbc = bcs.get(i);
            tblock = tails.get(i);
            rets = (Stmt) tblock.getTail();
            if (leftOp != null && leftisIntrested) {

                if (!(tblock.getTail() instanceof ReturnStmt)) {
                    Logger.printW(String.format("[%s] [Tail not ReturnStmt]: %s (%s)", this.hashCode(),
                            tblock.getTail(), tblock.getTail().getClass()));
                }

                tmp = new ParameterTransferStmt(leftOp, ((ReturnStmt) rets).getOp());
                tbc.getExecTrace().add(0, tmp);

                tbc.addIntrestedVariableIfNotConstant(((ReturnStmt) rets).getOp());// ??
                // parameter
            }

            tbc.setCurrentMethod(ive.getMethod());
            tbc.setCurrentBlock(tblock);
            tbc.setCurrentInstruction(rets);
        }
        BackwardContext b = bcs.get(0);
        bcs.remove(0);

        bcs.forEach(bc -> {
            this.put(bc);
        });
        bcs.clear();

        return true;
    }

    ////////////////////////////////////////////////////////
    //////////////////////// StmtPath //////////////////////
    @Override
    public Unit getStmtPathHeader() {
        // TODO Auto-generated method stub
        return this.getExecTrace().get(0);
    }

    @Override
    public Unit getSuccsinStmtPath(Unit u) {
        // TODO Auto-generated method stub
        if (u == null)
            return null;
        Unit told = null;
        for (Stmt tnew : this.getExecTrace()) {
            if (u == told) {
                return tnew;
            }
            told = tnew;
        }

        return null;
    }

    @Override
    public Unit getPredsinStmtPath(Unit u) {
        // TODO Auto-generated method stub
        if (u == null)
            return null;
        Unit told = null;
        for (Stmt tnew : this.getExecTrace()) {
            if (u == tnew) {
                return told;
            }
            told = tnew;
        }

        return null;
    }

    @Override
    public Unit getStmtPathTail() {
        // TODO Auto-generated method stub
        return this.getExecTrace().get(this.getExecTrace().size() - 1);
    }

    @Override
    public List<Stmt> getStmtPath() {
        return this.getExecTrace();
    }

    ////////////////////////////////////////////////////////
    //////////////////////// ICollecter ////////////////////
    List<BackwardContext> newGeneratedContext = new ArrayList<BackwardContext>();

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        newGeneratedContext.clear();
    }

    @Override
    public void put(BackwardContext bc) {
        newGeneratedContext.add(bc);
    }

    @Override
    public List<BackwardContext> retrieve() {
        // TODO Auto-generated method stub
        return newGeneratedContext;
    }

    ////////////////////////////////////////////////////////

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        for (SootMethod sm : methodes) {
            result.append("methodes", sm.toString());
        }
        for (Block blk : blockes) {
            result.append("blockes", blk.hashCode());
        }
        for (Stmt stmt : execTrace) {
            result.append("execTrace", stmt.toString());
        }

        JSONObject execTraceDetails = new JSONObject();
        HashSet<ValueBox> boxes = new HashSet<ValueBox>();
        for (Stmt stmt : execTrace) {
            boxes.addAll(stmt.getUseAndDefBoxes());
        }
        JSONObject tmp;
        for (ValueBox vb : boxes) {
            tmp = new JSONObject();
            tmp.put("class", vb.getValue().getClass().getSimpleName());
            tmp.put("str", vb.getValue().toString());
            tmp.put("hashCode", vb.getValue().hashCode() + "");

            execTraceDetails.put(vb.getValue().hashCode() + "", tmp);
        }
        result.put("ValueBoxes", execTraceDetails);

        return result;
    }

    private void doInnerField(SootMethod smthd, Stmt stmt) {
        if (!InstanceUtility.isCallField(smthd))
            return;
        if (stmt instanceof AssignStmt) {
            Value left = ((AssignStmt) stmt).getLeftOp();
            if (left instanceof InstanceFieldRef) {
                Value tmpLeft = CallGraph.getCalledField(smthd);
                ParameterTransferStmt tmpField = new ParameterTransferStmt(tmpLeft, left);
                this.getExecTrace().add(0, tmpField);

            }
        }
    }

    public void handleIntent(InvokeExpr invokeExpr, SootClass cls, String str, GetIntentEnum getIntentEnum) {
        IntentObject io = IntentObject.getInstance(dg, cls, str, getIntentEnum);
        List<BackwardContext> bcs = io.initIfHavenot();
        ArrayList<Stmt> tmpStmt = new ArrayList<>();
        for (BackwardContext tmp : bcs) {
            // 试试将field放在最前面
            tmpStmt.addAll(0, tmp.getExecTrace());
        }
        if (this.getExecTrace().containsAll(tmpStmt)) {
            this.getExecTrace().removeAll(tmpStmt);
        }
        Value left = ((AssignStmt) this.getExecTrace().get(0)).getLeftOp();
        this.getExecTrace().remove(0);
        if (tmpStmt.size() == 0) {
            if (invokeExpr.getArgs().size() > 1) {
                Value right = invokeExpr.getArg(1);
                ParameterTransferStmt tmp = new ParameterTransferStmt(left, right);
                tmpStmt.add(tmp);
            } else
                System.out.println("This intent can't trace!");
        } else {
            int last = tmpStmt.size() - 1;
            Value right = tmpStmt.get(last).getInvokeExpr().getArg(1);
            tmpStmt.remove(last);
            ParameterTransferStmt tmp = new ParameterTransferStmt(left, right);
            tmpStmt.add(tmp);
        }
        this.getExecTrace().addAll(0, tmpStmt);
    }

    public void traceField(SootMethod sootMethod, Value value, Value left, AssignStmt assignStmt) {
        SootField field = ((FieldRef) value).getField();
        System.out.println("start field" + field);
        SootClass cls = field.getDeclaringClass();
        SootField sootField = ((FieldRef) value).getField();
        if (cls.isApplicationClass()) {
            if (BackwardInstance.getTracedField().containsKey(sootField)) {
                ArrayList<Stmt> tmpStmt = BackwardInstance.getTracedField().get(sootField);
                if (this.getExecTrace().containsAll(tmpStmt)) {
                    this.getExecTrace().removeAll(tmpStmt);
                }
                this.getExecTrace().addAll(0, tmpStmt);
                return;
            }
            if (this.getField() != null && this.getField().contains(sootField)) {
                return;
            }
            this.getField().add(sootField);
            HeapObject ho = HeapObject.getInstance(dg, sootField, sootMethod, this.getField(), assignStmt);
            List<BackwardContext> bcs = ho.initIfHavenot();

            // 这里只是简单的弄了一下了
//			this.getField().remove(sootField);
            ArrayList<Stmt> tmpStmt = new ArrayList<>();
            for (BackwardContext tmp : bcs) {
                // 试试将field放在最前面
                tmpStmt.addAll(0, tmp.getExecTrace());
            }
            if (this.getExecTrace().containsAll(tmpStmt)) {
                this.getExecTrace().removeAll(tmpStmt);
            }
            Value newRight = CallGraph.getCalledField((FieldRef) value);
            if (tmpStmt.size() == 0) {
                NullConstant nullConstant = NullConstant.v();
                ParameterTransferStmt tmpField = new ParameterTransferStmt(value, nullConstant);
                tmpStmt.add(tmpField);
            } else if (newRight == null) {
                Stmt lastStmt = tmpStmt.get(tmpStmt.size() - 1);
                if (lastStmt instanceof AssignStmt) {
                    newRight = ((AssignStmt) lastStmt).getLeftOp();
//                    tmpStmt.remove(tmpStmt.size()-1);
                    BackwardInstance.putFieldSwitch(value, newRight);
                    ParameterTransferStmt tmpP;
                    tmpP = new ParameterTransferStmt(value, newRight);
                    tmpStmt.add(tmpP);
                }
            }
            this.getExecTrace().addAll(0, tmpStmt);
            BackwardInstance.getTracedField().put(sootField, tmpStmt);
        }
    }

    private void processStmt(Stmt stmt) {
        Value tmp;
        for (int index : startPoint.getTargetRgsIndexes()) {
            boolean isBorder = false;
            if (index == -1) {// set heap object
                // index == -1 指代的是那种field的例子
                // 例如 r0.<com.example.vsa.valuesetanalysisexample.VsaTest: java.lang.String
                // keypart1> = $r2，所以取右边的值，这里是赋值的关系
                tmp = ((JAssignStmt) currentInstruction).getRightOp();
            } else {
                // 否则获取当前参数位置的参数
                tmp = stmt.getInvokeExpr().getArg(index);
            }
            if (BorderClass.getBorderMethodStmt().contains(stmt)) {
                isBorder = true;
                GlobalStatistics.getInstance().countBorderParameterTrace();
                GlobalStatistics.getInstance().countGlobalParameterTrace();
            }
            if (tmp instanceof JimpleLocal) {
                String type = tmp.getType().toString();
                if (!type.equals("android.app.Activity")
                        && !type.equals("android.content.Context")) {
                    this.addIntrestedVariable(tmp);
                    if (isBorder) {
                        if (type.equals("android.hardware.Camera$Size") || type.equals("android.content.res.Resources") || type.equals("android.content.res.AssetManager")
                                || type.equals("android.graphics.Bitmap") || type.equals("android.graphics.Point") || type.equals("android.view.Display") || type.equals("android.app.Application") || type.equals("android.view.View")) {
                            GlobalStatistics.getInstance().countBorderTerminationParameterTrace();
                        } else {
                            GlobalStatistics.getInstance().countBorderReturnParameterTrace();
                        }
                    }
                } else {
                    if (isBorder) {
                        GlobalStatistics.getInstance().countBorderTerminationParameterTrace();
                    }
                }
            } else if (tmp instanceof Constant) {
                if (isBorder) {
                    GlobalStatistics.getInstance().countBorderConstantParameterTrace();
                    GlobalStatistics.getInstance().countConstantParameterTrace();

                }
            } else {
                Logger.printW("Target Variable is" + tmp.getClass() + " " + currentInstruction);
            }
        }
        // 全权之计
        if (backWardHasFinished() && stmt instanceof AssignStmt) {
            Value left = ((AssignStmt) stmt).getLeftOp();
            Value right = ((AssignStmt) stmt).getRightOp();
            if (left instanceof InstanceFieldRef) {
                this.getExecTrace().remove(0);
                Value tmpLeft = CallGraph.getCalledField((FieldRef) left);
                ParameterTransferStmt tmpField = new ParameterTransferStmt(tmpLeft, right);
                this.getExecTrace().add(0, tmpField);
            }
        }
    }

    private void processThisValue(Value v) {
        if (BackwardInstance.getThisInstance().containsKey(v.getType())) {
            ArrayList<Stmt> tmpStmt = BackwardInstance.getThisInstance().get(v.getType());
            if (this.getExecTrace().containsAll(tmpStmt)) {
                this.getExecTrace().removeAll(tmpStmt);
            }
            this.getExecTrace().addAll(0, tmpStmt);
            return;
        }
        SootClass cls = Scene.v().getSootClass(v.getType().toString());
        if (InstanceUtility.isType(cls, "Activity")) {
            System.out.println("this is instance");
        } else if (OrderMethod.getViewRec().containsKey(cls)) {
            System.out.println("this is view instance");
        } else {
            // 优先参数最少的初始函数
            Hashtable<Integer, ArrayList<SootMethod>> tmpArgs = new Hashtable<>();
            for (SootMethod smthd : cls.getMethods()) {
                if (smthd.isConstructor()) {
                    int args = smthd.getParameterCount();
                    if (!tmpArgs.containsKey(args))
                        tmpArgs.put(args, new ArrayList<>());
                    tmpArgs.get(args).add(smthd);
                }
            }
            ArrayList<SootMethod> sortedArgs = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                if (tmpArgs.containsKey(i)) {
                    sortedArgs.addAll(tmpArgs.get(i));
                    tmpArgs.remove(i);
                }
                if (tmpArgs.keySet().isEmpty())
                    break;
            }
//            if (v.getType().toString().equals("com.daon.sdk.face.DaonFace"))
//                System.out.println();
            boolean needBreak = false;
            for (SootMethod smthd : sortedArgs) {
//                if (smthd.isConstructor()) {
                if (needBreak)
                    break;
                List<Integer> index = new ArrayList<>();
                for (int i = 0; i < smthd.getParameterCount(); i++) {
                    GlobalStatistics.getInstance().countGlobalParameterTrace();
                    index.add(i);
                }
                List<StmtPoint> sps = StmtPoint.findCaller(smthd.toString(), index, this.getField(), true);
                if (sps.isEmpty())
                    sps = StmtPoint.findCaller(smthd.toString(), index, this.getField(), false);
                for (StmtPoint sp : sps) {
                    // 对每一组函数调用，new一个ValuePoint，添加到vps后面处理
                    ArrayList<Stmt> tmpStmt = new ArrayList<>();
                    if (((InvokeStmt) sp.getInstruction_location()).getInvokeExpr() instanceof SpecialInvokeExpr) {
                        ParameterTransferStmt tmp = new ParameterTransferStmt(v,
                                ((SpecialInvokeExpr) ((InvokeStmt) sp.getInstruction_location()).getInvokeExpr())
                                        .getBase());
                        tmpStmt.add(0, tmp);
                    }
                    ValuePoint vp = new ValuePoint(dg, sp.getMethod_location(), sp.getBlock_location(),
                            sp.getInstruction_location(), sp.getRegIndex(), this.getField());
                    InvokeExpr expr = ((InvokeStmt) sp.getInstruction_location()).getInvokeExpr();
                    if (expr instanceof SpecialInvokeExpr) {
                        if (((SpecialInvokeExpr) expr).getBase().equals(v))
                            break;
                    }
                    BackwardInstance.getThisInstance().put(v.getType(), tmpStmt);
                    List<BackwardContext> bcs = vp.initIfHavenot();
                    for (BackwardContext tmp : bcs) {
                        // 试试将field放在最前面
                        tmpStmt.addAll(0, tmp.getExecTrace());
                    }
                    if (this.getExecTrace().containsAll(tmpStmt)) {
                        this.getExecTrace().removeAll(tmpStmt);
                    }
                    this.getExecTrace().addAll(0, tmpStmt);
                    needBreak = true;
                    break;
                }
            }
//            }
        }
    }
}
