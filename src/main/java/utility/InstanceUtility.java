package utility;

import base.GlobalStatistics;
import main.AnalysisConfig;
import main.Native;
import order.OrderMethod;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.VoidType;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ClassConstant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

import java.util.Hashtable;

public class InstanceUtility {
    static EntryPointsUtility entryPointsUtility = new EntryPointsUtility();
    static AnalysisConfig config = AnalysisConfig.getInstance();

    public static SootClass getSootClass(String className) {
        if (Scene.v().containsClass(className)) {
            return Scene.v().getSootClass(className);
        } else {
            return null;
        }
    }

    public static SootClass getSootClassByType(Type classType) {
        return getSootClass(classType.toString());
    }

    public static SootClass getSootClassByValue(Value classValue) {
        return getSootClassByType(classValue.getType());
    }

    public static SootMethod getSootMethod(String methodName) {
        if (Scene.v().containsMethod(methodName)) {
            return Scene.v().getMethod(methodName);
        } else {
            return null;
        }
    }

    public static SootMethod getConstructor(SootClass cls) {
        for (SootMethod stmd : cls.getMethods()) {
            if (stmd.isConstructor())
                return stmd;
        }
        return null;
    }
    public static Value getBaseField(Stmt stmt){
        FieldRef fieldRef = stmt.getFieldRef();
        return getBaseField(fieldRef);
    }
    public static Value getBaseField(FieldRef fieldRef) {
        if (fieldRef instanceof InstanceFieldRef)
            return ((InstanceFieldRef) fieldRef).getBase();
        return null;
    }
    public static Value getBaseInvoke(Stmt stmt) {
        InvokeExpr invoke = stmt.getInvokeExpr();
        return getBaseInvoke(invoke);
    }

    public static Value getBaseInvoke(InvokeExpr invoke) {
        if (invoke instanceof VirtualInvokeExpr)
            return ((VirtualInvokeExpr) invoke).getBase();
        else if (invoke instanceof SpecialInvokeExpr)
            return ((SpecialInvokeExpr) invoke).getBase();
        else if (invoke instanceof InterfaceInvokeExpr)
            return ((InterfaceInvokeExpr) invoke).getBase();
        return null;
    }

    public static boolean isEmptyMethod(SootMethod call) {
        if (!call.isConcrete())
            return true;
        if (!call.getDeclaringClass().isAbstract())
            return false;
        Body body = call.retrieveActiveBody();
        Unit last = body.getUnits().getLast();
        Unit predOfLast = body.getUnits().getPredOf(last);
        return (last instanceof ReturnStmt || last instanceof ReturnVoidStmt) && predOfLast instanceof IdentityStmt;
    }

    public static boolean isCallField(SootMethod stmd) {
        Body body;
        try {
            SootClass cls = stmd.getDeclaringClass();
            if (!cls.isApplicationClass())
                return false;
            // 例如<org.adw.library.widgets.discreteseekbar.DiscreteSeekBar$CustomState: int a(org.adw.library.widgets.discreteseekbar.DiscreteSeekBar$CustomState)>
            boolean compare = stmd.getParameterType(0).equals(cls.getType());
            if (cls.isInnerClass()) {
                cls = cls.getOuterClass();
                if (!compare)
                    compare = stmd.getParameterType(0).equals(cls.getType());
            }
            try {
                body = stmd.retrieveActiveBody();
            } catch (Exception e) {
                return false;
            }
            FieldRef innerField = null;
            if (stmd.getParameterCount() > 0) {
                if (compare) {
                    if (stmd.getParameterCount() == 1) {
                        for (Unit unit : body.getUnits()) {
                            if (((Stmt) unit) instanceof AssignStmt) {
                                if (((AssignStmt) unit).getRightOp() instanceof FieldRef) {
                                    innerField = ((FieldRef) ((AssignStmt) unit).getRightOp());
                                    break;
                                }
                            }
                        }
                    } else if (stmd.getParameterCount() == 2 && stmd.getParameterType(1).equals(stmd.getReturnType())) {
                        for (Unit unit : body.getUnits()) {
                            if (((Stmt) unit) instanceof AssignStmt) {
                                if (((AssignStmt) unit).getLeftOp() instanceof FieldRef) {
                                    innerField = ((FieldRef) ((AssignStmt) unit).getLeftOp());
                                    break;
                                }
                            }
                        }
                    }

                    if (innerField != null) {
                        return true;
                    }
                }
            }
            return false;
//            if (stmd.getParameterCount() == 1 && stmd.getParameterType(0).equals(cls.getType())||stmd.getParameterCount() == 2 && stmd.getParameterType(0).equals(cls.getType()) && stmd.getParameterType(1).equals(stmd.getReturnType())) {
//                for (Unit unit : body.getUnits()) {
//                    if (((Stmt) unit) instanceof AssignStmt) {
//                        if (((AssignStmt) unit).getRightOp() instanceof FieldRef) {
//                            innerField = ((FieldRef) ((AssignStmt) unit).getRightOp());
//                            break;
//                        }
//                    }
//                }
//                if (innerField != null) {
//                    System.out.println(innerField+"8888888");
//                    return true;
//                }
//            }
//            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static SootClass isIntentCall(SootMethod smthd, SootMethod call, Unit unit) {
        if (call.getSignature()
                .equals("<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>")) {
            Value arg = ((Stmt) unit).getInvokeExpr().getArg(1);
            if (arg instanceof ClassConstant) {
                ClassConstant cc = (ClassConstant) arg;
                Type type = cc.toSootType();
                // 删掉这个有影响么？
//                if (!smthd.getDeclaringClass().equals(sc))
                return InstanceUtility.getSootClassByType(type);
            } else if (arg instanceof Local) {
                Hashtable<Integer, SootClass> inner = OrderMethod.getInner();
                return inner.getOrDefault(arg.hashCode(), null);
            }
        } else if (call.getSignature()
                .equals("<android.view.inputmethod.InputMethodManager: boolean showSoftInput(android.view.View,int)>") || call.getSignature()
                .equals("<android.view.inputmethod.InputMethodManager: boolean showSoftInput(android.view.View,int,android.os.ResultReceiver)>")) {
            return config.getSoftInput();
        }
        return null;
    }

    public static SootClass isViewCall(Unit unit) {
        if (unit instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) unit;
            if (as.getRightOp() instanceof CastExpr) {
                CastExpr castExpr = (CastExpr) as.getRightOp();
                Value cast = castExpr.getOp();
                Type type = castExpr.getCastType();
                SootClass cls = InstanceUtility.getSootClassByType(type);
                if (cast.getType().toString().equals("android.view.View") && cls != null)
                    return cls;
            }
        }
        return null;
    }

