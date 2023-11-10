package main;

import base.GlobalStatistics;
import graph.CallGraph;
import graph.ClassDependenceGraph;
import soot.*;
import soot.jimple.Stmt;
import utility.FileUtility;
import utility.InstanceUtility;
import utility.MethodUtility;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class BorderClass {


    AnalysisConfig config = AnalysisConfig.getInstance();

    private final HashSet<SootClass> borderClass = new HashSet<>();
    private final HashSet<HashSet<SootClass>> communityClass = new HashSet<>();
    private static final HashSet<SootMethod> borderMethod = new HashSet<>();
    private static final HashSet<Stmt> borderMethodStmt = new HashSet<>();
    private String writePath = config.getApkDir() + "_first_community.txt";
    private ArrayList<String> nativeClassName = (ArrayList<String>) Native.getNativeClasses().clone();

    public BorderClass() {
    }

    public void doAnalysis() {
        if (config.isDoLayerMethod() || config.isDoLayerClass()) {
            communityNative();
            communityCluster(config.getApkDir() + "_edge.txt", config.getAdditionCluster());
//            writeBorderClass();
            onlyCalSecondCluster(FileUtility.readTxtFile(writePath));
            setBorderMethodNative();
        } else {
            setBorderClass(FileUtility.readTxtFile(writePath));
            setBorderMethod();
        }
    }

    private void communityNative() {
        for (String clsName : nativeClassName) {
            borderClass.add(InstanceUtility.getSootClass(clsName));
        }
    }


    private void communityCluster(String txt, String pyPath) {
        appendWithImplement();
        String[] cmdArr = new String[]{"python3", pyPath, txt, nativeClassName.toString()};
        Process process;
        try {
            process = Runtime.getRuntime().exec(cmdArr);
            InputStream is = process.getInputStream();
            DataInputStream dis = new DataInputStream(is);
            writePath = dis.readLine();
            GlobalStatistics.getInstance().setFirstCluster(Integer.parseInt(writePath.split("&")[0]));
            process.waitFor();
            writePath = writePath.split("&")[1];
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void appendWithImplement() {
        HashSet<String> tmp = new HashSet<>();
        for (String clsName : nativeClassName) {
            SootClass cls = Scene.v().getSootClassUnsafe(clsName);
            if (cls != null) {
                for (SootClass imp : cls.getInterfaces()) {
                    if (imp.isApplicationClass())
                        tmp.add(imp.getName());
                }
                if (cls.getSuperclass().isApplicationClass())
                    tmp.add(cls.getSuperclass().getName());
            }
        }
        nativeClassName.addAll(tmp);
    }

    private void setBorderMethod() {
        HashSet<SootClass> tmpBorderClass = (HashSet<SootClass>) borderClass.clone();
//        for (SootClass clss : borderClass) {
//            if (clss.isInterface()) {
//                tmpBorderClass.remove(clss);
//            }
//        }
        for (SootClass cls : tmpBorderClass) {
            for (SootMethod stmd : cls.getMethods()) {
                if (!isInBorder(stmd, tmpBorderClass)) {
                    if (!stmd.isAbstract())
                        borderMethod.add(stmd);
                }
            }
        }
        GlobalStatistics.getInstance().countBorderMethod(borderMethod.size());
    }

    private void setBorderMethodNative() {
        HashSet<SootClass> tmpBorderClass = (HashSet<SootClass>) borderClass.clone();
//        for (SootClass clss : borderClass) {
//            if (clss.isInterface()) {
//                tmpBorderClass.remove(clss);
//            }
//        }
        for (SootClass cls : tmpBorderClass) {
            for (SootMethod stmd : cls.getMethods()) {
                if (stmd.isNative()) {
                    String name = stmd.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("close") || name.contains("destroy") || name.contains("delete") || name.contains("remove") || name.contains("flush") || name.contains("release"))
                        continue;
                    borderMethod.add(stmd);
                }
            }
        }
        GlobalStatistics.getInstance().countBorderMethod(borderMethod.size());
    }

    public static boolean isInBorder(SootMethod meth, HashSet<SootClass> borderClassCount) {
//		if (androidMethod.contains(meth.getSubSignature()))
//			return true;
        boolean inBorder = true;
        HashSet<SootMethod> ms = new HashSet<>();
        MethodUtility.findAllPointerOfThisMethod(ms, meth.getSubSignature(), meth.getDeclaringClass());
        for (SootMethod smthd : ms) {
            for (SootMethod call : CallGraph.getCallFrom(smthd)) {
//				inBorder = false;
                if (!borderClassCount.contains(call.getDeclaringClass())) {
                    inBorder = false;
                    break;
                }

            }
        }
        return inBorder;
    }

    private void onlyCalSecondCluster(ArrayList<String> lineTxt) {
        for (String line : lineTxt)
            if (line.equals("******************************")) {
                GlobalStatistics.getInstance().countSecondCluster();
            }
    }

    private void setBorderClass(ArrayList<String> lineTxt) {
        boolean isNative = false;
        HashSet<SootClass> community = new HashSet<>();
        for (String line : lineTxt)
            if (line.equals("******************************")) {
                GlobalStatistics.getInstance().countSecondCluster();
                if (isNative) {
                    borderClass.addAll(community);
                    isNative = false;
                } else {
                    communityClass.add((HashSet<SootClass>) community.clone());
                }
                community.clear();
            } else {
                if (nativeClassName.contains(line))
                    isNative = true;
                community.add(InstanceUtility.getSootClass(line));
            }
    }

    private int getBorderMethodCount(HashSet<SootClass> borderClassCount) {
        int count = 0;
        for (SootClass cls : borderClassCount) {
            if (cls.isInterface())
                continue;
            for (SootMethod stmd : cls.getMethods()) {
                if (!isInBorder(stmd, borderClassCount)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void mergeBorderClass() {
        for (HashSet<SootClass> classSet : communityClass) {
            HashSet<SootClass> mergeClass = (HashSet<SootClass>) classSet.clone();
            mergeClass.addAll(borderClass);
            if (getBorderMethodCount(borderClass) >= getBorderMethodCount(mergeClass) && borderClass.size() != mergeClass.size()) {
                borderClass.clear();
                borderClass.addAll(mergeClass);
                mergeBorderClass();
            }
        }
    }

    private void addTensorFlow() {
        HashSet<SootClass> tmp = new HashSet<>();
        for (SootClass cls : Scene.v().getClasses()) {
            if (!cls.getName().startsWith("org.tensorflow")) {
                if (ClassDependenceGraph.contains(cls))
                    for (ClassDependenceGraph.ClassDependenceGraphNode dep : ClassDependenceGraph.getNode(cls).getCallTo()) {
                        if (dep.getCls().getName().startsWith("org.tensorflow") && InstanceUtility.isType(cls, "Plain"))
                            tmp.add(cls);

                    }
            }
        }
        borderClass.addAll(tmp);
    }

    private void writeBorderClass() {
        HashSet<SootClass> tmp = (HashSet<SootClass>) borderClass.clone();
        for (SootClass cls : borderClass) {
            // remove dummyMain function
            if (!InstanceUtility.isType(cls, "Plain"))
                tmp.remove(cls);
            // remove thread
            if (cls.isInnerClass() && cls.getInterfaceCount() > 0 && (cls.getInterfaces().contains(InstanceUtility.getSootClass("java.lang.Runnable")) || cls.getInterfaces().contains(InstanceUtility.getSootClass("java.lang.Thread"))))
                tmp.remove(cls);
        }
        borderClass.clear();
        borderClass.addAll(tmp);
        addTensorFlow();

        writeToFile();
        GlobalStatistics.getInstance().countBorderClass(borderClass.size());
    }

    private void writeToFile() {
        List<String> content = new ArrayList<String>();
        for (SootClass cls : borderClass) {
            content.add(cls.toString());
        }
        FileUtility.wf_s(writePath.replace("_first", ""), content, false);
    }


    public static HashSet<SootMethod> getBorderMethod() {
        return borderMethod;
    }

    public static HashSet<Stmt> getBorderMethodStmt() {
        return borderMethodStmt;
    }


    public static void setBorderMethodStmt(Stmt stmt) {
        borderMethodStmt.add(stmt);
    }


}
