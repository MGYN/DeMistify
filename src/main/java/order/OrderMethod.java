package order;

import base.GlobalStatistics;
import graph.CallGraph;
import graph.InterfaceGraph;
import main.AnalysisConfig;
import main.BorderClass;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.ClassConstant;
import soot.jimple.FieldRef;
import soot.jimple.IdentityStmt;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.NewExpr;
import soot.jimple.NullConstant;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.internal.JCastExpr;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.util.cfgcmd.CFGGraphType;
import utility.BlockGenerator;
import utility.BlockUtility;
import utility.EntryPointsUtility;
import utility.InstanceUtility;
import utility.Logger;
import utility.MethodUtility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;

public class OrderMethod {

    SetupApplication analyzer;

    private Hashtable<SootClass, SootMethod> staticInitializer;
    AnalysisConfig config = AnalysisConfig.getInstance();
    private EntryPointsUtility entryPointsUtility;

    private static ArrayList<SootMethod> solvedMethods = new ArrayList<>();

    private HashSet<Integer> solvedUnits = new HashSet<>();


    private static Hashtable<Integer, SootClass> inner = new Hashtable<>();
    private HashSet<SootMethod> interfaceMethod = new HashSet<>();
    private HashSet<SootClass> firstAppear = new HashSet<>();
    private HashSet<SootMethod> countOrder = new HashSet<>();
    private HashSet<SootMethod> lifeCycle = new HashSet<>();
    private Hashtable<SootMethod, ArrayList<SootMethod>> borderMethodCallTrace = new Hashtable<>();
    private static Hashtable<SootMethod, ArrayList<SootMethod>> recordproMethod = new Hashtable<>();
    private Hashtable<SootMethod, ArrayList<Integer>> callArgsInner = new Hashtable<>();
    private Hashtable<Integer, ArrayList<SootClass>> returnInner = new Hashtable<>();
    static Hashtable<String, SootClass> interfaceSetters = new Hashtable<>();
    // 接口或者抽象函数的实现导致无法正确对应return赋值关系
    private Hashtable<SootMethod, SootMethod> realInstance = new Hashtable<>();
    private static Hashtable<SootClass, SootClass> viewRec = new Hashtable<>();

    public OrderMethod(SetupApplication analyzer) {
        this.analyzer = analyzer;
    }

    public void init() {
        staticInitializer = CallGraph.getStaticInitializer();
        config = AnalysisConfig.getInstance();
        entryPointsUtility = new EntryPointsUtility();
//        for (SootMethod smthd : borderClass.getBorderMethod()) {
//            borderMethodCallTrace.put(smthd, new CallTrace(smthd, new ArrayList<>()));
//        }
    }

    public void getOrder(SootMethod smthd, SootMethod caller, Integer lastUnit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
//        CallGraph.addCall(caller, smthd);
        if (smthd.equals(caller))
            return;
        String name = smthd.getName().toLowerCase(Locale.ROOT);
        if (name.equals("close") || name.equals("destroy") || name.equals("delete") || name.equals("remove") || name.equals("flush") || name.equals("release"))
            return;
        if (InstanceUtility.isClassInSystemPackage(smthd.getDeclaringClass().getName())
                || !smthd.getDeclaringClass().isApplicationClass())
            return;
        doAndroidxDummyMain(smthd, callTrace, callBackDelays);
        extraDummyMain(smthd, callTrace, callBackDelays);
//        System.out.println(caller + "-----------------" + smthd);
        solvedMethods.add(smthd);
        // for count
        if (BorderClass.getBorderMethod().contains(smthd)) {
            countOrder.add(smthd);
        }
        Body body;
        if (!solvedUnits.contains(lastUnit)) {
            solvedUnits.add(lastUnit);
//            System.out.println("[" + solvedUnits.size() + "]" + caller + "-----------------" + smthd);
            processCallTrace(smthd, callTrace);
            if (smthd.isConstructor() && staticInitializer.get(smthd.getDeclaringClass()) != null)
                getOrder(staticInitializer.get(smthd.getDeclaringClass()), smthd, generateHash(staticInitializer.get(smthd.getDeclaringClass()), smthd, null), callTrace, callBackDelays);
            if (!smthd.isConcrete()) {
                return;
            }
            try {
                body = smthd.retrieveActiveBody();

            } catch (Exception e) {
                Logger.printE("Retrive" + e.getMessage() + smthd);
                return;
            }
            callTrace.add(smthd);
            CFGGraphType graphtype = CFGGraphType.getGraphType("BRIEFBLOCKGRAPH");
            DirectedGraph<Block> directedGraph = (DirectedGraph<Block>) graphtype.buildGraph(body);
            ArrayList<CallBackDelay> tmp = new ArrayList<>();
            if (!entryPointsUtility.isEntryPointMethod(smthd))
                tmp = callBackDelays;
            for (Block block : directedGraph.getHeads()) {
                try {
                    doAnalysis(directedGraph, block, smthd, caller, callTrace, new HashSet<>(), tmp, 0);
                } catch (StackOverflowError e) {
                    System.out.println("!!!!!!" + smthd);
                }
            }
            callTrace.remove(callTrace.lastIndexOf(smthd));
            if (entryPointsUtility.isEntryPointMethod(smthd)) {
//            if(!tmp.isEmpty()){
                for (CallBackDelay c : tmp) {
                    c.run(callBackDelays);
                }
//                System.out.println(smthd + "///////////////////////");
            }
        }
        if (lifeCycle.contains(smthd) && countOrder.size() > 0) {
            GlobalStatistics.getInstance().countLifeCallBackHasOrder();
            countOrder.clear();
        }
    }

