package base;

import org.json.JSONObject;
import soot.SootField;
import soot.Type;
import soot.jimple.Stmt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class GlobalStatistics implements Serializable {
    static GlobalStatistics gs = new GlobalStatistics();

    HashSet<String> jcdgNodes = new HashSet<>();

    public GlobalStatistics() {
    }

    public void countTerminal() {
        terminationParameterTrace++;
    }

    public void countDeathCodeMethod() {
        deathCodeMethod++;
    }
    public void resetDeathCodeMethod() {
        deathCodeMethod = 0;
    }

    public static GlobalStatistics getInstance() {
        return gs;
    }

    public void setisConfuscated() {
        confuscated = 1;
    }
    public int getisConfuscated() {
        return confuscated;
    }
    public static void setInstance(GlobalStatistics globalStatistics) {
        gs = globalStatistics;
    }

    public void countJniClass(int num) {
        jniClass += num;
    }


    public void countBorderParameterTrace() {
        borderParameterTrace++;
    }

    public void countReturnMethodTrace() {
        returnMethodTrace++;
    }

    public void countConstantParameterTrace() {
        constantParameterTrace++;
    }

    public void countBorderConstantParameterTrace() {
        borderConstantParameterTrace++;
    }

    public void countBorderTerminationParameterTrace() {
        borderTerminationParameterTrace++;
    }

    public void countBorderReturnParameterTrace() {
        borderReturnParameterTrace++;
    }

    public void countGlobalParameterTrace() {
        globalParameterTrace++;
    }

    public void countDiveIntoMethodCall() {
        diveIntoMethodCall++;
    }

    public void countBackWard2Caller() {
        backWard2Caller++;
    }

    public void countLifeCallBack() {
        lifeCallBack++;
    }


    public void countJcdgEdges() {
        jcdgEdge++;
    }

    public void addJcdgNodes(String str) {
        jcdgNodes.add(str);
    }

    public void countSecondCluster() {
        secondCluster++;
    }

    public void setFirstCluster(int num) {
        firstCluster = num;
    }


    public void countOrderMethod(int num) {
        orderMethod += num;
    }

    public void countBorderClass(int num) {
        borderClass = num;
    }

    public void addBorderClass() {
        borderClass++;
    }

    public void countBorderMethod(int num) {
        borderMethod = num;
    }

    public void countLifeCallBackHasOrder() {
        lifeCallBackHasOrder++;
    }

    public void countSortedMethod(int num) {
        sortedMethod = num;
    }

    public void countOrderFieldMethod() {
        orderFieldMethod++;
    }

    public void countImplicitParameter() {
        implicitParameter++;
    }

    public void countBranchNext() {
        branchNext++;
    }

    public void updateMaxCallStack(int i) {
        if (i > maxCallStack)
            maxCallStack = i;
    }

    int diveIntoMethodCall = 0;
    int backWard2Caller = 0;
    int maxCallStack = 0;

    int orderMethod = 0;
    int secondCluster = 0;
    int firstCluster;
    int jcdgEdge = 0;
    int lifeCallBack = 0;
    int lifeCallBackHasOrder = 0;
    int jniClass = 0;

    int borderParameterTrace = 0;
    int borderConstantParameterTrace = 0;
    int borderTerminationParameterTrace = 0;
    int borderReturnParameterTrace = 0;

    int constantParameterTrace = 0;
    int terminationParameterTrace = 0;
    int globalParameterTrace = 0;
    int returnMethodTrace = 0;

    int borderClass;
    int borderMethod;
    int sortedMethod;
    int deathCodeMethod = 0;
    int orderFieldMethod = 0;
    int branchNext = 0;

    int implicitParameter = 0;
    int confuscated = 0;

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        result.put("diveIntoMethodCall", diveIntoMethodCall);
        result.put("backWard2Caller", backWard2Caller);
        result.put("totalStackSize", diveIntoMethodCall + backWard2Caller);
        result.put("maxCallStack", maxCallStack);

        result.put("borderClass", borderClass);
        result.put("borderMethod", borderMethod);
        result.put("sortedMethod", sortedMethod);
        result.put("deathCodeMethod", deathCodeMethod);
        result.put("orderFieldMethod", orderFieldMethod);
        result.put("branchNext", branchNext);

        result.put("borderParameterTrace", borderParameterTrace);
        result.put("borderConstantParameterTrace", borderConstantParameterTrace);
        result.put("borderTerminationParameterTrace", borderTerminationParameterTrace);
        result.put("borderReturnParameterTrace", borderReturnParameterTrace);

        result.put("jniClass", jniClass);
        result.put("lifeCallBack", lifeCallBack);
        result.put("returnMethodTrace", returnMethodTrace);
        result.put("lifeCallBackHasOrder", lifeCallBackHasOrder);
        result.put("returnMethodTrace", returnMethodTrace);

        result.put("jcdgNodes", jcdgNodes.size());
        result.put("jcdgEdge", jcdgEdge);
        result.put("firstCluster", firstCluster);
        result.put("secondCluster", secondCluster);

        result.put("implicitParameter", implicitParameter);
        result.put("confuscated", confuscated);
        return result;
    }

    public GlobalStatistics cloneInstance() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(baos);
            oos.writeObject(this);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            return (GlobalStatistics) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        //从流中读取
        return null;

    }
}
