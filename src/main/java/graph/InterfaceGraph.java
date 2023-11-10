
package graph;

import java.util.HashSet;
import java.util.Hashtable;

import main.AnalysisConfig;
import soot.Scene;
import soot.SootClass;
import utility.FileUtility;
import utility.InstanceUtility;
import utility.Logger;

public class InterfaceGraph {
    private static Hashtable<SootClass, HashSet<SootClass>> implement_by = new Hashtable<SootClass, HashSet<SootClass>>();
    private static Hashtable<SootClass, HashSet<String>> callBacks_to = new Hashtable<SootClass, HashSet<String>>();
    private static Hashtable<String, InterfaceGraphNode> nodes = new Hashtable<String, InterfaceGraphNode>();
    private static HashSet<SootClass> callBacks = new HashSet<SootClass>();

    public static void init(AnalysisConfig config) {
        long st = System.currentTimeMillis();
        Logger.printI("[Start Interface]");
        initCallBacks(config.getAdditionCallbacks());
        for (SootClass cls : Scene.v().getClasses()) {
            InstanceUtility.isConfuscated(cls);
            try {
                if (!cls.isApplicationClass()) {
                    continue;
                } else if (cls.hasSuperclass()) {
                    // 添加父类和子类的关系
                    // androidx被认为是用户代码
                    if (cls.getSuperclass().isApplicationClass())
                        addInterface(cls, cls.getSuperclass());
                    else
                        addCallBacks(cls, cls.getSuperclass());
                }
                // 获得该类的接口（包含继承和接口）
                if (cls.getInterfaces().isEmpty()) {
                    continue;
                }
                InterfaceGraph.createInterface(cls);
            } catch (Exception e) {
                System.out.println(cls + " has exception!");
            }
        }
        Logger.printI("[Interface time]:" + (System.currentTimeMillis() - st) / 1000 + "s");
    }

    private static void createInterface(SootClass cls) {
        for (SootClass implement : cls.getInterfaces()) {
            if (implement.isApplicationClass()) {
                addInterface(cls, implement);
            } else if (callBacks.contains(implement)) {
                addCallBacks(cls, implement);
            }

        }
    }

    public static boolean containsKey(SootClass cls) {
        return callBacks_to.containsKey(cls);
    }

    // 某个类实现了多个标准库的接口
    private static void addCallBacks(SootClass cls, SootClass implement) {
        if (!nodes.containsKey(cls.toString())) {
            InterfaceGraphNode tmp;
            tmp = new InterfaceGraphNode(cls);
            nodes.put(cls.toString(), tmp);
        }
        if (!callBacks_to.containsKey(cls)) {
            callBacks_to.put(cls, new HashSet<String>());
        }
        callBacks_to.get(cls).add(implement.toString());
    }

    // implement被cls实现了，包括了继承和接口
    private static void addInterface(SootClass cls, SootClass implement) {
        if (!nodes.containsKey(cls.toString())) {
            InterfaceGraphNode tmp;
            tmp = new InterfaceGraphNode(cls);
            nodes.put(cls.toString(), tmp);
        }
        if (!nodes.containsKey(implement.toString())) {
            InterfaceGraphNode tmp;
            tmp = new InterfaceGraphNode(implement);
            nodes.put(implement.toString(), tmp);
        }
        InterfaceGraphNode fn, tn;
        fn = getNode(cls);
        tn = getNode(implement);
        if (!implement_by.containsKey(implement)) {
            implement_by.put(implement, new HashSet<>());
        }

        implement_by.get(implement).add(cls);
        fn.addCallTo(tn);
        tn.addCallBy(fn);
    }

    public static InterfaceGraphNode getNode(SootClass from) {
        return getNode(from.toString());
    }

    public static InterfaceGraphNode getNode(String from) {
        return nodes.get(from);
    }

    // 获得某一类的所有依赖列表
    public static HashSet<SootClass> getInterfaceBy(SootClass implement) {
        try {
            return implement_by.get(implement);
        } catch (Exception e) {
            return null;
        }
    }

    public static HashSet<String> getCallBacks(SootClass cls) {
        try {
            return callBacks_to.get(cls);
        } catch (Exception e) {
            return null;
        }
    }

    private static void initCallBacks(String filePath) {
        for (String line : FileUtility.readTxtFile(filePath)) {
            if (InstanceUtility.getSootClass(line) != null)
                callBacks.add(InstanceUtility.getSootClass(line));
        }
    }

    public static boolean isInCallBacks(String callBack) {
        return callBacks.contains(InstanceUtility.getSootClass(callBack));
    }

    public static class InterfaceGraphNode implements GraphNode<InterfaceGraphNode> {
        SootClass cls;

        public InterfaceGraphNode(SootClass cls) {
            this.cls = cls;
        }

        HashSet<InterfaceGraphNode> implementBy = new HashSet<>();
        HashSet<InterfaceGraphNode> implementTo = new HashSet<>();

        public SootClass getCls() {
            return cls;
        }

        @Override
        public void addCallBy(InterfaceGraphNode clss) {
            implementBy.add(clss);
        }

        @Override
        public void addCallTo(InterfaceGraphNode clss) {
            implementTo.add(clss);
        }

        @Override
        public HashSet<InterfaceGraphNode> getCallBy() {
            return implementBy;
        }

        @Override
        public HashSet<InterfaceGraphNode> getCallTo() {
            return implementTo;
        }
    }

}