    private void doAnalysis(DirectedGraph<Block> directedGraph, Block block, SootMethod smthd, SootMethod caller, ArrayList<SootMethod> callTrace, HashSet<Block> solved, ArrayList<CallBackDelay> callBackDelays, int maxLength) {
        if (maxLength > 500)
            return;
        maxLength++;
        SootMethod call;
        Unit unit = block.getHead();
        boolean tail = true;
        while (tail) {
            if (block.getTail().equals(unit))
                tail = false;
            if (unit instanceof Stmt) {
                Stmt stmt = (Stmt) unit;
                processInstance(smthd, stmt);
                SootClass viewCall = InstanceUtility.isViewCall(unit);
                if (viewCall != null) {
                    doViewInit(smthd, viewCall, unit, callTrace, callBackDelays);
                }
                if (stmt.containsInvokeExpr()) {
                    if(processCallArgs(stmt)==-1)
                    {
                        break;
                    }
                    call = stmt.getInvokeExpr().getMethod();
                    SootClass intentCall = InstanceUtility.isIntentCall(smthd, call, unit);
                    if (intentCall != null) {
                        doDummyMain(smthd, intentCall, unit, callTrace, callBackDelays);
                        // continue?
                        if (tail)
                            unit = block.getSuccOf(unit);
                        continue;
                    }
                    // 判断是否是实现了回调函数，将回调函数对应，（如果调用的函数的类是系统函数）

                    boolean flag = processCallBack(smthd, call, stmt, unit, callTrace, callBackDelays);

                    if (call.getSubSignature().equals("boolean sendMessage(android.os.Message)")
                            || call.getSubSignature().equals("boolean sendEmptyMessage(int)") || call.getSubSignature().equals("boolean sendMessageDelayed(android.os.Message,long)")) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        assert base != null;
                        SootClass cls = inner.get(base.hashCode());
                        if (cls != null) {
                            doHandle(smthd, call, "void handleMessage(android.os.Message)", cls, unit, callTrace, callBackDelays);
                            doHandle(smthd, call, "boolean handleMessage(android.os.Message)", cls, unit, callTrace, callBackDelays);
                        }
                        flag = false;
                    }
                    if (call.getSubSignature()
                            .equals("void attachInterface(android.os.IInterface,java.lang.String)")) {
                        doAttachInterface(smthd, caller, stmt, false);
                    }
                    if (call.getSubSignature().equals("android.os.AsyncTask execute(java.lang.Object[])") || call.getSubSignature().equals("android.os.AsyncTask executeOnExecutor(java.util.concurrent.Executor,java.lang.Object[])")) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        assert base != null;
                        SootClass cls = utility.InstanceUtility.getSootClass(base.getType().toString());
                        doHandle(smthd, call, "void onPreExecute()", cls, unit, callTrace, callBackDelays);
                        doHandle(smthd, call, "java.lang.Object doInBackground(java.lang.Object[])", cls, unit, callTrace, callBackDelays);
                        doHandle(smthd, call, "void onPostExecute(java.lang.Object)", cls, unit, callTrace, callBackDelays);
                        doHandle(smthd, call, "void onCancelled()", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature()
                            .equals("bolts.Task callInBackground(java.util.concurrent.Callable,bolts.d)")) {
                        SootClass cls = utility.InstanceUtility
                                .getSootClass(stmt.getInvokeExpr().getArg(0).getType().toString());
                        doHandle(smthd, call, "java.lang.Object call()", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature()
                            .equals("android.content.ComponentName startService(android.content.Intent)")) {
                        SootClass cls = smthd.getDeclaringClass();
                        doHandle(smthd, call, "void onHandleIntent(android.content.Intent)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSignature()
                            .equals("<android.view.OrientationEventListener: void enable()>")) {
                        SootClass cls = smthd.getDeclaringClass();
                        doHandle(smthd, call, "void onOrientationChanged(int)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature()
                            .equals("void notifyDataSetChanged()")) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        assert base != null;
                        SootClass cls = utility.InstanceUtility.getSootClass(base.getType().toString());
                        doHandle(smthd, call, "void onBindViewHolder(android.support.v7.widget.RecyclerView$ViewHolder,int)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSignature()
                            .equals("<androidx.camera.core.ImageAnalysis: void setAnalyzer(java.util.concurrent.Executor,androidx.camera.core.ImageAnalysis$Analyzer)>")) {
                        SootClass cls = smthd.getDeclaringClass();
                        doHandle(smthd, call, "void analyze(androidx.camera.core.ImageProxy)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSignature()
                            .equals("<androidx.fragment.app.Fragment: androidx.activity.result.ActivityResultLauncher registerForActivityResult(androidx.activity.result.contract.ActivityResultContract,androidx.activity.result.ActivityResultCallback)>")) {
                        SootClass cls = InstanceUtility.getSootClass(stmt.getInvokeExpr().getArg(1).getType().toString());
                        if (cls != null)
                            doHandle(smthd, call, "void onActivityResult(java.lang.Object)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature().equals("void start()") && !call.getDeclaringClass().isApplicationClass() && stmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        assert base != null;
                        SootClass cls = utility.InstanceUtility.getSootClass(base.getType().toString());
                        doHandle(smthd, call, "void run()", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature().equals("android.os.CountDownTimer start()") && !call.getDeclaringClass().isApplicationClass() && stmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        assert base != null;
                        SootClass cls = utility.InstanceUtility.getSootClass(base.getType().toString());
                        doHandle(smthd, call, "void onTick(long)", cls, unit, callTrace, callBackDelays);
                        doHandle(smthd, call, "void onFinish()", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSubSignature()
                            .equals("bolts.Task onSuccess(bolts.h,java.util.concurrent.Executor,bolts.d)")) {
                        SootClass cls = utility.InstanceUtility
                                .getSootClass(stmt.getInvokeExpr().getArg(0).getType().toString());
                        doHandle(smthd, call, "java.lang.Object then(bolts.Task)", cls, unit, callTrace, callBackDelays);
                    } else if (call.getSignature()
                            .equals("<butterknife.ButterKnife: void bind(android.app.Activity)>")) {
                        String subSignature = String.format(
                                "void bind(butterknife.ButterKnife$Finder,%s,java.lang.Object)",
                                smthd.getDeclaringClass().getName());
                        SootClass cls = null;
                        for (SootMethod stmd : CallGraph.getCallTo(utility.InstanceUtility.getSootMethod(
                                "<butterknife.ButterKnife$ViewBinder: void bind(butterknife.ButterKnife$Finder,java.lang.Object,java.lang.Object)>"))) {
                            if (stmd.getDeclaringClass().declaresMethod(subSignature)) {
                                cls = stmd.getDeclaringClass();
                                break;
                            }
                        }
                        doHandle(smthd, call, subSignature, cls, unit, callTrace, callBackDelays);
                    }
                    // 如果是抽象的，并且存在接口函数,CallGraph.getCallTo已经处理了函数指向，将接口函数的所有实现添加到了指向里，istypes可能会造成oncreate()这种函数被找多次的问题
                    else if (InterfaceGraph.getInterfaceBy(call.getDeclaringClass()) != null && ((InstanceUtility.isTypes(smthd.getDeclaringClass(), "Plain","Activity") && !smthd.getDeclaringClass().getName().startsWith("dummyMainClass")) || InstanceUtility.isTypes(call.getDeclaringClass(), "Plain","Activity"))) {
                        if (interfaceMethod.contains(call)) {
                            if (tail)
                                unit = block.getSuccOf(unit);
                            continue;
                        }
                        // 存在有些函数调用，重载了父类函数，但是父类函数并不是抽象函数，先删了该部分
                        if (!InstanceUtility.isEmptyMethod(call)) {
//                            if (!(!smthd.isAbstract() && (stmt.getInvokeExpr() instanceof SpecialInvokeExpr) && ((SpecialInvokeExpr) stmt.getInvokeExpr()).getBase().toString().equals("r0") && !smthd.getDeclaringClass().equals(stmt.getInvokeExpr().getMethod().getDeclaringClass())))
                            Value base = InstanceUtility.getBaseInvoke(stmt);
                            if (base != null) {
                                SootClass callExtend = InstanceUtility.getSootClassByType(base.getType());
                                inner.put(call.hashCode(), callExtend);
                            }
                            getOrder(call, smthd, generateHash(call, smthd, unit), callTrace, callBackDelays);
                        }
                        interfaceMethod.add(call);
                        doInterface(smthd, call, call.getDeclaringClass(), unit, callTrace, callBackDelays);
                    } else if (flag) {
                        Value base = InstanceUtility.getBaseInvoke(stmt);
                        if (!call.getDeclaringClass().isApplicationClass() && base != null && inner.get(base.hashCode()) != null) {
                            SootMethod extend = getExtend(call, inner.get(base.hashCode()), false);
                            if (extend != null) {
                                realInstance.put(call, extend);
                                getOrder(extend, smthd, generateHash(extend, smthd, unit), callTrace, callBackDelays);
                            }
                            else
                                getOrder(call, smthd, generateHash(call, smthd, unit), callTrace, callBackDelays);
                        } else {
                            getOrder(call, smthd, generateHash(call, smthd, unit), callTrace, callBackDelays);
                        }
                    }
                    processInvoke(smthd, caller, stmt);
                    if (call.getSignature()
                            .equals("<java.lang.Class: java.lang.Object newInstance()>")) {
                        SootClass cls = inner.get(InstanceUtility.getBaseInvoke(stmt).hashCode());
                        if (cls != null) {
                            for (SootMethod sm : cls.getMethods()) {
                                if (sm.isConstructor()) {
                                    getOrder(sm, smthd, generateHash(sm, smthd, unit), callTrace, callBackDelays);
                                }
                            }
                        }
                    }
                }
            }
            if (tail)
                unit = block.getSuccOf(unit);
        }
        for (Block succof : directedGraph.getSuccsOf(block)) {
//            if (succof.getIndexInMethod() > block.getIndexInMethod()) {
                // 避免循环问题
//                if (directedGraph.getSuccsOf(block).size() == 1 && solved.contains(succof))
//                    continue;
                if (solved.contains(succof))
                    continue;
                solved.add(succof);

                doAnalysis(directedGraph, succof, smthd, caller, callTrace, solved, callBackDelays, maxLength);
//            }
        }
    }

    private void doInterface(SootMethod smthd, SootMethod call, SootClass callType, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        solvedMethods.add(call);
        SootClass real = null;
        if (((Stmt) unit).getInvokeExpr() instanceof InterfaceInvokeExpr)
            real = inner.get(InstanceUtility.getBaseInvoke((Stmt) unit).hashCode());
        if (((Stmt) unit).getInvokeExpr() instanceof VirtualInvokeExpr) {
            if (InstanceUtility.isEmptyMethod(call))
                real = inner.get(InstanceUtility.getBaseInvoke((Stmt) unit).hashCode());
            else
                real = inner.get(smthd.hashCode());
        }
        HashSet<SootClass> callBacks = new HashSet<>();
        if (real != null && real.isApplicationClass() && !real.isAbstract()) {
            callBacks.add(real);
        } else {
            callBacks.addAll(InterfaceGraph.getInterfaceBy(callType));
        }
        for (SootClass callback : callBacks) {
            SootMethod extend = null;
            if (real != null)
                extend = getExtend(call, callback, true);
            else
                extend = getExtend(call, callback, false);
            if (extend != null) {
                handlePro(call, extend);
                realInstance.put(call, extend);
                getOrder(extend, smthd, generateHash(extend, smthd, unit), callTrace, callBackDelays);
            }
            if (InterfaceGraph.getInterfaceBy(callback) != null && !(real != null && real.isApplicationClass())) {
                doInterface(smthd, call, callback, unit, callTrace, callBackDelays);
            }
        }
    }

    public SootMethod getExtend(SootMethod call, SootClass callback, boolean trace) {
        String abstractMethod = call.getSignature();
        String src = call.getDeclaringClass().getName().replace("$", "\\$");
        String des = callback.getName().replace("$", "\\$");
        String extendtMethod = abstractMethod.replaceFirst(src, des);
        SootMethod extend = null;
        if (Scene.v().containsMethod(extendtMethod))
            extend = Scene.v().getMethod(extendtMethod);
        if (trace && extend == null && callback.getSuperclass().isApplicationClass())
            extend = getExtend(call, callback.getSuperclass(), trace);
        return extend;
    }


    // 这里是处理所有相关的地方
    private boolean doCallBack(SootMethod smthd, SootMethod call,
                               SootClass callType, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        solvedMethods.add(call);
        boolean existCall = false;
        HashSet<String> callBacks = InterfaceGraph.getCallBacks(callType);
        if (callBacks != null) {
            for (Type type : call.getParameterTypes()) {
                if (callBacks.contains(type.toString())) {
                    for (SootMethod stmd : utility.InstanceUtility.getSootClass(type.toString()).getMethods()) {
                        String abstractMethod = stmd.getSignature();
                        String extendtMethod = abstractMethod.replace(stmd.getDeclaringClass().getName(),
                                callType.getName());
                        SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
                        if (extend != null) {
                            existCall = true;
                            handlePro(call, extend);
                            callBackDelays.add(new CallBackDelay(extend, smthd, generateHash(extend, smthd, unit), callTrace));
//                            getOrder(extend, smthd, generateHash(extend, smthd, unit), callTrace);
                        }
                    }
                }

            }
        }
        return existCall;
    }

    private void doThread(SootMethod smthd, SootMethod call, SootClass callType, String str, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        solvedMethods.add(call);
        for (Type type : call.getParameterTypes()) {
            if (type.toString().equals(str)) {
                for (SootMethod stmd : utility.InstanceUtility.getSootClass(type.toString()).getMethods()) {
                    String abstractMethod = stmd.getSignature();
                    String extendtMethod = abstractMethod.replace(stmd.getDeclaringClass().getName(), callType.getName());
                    SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
                    HashSet<SootMethod> ms = new HashSet<>();
                    MethodUtility.findAllPointerOfThisMethod(ms, stmd.getSubSignature(), callType);
                    for (SootMethod sm : ms) {
                        if (sm != null && sm.getDeclaringClass().isApplicationClass()) {
                            handlePro(call, extend);
                            getOrder(sm, smthd, generateHash(sm, smthd, unit), callTrace, callBackDelays);
                        }
                    }
                }
            }
        }
    }

    private void doHandle(SootMethod smthd, SootMethod call, String subSignature, SootClass cls, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {

        String extendtMethod = String.format("<%s: %s>", cls, subSignature);
        SootMethod extend = utility.InstanceUtility.getSootMethod(extendtMethod);
        if (extend != null && extend.getDeclaringClass().isApplicationClass()) {
            if (!recordproMethod.containsKey(call))
                recordproMethod.put(call, new ArrayList<>());
            recordproMethod.get(call).add(extend);
            getOrder(extend, smthd, generateHash(extend, smthd, unit), callTrace, callBackDelays);

        }

    }

    private SootClass doInvoke(SootMethod smthd, SootMethod caller, Stmt stmt) {
        String clsName = "";
        Value arg = stmt.getInvokeExpr().getArg(0);
        if (arg instanceof StringConstant) {
            StringConstant sc = (StringConstant) arg;
            clsName = sc.toString();
        } else if (arg instanceof Local) {
            for (Unit unit : caller.retrieveActiveBody().getUnits()) {
                Stmt s = (Stmt) unit;
                if (s.containsInvokeExpr() && s.getInvokeExpr().getMethod().equals(smthd)) {
                    for (Value a : s.getInvokeExpr().getArgs()) {
                        if (a instanceof StringConstant) {
                            StringConstant sc = (StringConstant) a;
                            clsName = sc.toString();
                        }
                    }
                }
            }
        }
        return InstanceUtility.getSootClass(clsName.replace("\"", ""));
    }

    private SootClass doAttachInterface(SootMethod smthd, SootMethod caller, Stmt stmt, boolean ask) {
        String name = "";
        Value arg = stmt.getInvokeExpr().getArg(1);
        if (arg instanceof StringConstant) {
            StringConstant sc = (StringConstant) arg;
            name = sc.toString();
        } else if (arg instanceof Local) {
            for (Unit unit : caller.retrieveActiveBody().getUnits()) {
                Stmt s = (Stmt) unit;
                if (s.containsInvokeExpr() && s.getInvokeExpr().getMethod().equals(smthd)) {
                    for (Value a : s.getInvokeExpr().getArgs()) {
                        if (a instanceof StringConstant) {
                            StringConstant sc = (StringConstant) a;
                            name = sc.toString();
                        }
                    }
                }
            }
        }
        if (ask) {
            return interfaceSetters.get(name);
        } else {
            SootClass cls = smthd.getDeclaringClass();
            interfaceSetters.put(name, cls);
            return null;
        }
    }


    private void extraDummyMain(SootMethod smthd, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        SootClass cls = smthd.getDeclaringClass();
        if (InstanceUtility.isType(cls, "Fragment")) {
            // 这里对不在entryPoint的class未排序
            if (!analyzer.getEntrypointClasses().contains(cls)) {
                if (firstAppear.contains(cls))
                    return;
                firstAppear.add(cls);
                for (SootMethod sm : cls.getMethods()) {
                    if (sm.isConstructor())
                        getOrder(sm, smthd, generateHash(sm, smthd, null), callTrace, callBackDelays);
                }
                for (MethodOrMethodContext methodContext : EntryPointsUtility.getLifecycleMethods(
                        entryPointsUtility.getComponentType(smthd.getDeclaringClass()), cls)) {
                    if (methodContext.method().getDeclaringClass().equals(cls)) {
                        getOrder(methodContext.method(), smthd, generateHash(methodContext.method(), smthd, null), callTrace, callBackDelays);
                    }

                }
            }
        }
    }

    public void doDummyMain(SootMethod smthd, SootClass cls, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        if (smthd.getDeclaringClass().equals(cls) || (smthd.getDeclaringClass().isInnerClass() && smthd.getDeclaringClass().getOuterClass().equals(cls)))
            return;
        String className = cls.toString();
        String dummyMainClassName = String.format(
                "<dummyMainClass: %s dummyMainMethod_%s(android.content.Intent)>", className,
                className.replace(".", "_"));
        if (firstAppear.contains(cls))
            return;
        firstAppear.add(cls);
        if (Scene.v().containsMethod(dummyMainClassName)) {
            SootMethod dummyMainClass = Scene.v().getMethod(dummyMainClassName);
            getOrder(dummyMainClass, smthd, generateHash(dummyMainClass, smthd, unit), callTrace, callBackDelays);
        }
    }

    public void doAndroidxDummyMain(SootMethod smthd, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        SootClass cls = smthd.getDeclaringClass();
        if (smthd.isConstructor() && InstanceUtility.isAndroidxProcess(cls)) {
            if (InterfaceGraph.getInterfaceBy(cls) == null) {
                for (MethodOrMethodContext methodContext : EntryPointsUtility.getLifecycleMethods(
                        EntryPointsUtility.ComponentType.Fragment, cls)) {
                    if (methodContext.method().getDeclaringClass().equals(cls)) {
                        getOrder(methodContext.method(), smthd, generateHash(methodContext.method(), smthd, null), callTrace, callBackDelays);
                    }

                }
            }
        }
    }

    public void doViewInit(SootMethod smthd, SootClass cls, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        viewRec.put(cls, smthd.getDeclaringClass());
        if (smthd.getDeclaringClass().equals(cls) || (smthd.getDeclaringClass().isInnerClass() && smthd.getDeclaringClass().getOuterClass().equals(cls)))
            return;
        for (SootMethod init : cls.getMethods()) {
            if (init.isConstructor()) {
                getOrder(init, smthd, generateHash(init, smthd, unit), callTrace, callBackDelays);
            }
        }
    }

    public static boolean isCalled(SootMethod smthd) {
        if (solvedMethods.contains(smthd))
            return true;
        return false;
    }

    public void setLifeCycle(SootMethod smthd) {
        lifeCycle.add(smthd);
    }

    public Integer generateHash(SootMethod call, SootMethod mthd, Unit unit) {
        Block block;
        try {
            if (unit == null) {
                return call.hashCode() + mthd.hashCode();
            }
            CompleteBlockGraph cbg = BlockGenerator.getInstance().generate(mthd.retrieveActiveBody());
            block = BlockUtility.findLocatedBlock(cbg, unit);
            return block.hashCode() + unit.hashCode() + call.hashCode();
        } catch (Exception e) {
            return 0000;
        }
    }

    public static ArrayList<SootMethod> getProMethod(SootMethod inter, HashSet<SootMethod> children) {
        ArrayList<SootMethod> store = new ArrayList<>();
        if (recordproMethod.containsKey(inter)) {
            ArrayList<SootMethod> record = recordproMethod.get(inter);
            for (SootMethod child : children) {
                if (record.contains(child))
                    store.add(child);
            }
        }
        return store;
    }

    private void processCallTrace(SootMethod smthd, ArrayList<SootMethod> callTrace) {
//        if (BorderClass.getBorderMethod().contains(smthd)) {
        if (!borderMethodCallTrace.containsKey(smthd)) {
            ArrayList<SootMethod> tmp = new ArrayList<>(callTrace);
            tmp.add(smthd);
            borderMethodCallTrace.put(smthd, tmp);
        }
//        }
    }

    private boolean processCallBack(SootMethod smthd, SootMethod call, Stmt stmt, Unit unit, ArrayList<SootMethod> callTrace, ArrayList<CallBackDelay> callBackDelays) {
        int count = 0;
        boolean flag = true;
        for (Type type : call.getParameterTypes()) {
            if (InterfaceGraph.isInCallBacks(type.toString())) {
                // 回调函数里传的参数，如果是回调函数类型

                SootClass cls = utility.InstanceUtility
                        .getSootClass(stmt.getInvokeExpr().getArg(count).getType().toString());
                // 这里指的是不存在内部类，直接实现在原class里
                if (cls == null)
                    cls = smthd.getDeclaringClass();
                if (!cls.isApplicationClass())
                    cls = inner.get(stmt.getInvokeExpr().getArg(count).hashCode());

                if (cls != null)
                    doCallBack(smthd, call, cls, unit, callTrace, callBackDelays);
                flag = false;
//									break;
            } else if (type.toString().equals("java.lang.Runnable")) {
                SootClass cls = utility.InstanceUtility
                        .getSootClass(stmt.getInvokeExpr().getArg(count).getType().toString());
                if (cls != null && !cls.isApplicationClass())
                    cls = inner.get(stmt.getInvokeExpr().getArg(count).hashCode());
                if (cls != null)
                    doThread(smthd, call, cls, "java.lang.Runnable", unit, callTrace, callBackDelays);
                flag = false;
            } else if (type.toString().equals("android.content.BroadcastReceiver")) {
                SootClass cls = utility.InstanceUtility
                        .getSootClass(stmt.getInvokeExpr().getArg(count).getType().toString());
                if (cls != null && !cls.isApplicationClass())
                    cls = inner.get(stmt.getInvokeExpr().getArg(count).hashCode());
                if (cls != null)
                    doThread(smthd, call, cls, "android.content.BroadcastReceiver", unit, callTrace, callBackDelays);
                flag = false;
            } else if (type.getEscapedName().equals("java.util.concurrent.Callable")) {
                SootClass cls = utility.InstanceUtility
                        .getSootClass(stmt.getInvokeExpr().getArg(count).getType().toString());
                if (cls != null && !cls.isApplicationClass())
                    cls = inner.get(stmt.getInvokeExpr().getArg(count).hashCode());
                if (cls != null)
                    doThread(smthd, call, cls, "java.util.concurrent.Callable", unit, callTrace, callBackDelays);
                flag = false;
            }
            count++;
        }
        return flag;
    }

    private void processInstance(SootMethod smthd, Stmt stmt) {
        SootClass cls;
        if (stmt instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) stmt;
            Value right = as.getRightOp();
            Value left = as.getLeftOp();
            Integer rightHashCode = right.hashCode();
            Integer leftHashCode = left.hashCode();
            if (right instanceof FieldRef) {
                rightHashCode = ((FieldRef) right).getField().toString().hashCode();
            }
            if (left instanceof FieldRef) {
                leftHashCode = ((FieldRef) left).getField().toString().hashCode();
            }
            if (right instanceof JCastExpr) {
                Value op = ((JCastExpr) right).getOp();
                rightHashCode = op.hashCode();
                if (op.toString().equals("r0")) {
                    cls = utility.InstanceUtility.getSootClass(op.getType().toString());
                    inner.put(rightHashCode, cls);
                }
            }
//            if (right instanceof NewExpr && Objects.requireNonNull(utility.InstanceUtility
//                    .getSootClass(((NewExpr) right).getType().toString())).isInnerClass()) {
            if (right instanceof NewExpr) {
                cls = utility.InstanceUtility.getSootClass(((NewExpr) right).getType().toString());
                // 额外处理一下
                assert cls != null;
                if (cls.getName().equals("android.os.Handler"))
                    cls = smthd.getDeclaringClass();
                inner.put(leftHashCode,
                        cls);
            } else if (inner.containsKey(rightHashCode)) {
                cls = inner.get(rightHashCode);
                inner.put(leftHashCode, cls);
            } else if (right instanceof InvokeExpr) {
                SootMethod invoke = stmt.getInvokeExpr().getMethod();
                if (InstanceUtility.isCallField(invoke)) {
                    Unit last = invoke.retrieveActiveBody().getUnits().getLast();
                    if (last instanceof ReturnStmt) {
                        if (inner.get(((ReturnStmt) last).getOp().hashCode()) != null) {
                            cls = inner.get(((ReturnStmt) last).getOp().hashCode());
                            inner.put(leftHashCode, cls);
                        }
                    }
                }

            }
        } else if (stmt instanceof IdentityStmt) {
            IdentityStmt is = (IdentityStmt) stmt;
            Value right = is.getRightOp();
            Value left = is.getLeftOp();
            Integer leftHashCode = left.hashCode();
            if (callArgsInner.containsKey(smthd) && right instanceof ParameterRef) {
                Integer v = callArgsInner.get(smthd).get(((ParameterRef) right).getIndex());
                if (v != 0) {
                    cls = inner.get(v);
                    inner.put(leftHashCode, cls);
                }
            }
        } else if (stmt instanceof ReturnStmt) {
            ReturnStmt rs = (ReturnStmt) stmt;
            Value rv = rs.getOp();
            if (rv instanceof NullConstant)
                return;
            SootClass returnCls;
            if (rv instanceof ClassConstant) {
                ClassConstant cc = (ClassConstant) rv;
                returnCls = InstanceUtility.getSootClass(cc.toSootType().toString());
            } else
                returnCls = InstanceUtility.getSootClass(rv.getType().toString());
            if (returnCls != null) {
                if (!returnInner.containsKey(smthd.hashCode()))
                    returnInner.put(smthd.hashCode(), new ArrayList<>());
                if (inner.containsKey(rv.hashCode())) {
                    cls = inner.get(rv.hashCode());
                    returnInner.get(smthd.hashCode()).add(cls);
                } else
                    returnInner.get(smthd.hashCode()).add(returnCls);
            }
        }
    }

    public void processInvoke(SootMethod smthd, SootMethod caller, Stmt stmt) {
        if (stmt instanceof AssignStmt) {
            AssignStmt as = (AssignStmt) stmt;
            Value right = as.getRightOp();
            Value left = as.getLeftOp();
            Integer leftHashCode = left.hashCode();
            if (right instanceof InvokeExpr) {
                SootMethod invoke = stmt.getInvokeExpr().getMethod();
                SootClass real = null;
                if (stmt.getInvokeExpr() instanceof InterfaceInvokeExpr)
                    real = inner.get(InstanceUtility.getBaseInvoke(stmt).hashCode());
                if (real != null) {
                    String abstractMethod = invoke.getSignature();
                    String src = invoke.getDeclaringClass().getName().replace("$", "\\$");
                    String des = real.getName().replace("$", "\\$");
                    String extendtMethod = abstractMethod.replaceFirst(src, des);
                    if (Scene.v().containsMethod(extendtMethod)) {
                        invoke = Scene.v().getMethod(extendtMethod);
                    }
                }
                SootClass returnCls;
                SootMethod instanceInvoke = realInstance.getOrDefault(invoke,null);
                if (invoke.getDeclaringClass().isApplicationClass() && returnInner.containsKey(invoke.hashCode())) {
                    returnCls = returnInner.get(invoke.hashCode()).get(0);
                    inner.put(leftHashCode, returnCls);
                }else if(instanceInvoke!=null&& returnInner.containsKey(instanceInvoke.hashCode())){
                    returnCls = returnInner.get(instanceInvoke.hashCode()).get(0);
                    inner.put(leftHashCode, returnCls);
                } else if (invoke.getSignature()
                        .equals("<java.lang.ClassLoader: java.lang.Class loadClass(java.lang.String)>")) {
                    SootClass cls = doInvoke(smthd, caller, stmt);
                    if (cls != null)
                        inner.put(leftHashCode, cls);
                } else if (invoke.getSignature()
                        .equals("<java.lang.Class: java.lang.Object newInstance()>")) {
                    SootClass cls = inner.get(InstanceUtility.getBaseInvoke(stmt).hashCode());
                    if (cls != null)
                        inner.put(leftHashCode, cls);
                } else if (invoke.getSubSignature()
                        .equals("android.os.IInterface queryLocalInterface(java.lang.String)")) {
                    SootClass cls = inner.get(InstanceUtility.getBaseInvoke(stmt).hashCode());
                    if (cls != null)
                        inner.put(leftHashCode, cls);
                } else {
                    returnCls = InstanceUtility.getSootClass(invoke.getReturnType().toString());
                    if (returnCls != null)
                        inner.put(leftHashCode, InstanceUtility.getSootClass(invoke.getReturnType().toString()));
                }
            }
        }
    }

    public int processCallArgs(Stmt stmt) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        SootMethod call = invokeExpr.getMethod();
        callArgsInner.remove(call);
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (Value arg : invokeExpr.getArgs()) {
            if (arg instanceof Local && inner.containsKey(arg.hashCode())) {
                arrayList.add(arg.hashCode());
            } else
                arrayList.add(0);
            if(arg instanceof StringConstant){
                StringConstant sc = (StringConstant) arg;
                String string = sc.toString().toLowerCase(Locale.ROOT);
                if(string.contains("releasing"))
                    return -1;
            }
        }
        callArgsInner.put(call, arrayList);
        return 0;
    }
    public static SootClass getTypeFrom(Value base){
        return inner.getOrDefault(base.hashCode(),null);

    }
    private void handlePro(SootMethod call, SootMethod extend) {
        if (!recordproMethod.containsKey(call))
            recordproMethod.put(call, new ArrayList<>());
        recordproMethod.get(call).add(extend);

    }

    public static ArrayList<SootMethod> getSolvedMethods() {
        return solvedMethods;
    }

    public Hashtable<SootMethod, ArrayList<SootMethod>> getBorderMethodCallTrace() {
        return borderMethodCallTrace;
    }

    public static Hashtable<SootClass, SootClass> getViewRec() {
        return viewRec;
    }
    public static Hashtable<Integer, SootClass> getInner() {
        return inner;
    }
    class CallBackDelay {
        SootMethod extend;
        SootMethod smthd;
        Integer integer;
        ArrayList<SootMethod> callTrace;

        CallBackDelay(SootMethod extend, SootMethod smthd, Integer integer, ArrayList<SootMethod> callTrace) {
            this.extend = extend;
            this.smthd = smthd;
            this.integer = integer;
            this.callTrace = callTrace;
        }

        private void run(ArrayList<CallBackDelay> callBackDelays) {
            getOrder(extend, smthd, integer, callTrace, callBackDelays);
        }
    }
}
