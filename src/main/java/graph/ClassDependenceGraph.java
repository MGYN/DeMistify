package graph;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Objects;

import main.Native;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.util.Chain;
import utility.Logger;

public class ClassDependenceGraph {
    static Hashtable<SootClass, HashSet<SootClass>> dependence = new Hashtable<SootClass, HashSet<SootClass>>();
    static Hashtable<String, ClassDependenceGraphNode> nodes = new Hashtable<String, ClassDependenceGraphNode>();
    static Hashtable<String, Double> count = new Hashtable<String, Double>();


    public static void init() {
        long st = System.currentTimeMillis();
        ClassDependenceGraphNode tmp;
        for (SootClass cls : Scene.v().getClasses()) {
            if (cls.isApplicationClass()) {
                tmp = new ClassDependenceGraphNode(cls);
                nodes.put(cls.toString(), tmp);
            }
        }
        Logger.printI("[Start Dependence]:");
        for (SootClass cls : Scene.v().getClasses()) {
            if (!cls.isApplicationClass()) {
                continue;
            }
            try {
                ClassDependenceGraph.createDependence(cls);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        Logger.printI("[Dependence time]:" + (System.currentTimeMillis() - st) / 1000 + "s");
    }

    public static void createDependence(SootClass cls) {
        addDependence(cls, cls, 1);
        compareNative(cls);
        SootClass dep;
        for (SootClass inter : cls.getInterfaces()) {
            addDependence(cls, inter, 4);
        }
        for (SootMethod smthd : cls.getMethods()) {
            HashSet<SootClass> chain = new HashSet<>(cls.getInterfaces());
            if(cls.getSuperclass().isApplicationClass())
                chain.add(cls.getSuperclass());
            for(SootClass t:chain) {
                SootMethod resolve = Scene.v().getFastHierarchy().resolveMethod(t, smthd, true);
                if(resolve!=null)
                    addDependence(cls, resolve.getDeclaringClass(), 4);
            }
            if (!smthd.isConcrete())
                continue;
            Body body = smthd.retrieveActiveBody();
            for (Unit unit : body.getUnits()) {
                if (unit instanceof Stmt) {
                    if (((Stmt) unit).containsInvokeExpr()) {
                        SootMethod depMeth = ((Stmt) unit).getInvokeExpr().getMethod();
                        dep = depMeth.getDeclaringClass();
//                        if (depMeth.isAbstract() && dep.isApplicationClass()) {
//                                for(SootMethod resolve : Scene.v().getFastHierarchy().resolveAbstractDispatch(dep,depMeth))
//                                    if (resolve != null) {
//                                        while(cls.isInnerClass())
//                                            cls = cls.getOuterClass();
//                                        addDependence(cls, resolve.getDeclaringClass(), 4);
//                                    }
//                        }
                        addDependence(cls, dep, 4);
                    }
                }
            }
        }
    }

    public static void addDependence(SootClass cls, SootClass dep, double num) {
//        while (cls.isInnerClass())
//            cls = cls.getOuterClass();
//        while (dep.isInnerClass())
//            dep = dep.getOuterClass();
        if (dep.equals(cls) && dependence.containsKey(cls) && dependence.get(cls).contains(dep))
            return;
        if (dep.isApplicationClass()) {
            String clsPackageName = cls.getPackageName();
            String depPackageName = dep.getPackageName();
            ClassDependenceGraphNode fn, tn;
            fn = getNode(cls);
            tn = getNode(dep);
            if (fn == null || tn == null) {
                return;
            }
            if (!dependence.containsKey(cls)) {
                dependence.put(cls, new HashSet<SootClass>());
            }
            String edge = cls.getName() + "--" + dep.getName();
            if (!count.containsKey(edge)) {
                count.put(edge, 0.0);
            }
            dependence.get(cls).add(dep);

//            if (depPackageName.matches(clsPackageName)) {
//                num = num * 4;
//            } else {
//                num = num * 1.2;
//            }

            Double value = count.get(edge) + num;
            count.put(edge, value);
            fn.addCallTo(tn);
            tn.addCallBy(fn);
        }
    }

    public static void compareNative(SootClass cls) {
        for (String ncls : Native.getNativeClasses()) {
            if (Scene.v().getSootClassUnsafe(ncls).getPackageName().equals(cls.getPackageName())) {
                addDependence(cls, Scene.v().getSootClassUnsafe(ncls), 40.0);
            }
        }
    }

    public static ClassDependenceGraphNode getNode(SootClass from) {
        return getNode(from.toString());
    }

    public static ClassDependenceGraphNode getNode(String from) {
        return nodes.get(from);
    }

    public static HashSet<SootClass> getDependence(SootClass clsname) {
        return dependence.get(clsname);
    }

    public static boolean contains(SootClass cls) {
        return nodes.containsKey(cls.toString());
    }

    public static Double getCount(String edge) {
        return count.get(edge);
    }

    public static class ClassDependenceGraphNode implements GraphNode<ClassDependenceGraphNode> {
        SootClass cls;

        HashSet<ClassDependenceGraphNode> dependenceBy = new HashSet<>();
        HashSet<ClassDependenceGraphNode> dependenceTo = new HashSet<>();

        public ClassDependenceGraphNode(SootClass cls) {
            this.cls = cls;
        }

        public void addCallBy(ClassDependenceGraphNode clss) {
            dependenceBy.add(clss);
        }

        public void addCallTo(ClassDependenceGraphNode clss) {
            dependenceTo.add(clss);
        }

        public HashSet<ClassDependenceGraphNode> getCallBy() {
            return dependenceBy;
        }

        public HashSet<ClassDependenceGraphNode> getCallTo() {
            return dependenceTo;
        }

        public SootClass getCls() {
            return cls;
        }
    }
}
