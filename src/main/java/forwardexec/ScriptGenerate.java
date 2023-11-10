package forwardexec;

import main.AnalysisConfig;
import soot.SootClass;
import soot.jimple.Stmt;
import utility.FileUtility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class ScriptGenerate {
    HashSet<Stmt> globalStmt = new HashSet<Stmt>();
    private final ArrayList<String> scripts = new ArrayList<>();
    AnalysisConfig config = AnalysisConfig.getInstance();
    Hashtable<String, Integer> baseHash = new Hashtable<>();
    static ScriptGenerate sg = new ScriptGenerate();
    HashSet<SootClass> activity = new HashSet<>();

    public static ScriptGenerate getInstance() {
        return sg;
    }

    public static void reset() {
        sg = new ScriptGenerate();
    }

    public ScriptGenerate() {

    }

    public void addActivity(SootClass cls) {
        activity.add(cls);
    }

    public HashSet<SootClass> getActivity() {
        return activity;
    }

    public Hashtable<String, Integer> getBaseHash() {
        return baseHash;
    }

    public void addStmt(Stmt stmt) {
        globalStmt.add(stmt);
    }

    public boolean containsStmt(Stmt stmt) {
        return globalStmt.contains(stmt);
    }

    public void addScripts(ArrayList<String> tmpScripts) {
        scripts.addAll(tmpScripts);
    }

    public void write(int layer) {
        ArrayList<String> baseScript = FileUtility.readTxtFile(config.getAdditionScript());
        ArrayList<String> results = new ArrayList<>();
        for (String line : baseScript) {
            if (line.contains("//script")) {
                results.addAll(scripts);
            } else
                results.add(line.replace("launchName", config.getLauncher().getName()));
        }
        FileUtility.wf_s(config.getApkDir() + config.getNaturalOrderString() + layer + ".js", results, false);
    }
}
