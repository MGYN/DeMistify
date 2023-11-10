package main;

import org.json.JSONArray;
import soot.Scene;
import utility.Logger;

import java.util.ArrayList;

public class Native {

    public static ArrayList<String> nativeClasses = new ArrayList<>();

    public static ArrayList<String> getNativeClasses() {
        return nativeClasses;
    }

    public static void solve(JSONArray nativeClass) {
        if (nativeClass.length() == 0) {
            Logger.printE("nativeClass is empty");
            System.exit(0);
        } else {
            for (Object tob : nativeClass) {
                String name = (String) tob;
                while (name.matches("(.*)\\.(.*)")) {
                    if (Scene.v().containsClass(name)) {
                        if (!nativeClasses.contains(name))
                            nativeClasses.add(name);
                        break;
                    } else {
                        name = name.substring(0, name.lastIndexOf("."));
                    }
                }
            }
            if (nativeClasses.isEmpty()) {
                Logger.printE("nativeClass is empty");
                System.exit(0);
            }
        }
    }
}
