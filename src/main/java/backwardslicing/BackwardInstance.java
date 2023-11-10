package backwardslicing;

import soot.SootField;
import soot.Type;
import soot.Value;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.Hashtable;

public class BackwardInstance {
    private static final Hashtable<SootField, ArrayList<Stmt>> tracedField = new Hashtable<>();
    private static final Hashtable<Type, ArrayList<Stmt>> thisInstance = new Hashtable<>();
    private static final Hashtable<Value, Value> fieldSwitch = new Hashtable<>();

    public static Hashtable<Type, ArrayList<Stmt>> getThisInstance() {
        return thisInstance;
    }

    public static Hashtable<SootField, ArrayList<Stmt>> getTracedField() {
        return tracedField;
    }

    public static Hashtable<Value, Value> getFieldSwitch() {
        return fieldSwitch;
    }

    public static void putFieldSwitch(Value a, Value b) {
        if (a != null && b != null)
            fieldSwitch.put(a, b);
    }

    public static void reset() {
        tracedField.clear();
        thisInstance.clear();
        fieldSwitch.clear();
    }
}