    public static boolean isClassInSystemPackage(String className) {
        return (className.startsWith("androidx.") || className.startsWith("android.support") || className.startsWith("java.")
                || className.startsWith("javax.") || className.startsWith("sun.") || className.startsWith("org.omg.")
                || className.startsWith("org.w3c.dom.") || className.startsWith("kotlin") || (className.startsWith("com.android.") && !className.startsWith("com.android.inputmethod.")) || className.startsWith("org.opencv."));
    }

    public static boolean isType(SootClass cls, String type) {
        if (cls == null)
            return false;
        return entryPointsUtility.getComponentType(cls).name().equals(type);
    }

    public static boolean isTypes(SootClass cls, String type1, String type2) {
        if (cls == null)
            return false;
        return entryPointsUtility.getComponentType(cls).name().equals(type1) || entryPointsUtility.getComponentType(cls).name().equals(type2);
    }

    public static boolean isAndroidxProcess(SootClass cls) {
        boolean hasComponentCallbacks = false;
        boolean OnCreateContextMenuListener = false;
        for (SootClass interfaceCls : cls.getInterfaces()) {
            if (interfaceCls.getName().equals("android.content.ComponentCallbacks"))
                hasComponentCallbacks = true;
            if (interfaceCls.getName().equals("android.view.View$OnCreateContextMenuListener"))
                OnCreateContextMenuListener = true;
        }
        SootClass superCls = cls.getSuperclass();
        if (hasComponentCallbacks && OnCreateContextMenuListener)
            return true;
        if (superCls != null && !superCls.toString().equals("java.lang.Object"))
            return isAndroidxProcess(superCls);
        return false;
    }

    public static boolean isImplicitParameter(SootClass cls) {
        if (cls != null) {
            if (cls.isApplicationClass() && cls.isInterface()) {
                for (SootMethod smthd : cls.getMethods()) {
                    if (!(smthd.getReturnType() instanceof VoidType)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    public static void isConfuscated(SootClass cls) {
        if (GlobalStatistics.getInstance().getisConfuscated() == 1)
            return;
        boolean flag = false;
        for (String nativeCls : Native.getNativeClasses()) {
            if (cls.getName().startsWith(OtherUtility.stringSplit(nativeCls, "\\.", 2))) {
                flag = true;
                break;
            }
        }
        if (!flag)
            return;
        if (cls.isApplicationClass() && !InstanceUtility.isClassInSystemPackage(cls.getPackageName()) && cls.getName().endsWith(".R")) {
            GlobalStatistics.getInstance().setisConfuscated();
        }
    }

    public static Type getListType(Stmt stmt) {
        if (stmt.containsInvokeExpr()) {
            SootMethod caller = stmt.getInvokeExpr().getMethod();
            if(caller.isAbstract()){
//                Value base = getBase(stmt);
//                SootClass sootClass = OrderMethod.getTypeFrom(base);
//                if(sootClass==null)
                    return null;
//                else
//                    return sootClass.getType();
            }
            Body body = caller.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                Stmt instruction = (Stmt) unit;
                if (instruction.containsInvokeExpr()) {
                    InvokeExpr invokeExpr = instruction.getInvokeExpr();
                    if (invokeExpr.getMethod().toString().equals("<java.util.Arrays: java.util.List asList(java.lang.Object[])>")) {
                        Value instr = invokeExpr.getArg(0);
                        if (instr.getType() instanceof ArrayType) {
                            return ((ArrayType) instr.getType()).baseType;
                        }
                    }
                    if (invokeExpr.getMethod().toString().equals("<android.util.SparseArray: void append(int,java.lang.Object)>")) {
                        Value instr = invokeExpr.getArg(1);
                        return instr.getType();
                    }
                }
            }

        }
        return null;
    }
}
