package main;

import soot.SootClass;

import java.util.HashSet;

public class AnalysisConfig {
    private AnalysisConfig() {

    }

    public static AnalysisConfig getInstance() {
        return config;
    }


    private SootClass launcher;


    private SootClass softInput;
    static AnalysisConfig config = new AnalysisConfig();
    // Files
    private String targetConfigFile = "";
    private String androidPlatformDir = System.getenv("ANDROID_JARS");
    private String outputFile = "C:\\Users\\RPC_7\\Desktop\\test\\";

    // Timeouts
    private long timeout = 60 * 1000;


    // Analysis Parameters
    private boolean naturalOrder = true;
    private String naturalOrderString = "";
    private String additionPath = "";

    private boolean doLayerMethod = false;
    private boolean doLayerClass = false;
    private Integer traceLayer = 0;

    private String packageName = "";

    private String apkDir = "";


    public void setTargetConfigFile(String targetConfigFile) {
        this.targetConfigFile = targetConfigFile;
    }

    // Files
    public String getTargetConfigFile() {
        return targetConfigFile;
    }

    public void setAndroidPlatformDir(String androidPlatformDir) {
        this.androidPlatformDir = androidPlatformDir;
    }

    public String getAndroidPlatformDir() {
        return androidPlatformDir;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    // Timeouts
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    // Analysis Parameters


    public void setNaturalOrder(String naturalOrder) {
        boolean order = Boolean.parseBoolean(naturalOrder);
        this.naturalOrder = order;
        if (!order)
            naturalOrderString = "_reverse";
    }

    public String getNaturalOrderString() {
        return naturalOrderString;
    }

    public boolean isNaturalOrder() {
        return naturalOrder;
    }
    public boolean isDoLayerMethod() {
        return doLayerMethod;
    }

    public void setDoLayerMethod(boolean doLayerMethod) {
        this.doLayerMethod = doLayerMethod;
    }
    public boolean isDoLayerClass() {
        return doLayerClass;
    }

    public void setDoLayerClass(boolean doLayerClass) {
        this.doLayerClass = doLayerClass;
    }
    public Integer getTraceLayer() {
        return traceLayer;
    }

    public void setTraceLayer(Integer traceLayer) {
        this.traceLayer = traceLayer;
    }

    public void setAdditionPath(String additionPath) {
        this.additionPath = additionPath;
    }

    public String getApkDir() {
        return apkDir;
    }

    public void setApkDir(String apkDir) {
        this.apkDir = apkDir;
    }

    public String getAdditionCluster() {
        String cluster = "addition/cluster.py";
        if (!this.additionPath.isEmpty())
            cluster = this.additionPath + "/" + cluster;
        return cluster;
    }

    public String getAdditionScript() {
        String script = "addition/script.js";
        if (!this.additionPath.isEmpty())
            script = this.additionPath + "/" + script;
        return script;
    }

    public String getAdditionCallbacks() {
        String callBacks = "addition/AndroidCallbacks.txt";
        if (!this.additionPath.isEmpty())
            callBacks = this.additionPath + "/" + callBacks;
        return callBacks;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public SootClass getLauncher() {
        return this.launcher;
    }

    public void setLauncher(HashSet<SootClass> launcher) {
        for (SootClass l : launcher)
            this.launcher = l;
    }

    public SootClass getSoftInput() {
        return softInput;
    }

    public void setSoftInput(SootClass softInput) {
        this.softInput = softInput;
    }
}
