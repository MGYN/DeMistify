package utility;

import graph.GetIntentEnum;
import graph.PutIntentEnum;

public class EnumUtility {

    public static boolean isIncludePut(String key) {
        boolean include = false;
        for (PutIntentEnum e : PutIntentEnum.values()) {
            if (e.getKey().equals(key)) {
                include = true;
                break;
            }
        }
        return include;
    }

    public static boolean isIncludeGet(String key) {
        boolean include = false;
        for (GetIntentEnum e : GetIntentEnum.values()) {
            if (e.getKey().equals(key)) {
                include = true;
                break;
            }
        }
        return include;
    }

    public static PutIntentEnum getPutIntentEnumType(String key) {
        for (PutIntentEnum e : PutIntentEnum.values()) {
            if (e.getKey().equals(key)) {
                return e;
            }
        }
        return null;
    }
    public static GetIntentEnum getGetIntentEnumType(String key) {
        for (GetIntentEnum e : GetIntentEnum.values()) {
            if (e.getKey().equals(key)) {
                return e;
            }
        }
        return null;
    }
}
