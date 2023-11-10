package base;

import graph.CallGraph;
import graph.GetIntentEnum;
import graph.PutIntentEnum;
import order.OrderMethod;
import order.SortOrder;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import utility.BlockGenerator;
import utility.BlockUtility;
import utility.EnumUtility;
import utility.InstanceUtility;
import utility.Logger;
import utility.MethodUtility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class StmtPoint {
    SootMethod method_location;
    Block block_location;
    Unit instruction_location;
    HashSet<SootField> field;
    List<Integer> regIndex;

    public StmtPoint(SootMethod method_location, Block block_location, Unit instruction_location,
                     List<Integer> regIndex, HashSet<SootField> field) {
        super();
        this.method_location = method_location;
        this.block_location = block_location;
        this.instruction_location = instruction_location;
        this.regIndex = regIndex;
        this.field = field;
    }

    public SootMethod getMethod_location() {
        return method_location;
    }

    public void setMethod_location(SootMethod method_location) {
        this.method_location = method_location;
    }

    public Block getBlock_location() {
        return block_location;
    }

    public void setBlock_location(Block block_location) {
        this.block_location = block_location;
    }

    public Unit getInstruction_location() {
        return instruction_location;
    }

    public HashSet<SootField> getField() {
        return field;
    }

    public void setInstruction_location(Unit instruction_location) {
        this.instruction_location = instruction_location;
    }

    public void setField(HashSet<SootField> field) {
        this.field = field;
    }

    public List<Integer> getRegIndex() {
        return regIndex;
    }

    public void SetRegIndex(List<Integer> regIndex) {
        this.regIndex = regIndex;
    }

    public static List<StmtPoint> findCaller(String signature, List<Integer> regIndex, HashSet<SootField> field,
                                             boolean isCalled) {
        SootMethod sm = Scene.v().getMethod(signature);
        /*
         * try { sm = Scene.v().getMethod(signature); }catch (Exception e) { return
         * null; }
         */
        HashSet<SootMethod> ms = new HashSet<SootMethod>();
        ms.add(sm);
        // sm����sink����
        // ����<init>����<cinit>
        if (sm.getName().charAt(0) != '<') {
            // sm.getSubSignature()�Ǻ�벿�֣�sm.getDeclaringClass()�������������������Ŀ�ĺ����Ķ����λ�á�
            MethodUtility.findAllPointerOfThisMethod(ms, sm.getSubSignature(), sm.getDeclaringClass());
        }

        List<StmtPoint> sps = new ArrayList<StmtPoint>();
        CallGraph.CallGraphNode node;
        CompleteBlockGraph cbg;
        Block block;
        for (SootMethod tmpm : ms) {
            node = CallGraph.getNode(tmpm.toString());
            try {
                // ��ȡ��ǰ���������б�����λ��
                ArrayList<SootMethod> smthds = new ArrayList<>();
                for (CallGraph.CallGraphNode bn : node.getCallBy()) {
                    smthds.add(bn.getSmthd());
                }
                SortOrder.sortMethods(smthds, true);
                for (SootMethod find : smthds) {
                    if (isCalled && !OrderMethod.isCalled(find))
                        continue;
//					if (!bn.getSmthd().isConcrete() && bn.getSmthd().getDeclaringClass().isApplicationClass()) {
//						sps.addAll(StmtPoint.findCaller(bn.getSmthd().toString(), Collections.singletonList(-1)));
//					}
                    PatchingChain<Unit> us = find.retrieveActiveBody().getUnits();
                    for (Unit unit : us) {
                        if (unit instanceof Stmt) {
                            if (((Stmt) unit).containsInvokeExpr()) {
                                // �ҵ��˵��øú����ĵط�
                                if (((Stmt) unit).getInvokeExpr().getMethod() == node.getSmthd()) {
                                    cbg = BlockGenerator.getInstance().generate(find.retrieveActiveBody());
                                    block = BlockUtility.findLocatedBlock(cbg, unit);
                                    sps.add(new StmtPoint(find, block, unit, regIndex, field));
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                continue;
            }
        }
        return sps;
    }

    /**
     * @param isCalled:�Ƿ���е����ж�
     * @param isOrder:�Ƿ����˳���ж�
     */
    public static List<StmtPoint> findFieldSetter(SootField sootField, SootMethod sootMethod, HashSet<SootField> field, AssignStmt assignStmt,
                                                  boolean isCalled, boolean isOrder) {
        if(sootField.getSignature().equals("<com.cuanativeandroidrdc.mobile.activity.FragAuthFaceIDCapture: com.daon.sdk.face.DaonFace facesdk>"))
            System.out.println();
        List<StmtPoint> sps = new ArrayList<StmtPoint>();
        ArrayList<SootMethod> mthdes = CallGraph.getFieldSetter(sootField);
        CompleteBlockGraph cbg;
        Block block;
        // ��ֹһ��ʵ����������
        ArrayList<String> fieldNewInstance = new ArrayList<>();
        if (mthdes != null) {
            ArrayList<SootMethod> result = SortOrder.sortMethods(mthdes, false);
            // sometimes, the orders may not correct, and the mthdes will be set to empty
            if(result.isEmpty())
                result.addAll(mthdes);
            for (SootMethod mthd : mthdes) {
//                System.out.println(mthd + "***" + sootMethod);
//                if(mthd.getSignature().equals("<com.daon.dmds.utils.face.FaceExtractorImpl: void <init>(android.content.Context,java.lang.String)>"))
//                    System.out.println();
//                System.out.println(OrderMethod.isCalled(mthd));
//                System.out.println(SortOrder.getNumber(mthd));
//                System.out.println(SortOrder.getNumber(sootMethod));
                if (isCalled && !OrderMethod.isCalled(mthd))
                    continue;
                // (֮ǰ�еȺţ�����ȥ��  9.20)
                if (isOrder && SortOrder.getNumber(mthd) > SortOrder.getNumber(sootMethod)) {
                    continue;
                }
                PatchingChain<Unit> us = mthd.retrieveActiveBody().getUnits();
                for (Unit unit : us) {
                    if (unit instanceof Stmt) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt instanceof AssignStmt) {
                            if (((AssignStmt) stmt).equals(assignStmt))
                                return sps;
                        }
                        for (ValueBox vbox : stmt.getDefBoxes()) {
                            if (vbox.getValue() instanceof FieldRef)
                                if (vbox.getValue() instanceof FieldRef
                                        && ((FieldRef) vbox.getValue()).getField() == sootField) {
                                    cbg = BlockGenerator.getInstance().generate(mthd.retrieveActiveBody());
                                    block = BlockUtility.findLocatedBlock(cbg, unit);
                                    StmtPoint sp = new StmtPoint(mthd, block, unit, Collections.singletonList(-1),
                                            field);
                                    // ֻ����һ��field����
                                    if (!sps.contains(sp)) {
                                        if (!fieldNewInstance.contains(stmt.toString())) {
                                            fieldNewInstance.add(stmt.toString());
                                            sps.add(sp);
                                        }else{
                                            // Ϊ��field�ĵ���˳��
                                            sps.clear();
                                            sps.add(sp);
                                        }
                                    }
//									return sps;
                                }
                        }
                    }
                }
                // ����������������֣�ԭʼ���������汻ע��
//                for (Unit unit : us) {
//                    if (unit instanceof Stmt) {
//                        for (ValueBox vbox : ((Stmt) unit).getDefBoxes()) {
//                            if (vbox.getValue() instanceof FieldRef)
//                                if (vbox.getValue() instanceof FieldRef
//                                        && ((FieldRef) vbox.getValue()).getField() == sootField) {
//                                    cbg = BlockGenerator.getInstance().generate(mthd.retrieveActiveBody());
//                                    block = BlockUtility.findLocatedBlock(cbg, unit);
//                                    StmtPoint sp = new StmtPoint(mthd, block, unit, Collections.singletonList(-1),
//                                            field);
//                                    // ֻ����һ��field����
//                                    if (!sps.contains(sp))
//                                        sps.add(sp);
////									return sps;
//                                }
//                        }
//                    }
//                }
            }
        } else {
            Logger.printW("no fieldSetter " + sootField);
        }

        return sps;
    }

    public static List<StmtPoint> findIntentSetter(SootClass sootClass, String key, GetIntentEnum getIntentEnum) {

        List<StmtPoint> sps = new ArrayList<>();
        ArrayList<SootMethod> mthdes = new ArrayList<>();
        if (InstanceUtility.isType(sootClass, "Activity"))
            mthdes = CallGraph.getIntentSetter(sootClass);
        else if (InstanceUtility.isType(sootClass, "Fragment")) {
            for (SootClass cls : Objects.requireNonNull(CallGraph.getFragmentByActivity(sootClass)))
                mthdes.addAll(CallGraph.getIntentSetter(cls));
        }
        CompleteBlockGraph cbg;
        Block block;
        if (mthdes != null) {
            SortOrder.sortMethods(mthdes, false);
            for (SootMethod mthd : mthdes) {
                PatchingChain<Unit> us = mthd.retrieveActiveBody().getUnits();
                for (Unit unit : us) {
                    if (unit instanceof Stmt) {
                        Stmt stmt = (Stmt) unit;
                        if (stmt.containsInvokeExpr()) {
                            InvokeExpr invokeExpr = stmt.getInvokeExpr();
                            PutIntentEnum putIntentEnum = EnumUtility.getPutIntentEnumType(invokeExpr.getMethod().getSignature());
                            if (putIntentEnum != null && putIntentEnum.getValue() == getIntentEnum.getValue())
                                if (invokeExpr.getArg(0) instanceof StringConstant) {
                                    String info = ((StringConstant) invokeExpr.getArg(0)).toString();
                                    if (info.equals(key)) {
                                        cbg = BlockGenerator.getInstance().generate(mthd.retrieveActiveBody());
                                        block = BlockUtility.findLocatedBlock(cbg, unit);
                                        List<Integer> args = new ArrayList<>();
                                        args.add(1);
                                        StmtPoint sp = new StmtPoint(mthd, block, unit, args, null);
                                        if (!sps.contains(sp)) {
                                            sps.add(sp);
                                        }
                                    }
                                } else {
                                    Logger.printW("This intent call register, StmtPoint");
                                }
                        }
                    }
                }
            }
        } else {
            Logger.printW("no intentSetter " + sootClass);
        }

        return sps;
    }
}
