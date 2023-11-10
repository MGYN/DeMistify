package main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.json.JSONArray;


import org.json.JSONObject;
import soot.PackManager;
import soot.Scene;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import utility.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    protected final Options options = new Options();
    protected SetupApplication analyzer = null;
    long stime = System.currentTimeMillis();
    // Files
    private static final String OPTION_CONFIG_FILE = "c";
    private static final String OPTION_PLATFORMS_DIR = "p";
    private static final String OPTION_OUTPUT_FILE = "o";

    // Timeouts
    private static final String OPTION_TIMEOUT = "dt";

    // Analysis Parameters
    public static final String OPTION_NATURAl_ORDER = "nor";
    public static final String OPTION_ADDITION_PATH = "ap";
    public static final String OPTION_DO_LAYER_METHOD = "dlm";
    public static final String OPTION_DO_LAYER_CLASS = "dlc";
    public static final String OPTION_TRACE_LAYER = "tl";
    public static long concurrentTime1, concurrentTime2, concurrentMemory1, concurrentMemory2;
    final AnalysisConfig config = AnalysisConfig.getInstance();

    protected Main() {
        initializeCommandLineOptions();
    }


    private void initializeCommandLineOptions() {
        options.addOption(OPTION_CONFIG_FILE, "configfile", true, "Use the given configuration file");

        options.addOption(OPTION_PLATFORMS_DIR, "platformsdir", true, "Path to the platforms directory from the Android SDK");
        options.addOption(OPTION_OUTPUT_FILE, "outputfile", true, "Output XML file for the discovered data flows");
        options.addOption(OPTION_ADDITION_PATH, "addition", true, "addition path");
        options.addOption(OPTION_NATURAl_ORDER, "natural", true, "natural");
        options.addOption(OPTION_DO_LAYER_METHOD, "dolayerMethod", true, "dolayerMethod");
        options.addOption(OPTION_DO_LAYER_CLASS, "dolayerClass", true, "dolayerClass");
        options.addOption(OPTION_TRACE_LAYER, "tracelayer", true, "trace");
        // Timeouts
        options.addOption(OPTION_TIMEOUT, "timeout", true, "Timeout for the main data flow analysis");
    }

    private Integer getIntOption(CommandLine cmd, String option) {
        String str = cmd.getOptionValue(option);
        if (str == null || str.isEmpty()) return null;
        else return Integer.parseInt(str);
    }

    private void parseCommandLineOptions(CommandLine cmd) {
        // Files
        {
            String configFile = cmd.getOptionValue(OPTION_CONFIG_FILE);
            if (configFile != null && !configFile.isEmpty()) config.setTargetConfigFile(configFile);
        }
        {
            String platformsDir = cmd.getOptionValue(OPTION_PLATFORMS_DIR);
            if (platformsDir != null && !platformsDir.isEmpty()) config.setAndroidPlatformDir(platformsDir);
        }
        {
            String outputFile = cmd.getOptionValue(OPTION_OUTPUT_FILE);
            if (outputFile != null && !outputFile.isEmpty()) config.setOutputFile(outputFile);
        }
        {
            Integer timeout = getIntOption(cmd, OPTION_TIMEOUT);
            if (timeout != null) config.setTimeout(timeout);
        }
        {
            String doLayerMethod = cmd.getOptionValue(OPTION_DO_LAYER_METHOD);
            if (doLayerMethod != null) config.setDoLayerMethod(Boolean.parseBoolean(doLayerMethod));
        }{
            String doLayerClass = cmd.getOptionValue(OPTION_DO_LAYER_CLASS);
            if (doLayerClass != null) config.setDoLayerClass(Boolean.parseBoolean(doLayerClass));
        }
        {
            Integer traceLayer = getIntOption(cmd, OPTION_TRACE_LAYER);
            if (traceLayer != null) config.setTraceLayer(traceLayer);
        }
        {
            String naturalOrder = cmd.getOptionValue(OPTION_NATURAl_ORDER);
            if (naturalOrder != null && !naturalOrder.isEmpty()) config.setNaturalOrder(naturalOrder);
        }
        {
            String additionPath = cmd.getOptionValue(OPTION_ADDITION_PATH);
            if (additionPath != null && !additionPath.isEmpty()) config.setAdditionPath(additionPath);
        }
    }

    public static void main(String[] args) throws Exception {
        concurrentTime1 = System.nanoTime();
        Runtime runtime = Runtime.getRuntime();
        concurrentMemory1 = runtime.totalMemory()-runtime.freeMemory();
        Main main = new Main();
        main.run(args);
        concurrentTime2 = System.nanoTime();
        //得到虚拟机运行、所要测试的执行代码执行完毕时jvm所占用的内存（byte）。
        concurrentMemory2 = runtime.totalMemory()-runtime.freeMemory();
        String memory = String.valueOf((double)(concurrentMemory2-concurrentMemory1)/1024/1024) ;
        System.err.printf("Total memory costs: %s%n", memory);
    }

    public void run(String[] args) throws IOException, CloneNotSupportedException {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            parseCommandLineOptions(cmd);
        } catch (Exception e) {
            System.err.printf("The analysis has failed. Error message: %s%n", e.getMessage());
            e.printStackTrace();
        }

        JSONObject targetConfig = new JSONObject(new String(Files.readAllBytes(Paths.get(config.getTargetConfigFile()))));
        String apk = targetConfig.getString("apk");
        JSONArray nativeClass = targetConfig.getJSONArray("nativeClass");
        File file = new File(apk);
        String apkPath = file.getAbsolutePath();
        buildCallGraph(apkPath, config.getAndroidPlatformDir());
        Native.solve(nativeClass);
        config.setPackageName(analyzer.getPackageName());
        config.setLauncher(analyzer.getLaunchableActivitys());
        config.setSoftInput(analyzer.getSoftInput());
        Analysis analysis = new Analysis(analyzer,config);
        analysis.doLoadAndPreprocess();
        long etime = System.currentTimeMillis();
        Logger.printI("[Total Time Cost]:" + (etime - stime) / 1000 + "s");
    }


    public void buildCallGraph(String apkDir, String platformDir) {
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(apkDir);
        config.getAnalysisFileConfig().setAndroidPlatformDir(platformDir);
        config.setEnableReflection(true);
        config.setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);
        config.setMergeDexFiles(true);
        config.getCallbackConfig().setEnableCallbacks(false);
        // add the onActivityResult && onResumeFragments in soot-infoflow-android
        analyzer = new SetupApplication(config);
        analyzer.constructCallgraph();
        Logger.printI("[Load APK time]:" + (System.currentTimeMillis() - stime) / 1000 + "s");
        // SystemClassHandler.v().isClassInSystemPackage(className) 修改
    }
}