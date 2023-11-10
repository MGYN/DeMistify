package graph;

import main.AnalysisConfig;
import org.jf.util.ExceptionWithContext;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.android.SetupApplication;
import utility.FileUtility;
import utility.InstanceUtility;
import utility.ListUtility;
import utility.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class CallGraph {
    static AnalysisConfig config = AnalysisConfig.getInstance();
    private static final Hashtable<String, CallGraphNode> nodes = new Hashtable<>();
    private static final ArrayList<String> callRec = new ArrayList<>();
    private static final Hashtable<SootClass, HashSet<SootMethod>> constructor = new Hashtable<>();
    static Hashtable<String, ArrayList<SootMethod>> fieldSetters = new Hashtable<>();
    static Hashtable<SootClass, ArrayList<SootMethod>> intentSetters = new Hashtable<>();
    static Hashtable<String, HashSet<SootClass>> interfaceSetters = new Hashtable<>();
    static Hashtable<SootClass, HashSet<SootClass>> fragmentByActivity = new Hashtable<>();
    static Hashtable<String, HashSet<SootMethod>> fieldOrder = new Hashtable<>();
    private static final HashSet<FieldRef> fieldRef = new HashSet<>();
    private static final Hashtable<SootClass, HashSet<SootClass>> hasCall = new Hashtable<>();
    private static final Hashtable<SootClass, SootMethod> staticInitializer = new Hashtable<>();

    private static final Hashtable<Value, Value> fieldPassed = new Hashtable<>();
    private static final Hashtable<String, Integer> rawResource = new Hashtable<>();
    public static void init(SetupApplication analyzer) {
        long st = System.currentTimeMillis();
        CallGraphNode tmp;
        Value tv;
        FieldRef fr;
        String str;
        Body body;
        SootMethod call;
        for (SootClass sclas : Scene.v().getClasses()) {
            for (SootMethod smthd : sclas.getMethods()) {
                tmp = new CallGraphNode(smthd);
                nodes.put(smthd.toString(), tmp);
            }
        }
        Logger.printI("[Start CG]" + (System.currentTimeMillis() - st) / 1000 + "s");
        for (SootClass sclas : Scene.v().getClasses()) {
            for (SootMethod smthd : ListUtility.clone(sclas.getMethods())) {
                // 如果没有函数体
                if (smthd.isStaticInitializer())
                    staticInitializer.put(sclas, smthd);
                if (smthd.isConstructor()) {
                    if (!constructor.containsKey(sclas))
                        constructor.put(sclas, new HashSet<>());
                    constructor.get(sclas).add(smthd);
                }
                if (!smthd.isConcrete())
                    continue;
                try {
                    body = smthd.retrieveActiveBody();
                } catch (RuntimeException e) {
                    continue;
                }

                if (body == null)
                    continue;
                boolean containsRaw = false;
                int resourceId = 0;
//				String className = "";
                for (Unit unit : body.getUnits()) {
                    if (unit instanceof Stmt) {
                        boolean traceIntent = false;
                        SootClass intentCall;
                        Stmt stmt = (Stmt) unit;
                        if(stmt instanceof AssignStmt){
                            AssignStmt as = (AssignStmt) stmt;
                            Value leftOp = as.getLeftOp();
                            Value rightOp = as.getRightOp();
                            if(rightOp instanceof FieldRef){
                                fieldPassed.put(leftOp,rightOp);
                            }
                            if(rightOp instanceof Local && fieldPassed.containsKey(rightOp))
                            {
                                fieldPassed.put(leftOp,fieldPassed.get(rightOp));
                                fieldPassed.remove(rightOp);
                            }
                        }
                        if (stmt.containsInvokeExpr()) {
                            try {
                                call = stmt.getInvokeExpr().getMethod();
                                if(call.getSignature().equals("<android.content.res.Resources: java.io.InputStream openRawResource(int)>")) {
                                    containsRaw = true;
                                    Value arg = stmt.getInvokeExpr().getArg(0);
                                    resourceId = Integer.parseInt(arg.toString());
                                }
                                if(containsRaw && call.getSignature().equals("<java.io.FileOutputStream: void <init>(java.lang.String)>")) {
                                    Value arg = stmt.getInvokeExpr().getArg(0);
                                    if(fieldPassed.containsKey(arg) && fieldPassed.get(arg) instanceof FieldRef) {
                                        rawResource.put(((FieldRef) fieldPassed.get(arg)).getField().toString(), resourceId);
                                    }
                                }
                                // 判断是否是实现了回调函数，将回调函数对应，（如果调用的函数的类是系统函数）
                                int count = 0;
                                for (Type type : call.getParameterTypes()) {
                                    if (InterfaceGraph.isInCallBacks(type.toString())) {

                                        // 回调函数里传的参数，如果是回调函数类型
                                        SootClass cls = utility.InstanceUtility.getSootClass(
                                                stmt.getInvokeExpr().getArg(count).getType().toString());
                                        // 这里指的是不存在内部类，直接实现在原class里
                                        if (cls == null)
                                            cls = smthd.getDeclaringClass();
                                        if (InterfaceGraph.containsKey(cls)) {
                                            // 存在调用回调函数的例子，smthd是函数位置，call是回调函数，cls是参数里的回调函数的类
                                            doCallBack(smthd, call, cls);
                                        }
                                    } else if (type.toString().equals("java.lang.Runnable")) {
                                        SootClass cls = utility.InstanceUtility.getSootClass(
                                                stmt.getInvokeExpr().getArg(count).getType().toString());
                                        doThread(smthd, call, cls, "java.lang.Runnable");

                                    } else if (stmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                                        InvokeExpr invoke = stmt.getInvokeExpr();
                                        Value base = ((VirtualInvokeExpr) invoke).getBase();
                                        SootClass cls = utility.InstanceUtility.getSootClass(base.getType().toString());
                                        if (call.getSubSignature().equals("boolean sendMessage(android.os.Message)")
                                                || call.getSubSignature().equals("boolean sendEmptyMessage(int)")) {
                                            doHandle(smthd, call, cls);
                                        }
                                    }
                                    if(type.toString().equals("android.content.Intent"))
                                        traceIntent = false;
                                    count++;
                                }
                                SootClass smthdCls = smthd.getDeclaringClass().isInnerClass() ? smthd.getDeclaringClass().getOuterClass() : smthd.getDeclaringClass();
                                if (call.getSignature()
                                        .equals("<android.content.Intent: void <init>(android.content.Context,java.lang.Class)>")) {
                                    traceIntent = true;
                                    Value argContext = stmt.getInvokeExpr().getArg(1);
                                    ClassConstant cc = (ClassConstant) argContext;
                                    Type type = cc.toSootType();
                                    intentCall = utility.InstanceUtility.getSootClassByType(type);

                                    if (!smthd.getDeclaringClass().equals(intentCall)) {
                                        if (!intentSetters.containsKey(intentCall))
                                            intentSetters.put(intentCall, new ArrayList<>());
                                        intentSetters.get(intentCall).add(smthd);
//                                        // startActivity之类的
//                                        analyzer.getEntrypointClasses().remove(intentCall);
                                    }
                                }
                                SootClass callCls = call.getDeclaringClass();
                                // fragment调用之类的
                                if (call.isConstructor() && InstanceUtility.isType(callCls, "Fragment")) {
                                    if (!fragmentByActivity.containsKey(callCls))
                                        fragmentByActivity.put(callCls, new HashSet<>());
                                    if(callCls.equals(smthdCls))
                                        continue;
                                    if(fragmentByActivity.containsKey(smthdCls)&&fragmentByActivity.get(smthdCls).contains(callCls))
                                        continue;
                                    fragmentByActivity.get(callCls).add(smthdCls);
                                }
                                addCall(smthd, call);
                            } catch (Exception e) {
//								Logger.printW(e.getMessage());
                            }

                        }
                        for (ValueBox var : stmt.getUseAndDefBoxes()) {
                            tv = var.getValue();
                            if (tv instanceof InstanceFieldRef) {
                                InstanceFieldRef Ifr = (InstanceFieldRef) tv;
                                if (Ifr.getBase().toString().equals("r0") && !InstanceUtility.isCallField(smthd))
                                    fieldRef.add((FieldRef) tv);
                                else if (!Ifr.getBase().toString().equals("r0") && !InstanceUtility.isCallField(smthd)) {
                                    if(Ifr.getField().getDeclaringClass().isInnerClass()&&Ifr.getField().getDeclaringClass().equals(smthd.getDeclaringClass()))
                                        fieldRef.add((FieldRef) tv);
                                }
                            }
                        }
                        for (ValueBox var : stmt.getDefBoxes()) {
                            tv = var.getValue();
                            if (tv instanceof FieldRef) {
                                fr = (FieldRef) tv;
                                // 代码里定义的常量，并且被使用的，下文可以理解为是开发者定义的常量
                                if (fr.getField().getDeclaringClass().isApplicationClass()) {
                                    str = fr.getField().toString();
                                    if (!fieldSetters.containsKey(str)) {
                                        fieldSetters.put(str, new ArrayList<SootMethod>());
                                    }
                                    // 将常量和函数名对应起来
                                    if (fieldSetters.get(str).contains(smthd))
                                        continue;
                                    if (smthd.isConstructor()) {
                                        fieldSetters.get(str).add(0, smthd);
                                    } else {
                                        fieldSetters.get(str).add(smthd);
                                    }
//									Logger.print("FS:" + smthd + " " + str + " " + unit);
                                }
                            }
                        }
                    }
                }
            }
        }
        FileUtility.wf_s(config.getApkDir() + "_call.txt", callRec, false);
        Logger.printI("[CG time]:" + (System.currentTimeMillis() - st) / 1000 + "s");
    }

    private static void doCallBack(SootMethod meth, SootMethod call, SootClass callType) {

        HashSet<String> callBacks = InterfaceGraph.getCallBacks(callType);
        for (Type type : call.getParameterTypes()) {
            if (callBacks.contains(type.toString())) {
                for (SootMethod stmd : utility.InstanceUtility.getSootClass(type.toString()).getMethods()) {

                    String abstractMethod = stmd.getSignature();
                    String extendtMethod = abstractMethod.replace(stmd.getDeclaringClass().getName(),
                            callType.getName());
                    SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
                    if (extend != null) {
                        addCall(meth, extend);
//						System.out.println(meth + "-----" + call + "*****" + extend);
                    }
                }
            }

        }
    }

    private static void doThread(SootMethod meth, SootMethod call, SootClass callType, String str) {
        for (Type type : call.getParameterTypes()) {
            if (type.toString().equals(str)) {
                for (SootMethod stmd : utility.InstanceUtility.getSootClass(type.toString()).getMethods()) {

                    String abstractMethod = stmd.getSignature();
                    String extendtMethod = abstractMethod.replace(stmd.getDeclaringClass().getName(),
                            callType.getName());
                    SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
                    if (extend != null && extend.getDeclaringClass().isApplicationClass()) {
                        addCall(meth, extend);
//						System.out.println(meth + "-----" + call + "*****" + extend);
                    }
                }
            }

        }
    }

    private static void doHandle(SootMethod meth, SootMethod call, SootClass callType) {
        String subsignature = "void handleMessage(android.os.Message)";
        for (SootClass cls : Scene.v().getClasses()) {
            if (cls.isInnerClass() && cls.getOuterClassUnsafe().equals(meth.getDeclaringClass())
                    && cls.declaresMethod(subsignature)) {
                String extendtMethod = String.format("<%s: %s>", cls, subsignature);
                SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
                if (extend != null && extend.getDeclaringClass().isApplicationClass()) {
                    addCall(meth, extend);
                }
            }
        }

    }

    private static void doInterface(SootMethod stmd, SootClass cls) {
        if (InterfaceGraph.getInterfaceBy(cls) == null) {
            return;
        }
        for (SootClass c : InterfaceGraph.getInterfaceBy(cls)) {
            String abstractMethod = stmd.getSignature();
            String extendtMethod = abstractMethod.replace(stmd.getDeclaringClass().getName(), c.getName());
            SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
            if (extend != null) {
                addCall(stmd, extend);
            } else {
                doInterface(stmd, c);
            }
        }
    }
// 需要在orderMethod里添加他
    public static void addCall(SootMethod from, SootMethod to) {
        CallGraphNode fn, tn;
        fn = getNode(from);
        tn = getNode(to);
        if (fn == null || tn == null) {
//			System.out.println(from + "///////" + to);
            return;
        }
        if (!hasCall.containsKey(from.getDeclaringClass())) {
            hasCall.put(from.getDeclaringClass(), new HashSet<>());
        }
        hasCall.get(from.getDeclaringClass()).add(to.getDeclaringClass());
        callRec.add(from + " ----> " + to);
        fn.addCallTo(tn);
        tn.addCallBy(fn);
//		System.out.println(from + "-----------------" + to);
    }


    public static CallGraphNode getNode(SootMethod from) {
        return getNode(from.toString());
    }

    public static CallGraphNode getNode(String from) {
        return nodes.get(from);
    }


    public static ArrayList<SootMethod> getCallFrom(SootMethod meth) {
        ArrayList<SootMethod> func = new ArrayList<SootMethod>();
        HashSet<CallGraphNode> srcNode;
        try {
            srcNode = CallGraph.getNode(meth).getCallBy();
        } catch (NullPointerException e) {
            srcNode = new HashSet<>();
        }
        for (CallGraphNode node : srcNode) {
            SootMethod src = node.getSmthd();
            if (src.isJavaLibraryMethod()) {
                continue;
            } else {
                func.add(src);
            }
        }
        return func;
    }

    // 返回当前SootMethod的所有target边列表
    public static ArrayList<SootMethod> getCallTo(SootMethod meth) {
        ArrayList<SootMethod> func = new ArrayList<SootMethod>();
        HashSet<CallGraphNode> tgtNode;
        try {
            tgtNode = CallGraph.getNode(meth).getCallTo();
        } catch (NullPointerException e) {
            tgtNode = new HashSet<>();
        }
        for (CallGraphNode node : tgtNode) {
            SootMethod tgt = node.getSmthd();
            if (tgt.isJavaLibraryMethod()) {
                continue;
            } else {
                func.add(tgt);
            }
        }
        return func;
    }

    public static ArrayList<SootMethod> getFieldSetter(SootField sootField) {
        return fieldSetters.get(sootField.toString());
    }

    public static HashSet<SootClass> getFragmentByActivity(SootClass cls) {
        HashSet<SootClass> result = new HashSet<>();
        if (fragmentByActivity.get(cls) == null)
            return result;
        for (SootClass c : fragmentByActivity.get(cls)) {
            if (InstanceUtility.isType(c, "Activity"))
                result.add(c);
            else
                result.addAll(getFragmentByActivity(c));
        }
        return result;
    }

    public static ArrayList<SootMethod> getIntentSetter(SootClass cls) {
        return intentSetters.get(cls);
    }

    public static HashSet<SootMethod> getOrder(SootField sootField) {
        return fieldOrder.get(sootField.toString());
    }


    public static Hashtable<SootClass, SootMethod> getStaticInitializer() {
        return staticInitializer;
    }

    public static class CallGraphNode implements GraphNode<CallGraphNode> {
        SootMethod smthd;

        HashSet<CallGraphNode> callBy = new HashSet<>();
        HashSet<CallGraphNode> callTo = new HashSet<>();

        public CallGraphNode(SootMethod smthd) {
            this.smthd = smthd;
        }

        @Override
        public void addCallBy(CallGraphNode smtd) {
            callBy.add(smtd);
        }

        @Override
        public void addCallTo(CallGraphNode smtd) {
            callTo.add(smtd);
        }

        @Override
        public HashSet<CallGraphNode> getCallBy() {
            return callBy;
        }

        @Override
        public HashSet<CallGraphNode> getCallTo() {
            return callTo;
        }

        public SootMethod getSmthd() {
            return smthd;
        }

    }

    public static Value getCalledField(SootMethod stmd) {
        Body body = stmd.retrieveActiveBody();
        FieldRef innerField = null;
        for (Unit unit : body.getUnits()) {
            if (((Stmt) unit) instanceof AssignStmt) {
                if (((AssignStmt) unit).getRightOp() instanceof FieldRef) {
                    innerField = ((FieldRef) ((AssignStmt) unit).getRightOp());
                    break;
                } else if (((AssignStmt) unit).getLeftOp() instanceof FieldRef) {
                    innerField = ((FieldRef) ((AssignStmt) unit).getLeftOp());
                    break;
                }
            }
        }
        if (innerField != null) {
            return getCalledField(innerField);
        }
        return null;

    }

    public static Value getCalledField(FieldRef thisField) {
        for (FieldRef field : fieldRef) {
            if (field.getField().getSignature().equals(((FieldRef) thisField).getField().getSignature())) {
                return (Value) field;
            }
        }
        return null;
    }
    public static Integer getRawResourceIdIfHave(String thisField) {
        if(rawResource.containsKey(thisField))
            return rawResource.get(thisField);
        return 0;
    }
}
