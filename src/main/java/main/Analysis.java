package main;

import backwardslicing.BackwardContext;
import backwardslicing.BackwardInstance;
import base.GlobalStatistics;
import forwardexec.ScriptGenerate;
import graph.CallGraph;
import graph.ClassDependenceGraph;
import graph.DGraph;
import graph.HeapObject;
import graph.IDGNode;
import graph.InterfaceGraph;
import graph.ValuePoint;
import order.OrderMethod;
import order.SortOrder;
import org.json.JSONObject;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.SetupApplication;
import utility.FileUtility;
import utility.InstanceUtility;
import utility.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Analysis {
    BorderClass borderClass;
    OrderMethod orderMethod;
    SetupApplication analyzer;
    AnalysisConfig config;
    SortOrder sortOrder;
    static List<String> content = new ArrayList<>();
    public static ArrayList<ValuePoint> allvps = new ArrayList<>();

    public Analysis(SetupApplication analyzer, AnalysisConfig config) {
        this.analyzer = analyzer;
        this.config = config;
    }

    public static void startWatcher(final long sec) {
        Thread t = new Thread() {
            public void run() {
                try {
                    Thread.sleep(sec * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                Logger.printE("TimeOut,exiting...");
                System.exit(0);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public void doLoadAndPreprocess() throws IOException, CloneNotSupportedException {


        initDirs();
        startWatcher(config.getTimeout());

        GlobalStatistics.getInstance().countJniClass(Native.getNativeClasses().size());
        GlobalStatistics cloneInstance = GlobalStatistics.getInstance().cloneInstance();
        // 9.21 有问题再删
        for (SootClass cls : Scene.v().getClasses()) {
            if (cls.getName().startsWith("androidx."))
                cls.setLibraryClass();
        }
        generateDependency();
        CallGraph.init(analyzer);
        borderClass = new BorderClass();
        borderClass.doAnalysis();
        orderMethod = new OrderMethod(analyzer);
        orderMethod.init();
        /*
        need modify
         */

        getSort(true, false);
        getSort(false, false);
        getSort(false, true);
        GlobalStatistics globalStatistics = GlobalStatistics.getInstance().cloneInstance();
        sortOrder = new SortOrder(orderMethod, globalStatistics);
        sortOrder.constructCallTree();
        if (config.isDoLayerMethod() || config.isDoLayerClass()) {
            for (int depth = 0; depth < config.getTraceLayer(); depth++) {
                if (!anslysisVP(globalStatistics.cloneInstance(), depth))
                    break;
            }
        } else {
            anslysisVP(globalStatistics.cloneInstance(), 0);
        }

    }

    public void wf(String content, String naturalOrderString, int depth) {
        FileUtility.wf(config.getApkDir() + naturalOrderString + depth + ".txt", content, false);
    }

    public void wf_s(String extra, List<String> content) {
        FileUtility.wf_s(config.getApkDir() + "_" + extra + ".txt", content, false);
    }

    public void initDirs() {
        File tmp = new File(config.getOutputFile());
        if (!tmp.exists()) tmp.mkdir();
        tmp = new File(config.getOutputFile(), analyzer.getPackageName());
        config.setApkDir(tmp.getPath() + "/" + analyzer.getPackageName());
        if (!tmp.exists()) tmp.mkdir();
    }

    public void generateDependency() {
        InterfaceGraph.init(config);
        ClassDependenceGraph.init();
        for (SootClass cls : Scene.v().getClasses()) {
            if (cls.isApplicationClass()) {
                if (ClassDependenceGraph.getDependence(cls) == null || cls.getName().equals("dummyMainClass")) continue;
                for (SootClass dep : ClassDependenceGraph.getDependence(cls)) {
//                    if (analyzer.getEntrypointClasses().contains(cls) || (cls.isInnerClass() && analyzer.getEntrypointClasses().contains(cls.getOuterClass())))
//                        continue;
                    GlobalStatistics.getInstance().addJcdgNodes(cls.getName());
                    GlobalStatistics.getInstance().addJcdgNodes(dep.getName());
                    GlobalStatistics.getInstance().countJcdgEdges();
                    String edge = cls.getName() + "--" + dep.getName();
                    double weight = ClassDependenceGraph.getCount(edge);
                    content.add(edge + "\t" + weight);
                }
            }
        }
        wf_s("edge", content);
    }

    public void getSort(boolean doLaunch, boolean doSurplus) throws IOException {
        SootMethod smthd = Scene.v().getMethod("<dummyMainClass: void dummyMainMethod(java.lang.String[])>");
        Body body = smthd.retrieveActiveBody();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof Stmt) {
                if (((Stmt) unit).containsInvokeExpr()) {
                    SootMethod call = ((Stmt) unit).getInvokeExpr().getMethod();
                    SootClass tmp = Scene.v().getSootClassUnsafe(call.getReturnType().toString());
                    if (doLaunch) {
                        if (!analyzer.getLaunchableActivitys().contains(tmp)) continue;
                    } else {
                        if (analyzer.getLaunchableActivitys().contains(tmp)) continue;
                        if (!doSurplus && !analyzer.getEntrypointClasses().contains(tmp)) continue;
                        if (doSurplus && analyzer.getEntrypointClasses().contains(tmp)) continue;
                    }
                    for (Unit dummy : call.retrieveActiveBody().getUnits()) {
                        if (((Stmt) dummy).containsInvokeExpr()) {
                            {
                                SootClass cls = ((Stmt) dummy).getInvokeExpr().getMethod().getDeclaringClass();
                                if (!InstanceUtility.isType(cls, "Plain")) {
                                    GlobalStatistics.getInstance().countLifeCallBack();
                                    orderMethod.setLifeCycle(((Stmt) dummy).getInvokeExpr().getMethod());
                                }
                            }
                        }
                    }
                    orderMethod.getOrder(call, smthd, orderMethod.generateHash(call, smthd, unit), new ArrayList<>(), new ArrayList<>());
                }
            }
        }
    }

    public boolean anslysisVP(GlobalStatistics globalStatistics, int depth) throws CloneNotSupportedException {
        System.out.printf("Run the No.%s layer trace.%n", depth);
        GlobalStatistics.setInstance(globalStatistics);
        ScriptGenerate.reset();
        BackwardInstance.reset();
        HeapObject.reset();
        BorderClass.getBorderMethodStmt().clear();
        allvps.clear();
        sortOrder.setInstance(globalStatistics);
        boolean layer = sortOrder.getSort(depth);
        if (layer) {
            DGraph dg = new DGraph();
            List<ValuePoint> vps;
            String tsig;
            List<Integer> regIndex;
            JSONObject tmp;
            for (SootMethod smth : sortOrder.getSortedOrder()) {
                if(depth==4){
                    System.out.println();
                }
                regIndex = new ArrayList<>();
                for (Type para : smth.getParameterTypes()) {
                    SootClass pareCls = InstanceUtility.getSootClassByType(para);
                    if (InstanceUtility.isImplicitParameter(pareCls)) {
                        GlobalStatistics.getInstance().countImplicitParameter();
                    }
                }
                for (int tob = 0; tob < smth.getParameterCount(); tob++) {
                    regIndex.add((Integer) tob);
                }
                tsig = smth.toString();
                vps = ValuePoint.find(dg, tsig, regIndex);
                for (ValuePoint vp : vps) {
                    tmp = new JSONObject();
                    tmp.put("sigatureInApp", tsig);
                    vp.setAppendix(tmp);
                    Stmt stmt = ((Stmt) vp.getInstruction_location());
                    BorderClass.setBorderMethodStmt(stmt);
                    vp.print();
                }
                allvps.addAll(vps);
            }
            dg.solve(allvps);
            ScriptGenerate.getInstance().write(depth);

            JSONObject result = new JSONObject();

            for (IDGNode tn : dg.getNodes()) {
                Logger.print(tn.toString());
            }
            for (IDGNode vp : dg.getNodes()) {
                if (vp instanceof HeapObject) continue;
                ValuePoint v = (ValuePoint) vp;
                for (BackwardContext var : v.getBcs()) {
                    var.printExceTrace();
                }
            }
            result.put("pname", analyzer.getPackageName());
            result.put("GlobalStatistics", GlobalStatistics.getInstance().toJson());
            wf(result.toString(), config.getNaturalOrderString(), depth);
        }
        return layer;
    }
}
