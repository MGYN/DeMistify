package graph;

public enum PutIntentEnum {
    Short("<android.content.Intent: android.content.Intent putExtra(java.lang.String,short)>", 1),
    Int("<android.content.Intent: android.content.Intent putExtra(java.lang.String,int)>", 2),
    Long("<android.content.Intent: android.content.Intent putExtra(java.lang.String,long)>", 3),
    Float("<android.content.Intent: android.content.Intent putExtra(java.lang.String,float)>", 4),
    Double("<android.content.Intent: android.content.Intent putExtra(java.lang.String,double)>", 5),
    Boolean("<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean)>", 6),
    Byte("<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte)>", 7),
    Char("<android.content.Intent: android.content.Intent putExtra(java.lang.String,char)>", 8),
    String("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String)>", 9),
    Parcelable("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable)>", 10),
    ShortArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,short[])>", 11),
    IntArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,int[])>", 12),
    LongArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,long[])>", 13),
    FloatArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,float[])>", 14),
    DoubleArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,double[])>", 15),
    BooleanArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,boolean[])>", 16),
    ByteArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,byte[])>", 17),
    CharArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,char[])>", 18),
    StringArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.String[])>", 19),
    ParcelableArr("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Parcelable[])>", 20),
    Serializable("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.io.Serializable)>", 21),
    Bundle("<android.content.Intent: android.content.Intent putExtra(java.lang.String,android.os.Bundle)>", 22),
    CharSequence("<android.content.Intent: android.content.Intent putExtra(java.lang.String,java.lang.CharSequence)>", 23);

    private String key;
    private int value;
    PutIntentEnum(String key, int value) {
        this.key = key;
        this.value = value;
    }
    public String getKey() {
        return key;
    }

    public int getValue() {
        return value;
    }
}
