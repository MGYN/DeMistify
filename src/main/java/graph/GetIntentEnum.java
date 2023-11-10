package graph;

public enum GetIntentEnum {
    Short("<android.content.Intent: short getShortExtra(java.lang.String,short)>", 1),
    Int("<android.content.Intent: int getIntExtra(java.lang.String,int)>", 2),
    Long("<android.content.Intent: long getLongExtra(java.lang.String,long)>", 3),
    Float("<android.content.Intent: float getFloatExtra(java.lang.String,float)>", 4),
    Double("<android.content.Intent: double getDoubleExtra(java.lang.String,double)>", 5),
    Boolean("<android.content.Intent: boolean getBooleanExtra(java.lang.String,boolean)>", 6),
    Byte("<android.content.Intent: byte getByteExtra(java.lang.String,byte)>", 7),
    Char("<android.content.Intent: char getCharExtra(java.lang.String,char)>", 8),
    String("<android.content.Intent: java.lang.String getStringExtra(java.lang.String)>", 9),
    Parcelable("<android.content.Intent: android.os.Parcelable getParcelableExtra(java.lang.String)>", 10),
    ShortArr("<android.content.Intent: short[] getShortArrayExtra(java.lang.String)>", 11),
    IntArr("<android.content.Intent: int[] getIntArrayExtra(java.lang.String)>", 12),
    LongArr("<android.content.Intent: long[] getLongArrayExtra(java.lang.String)>", 13),
    FloatArr("<android.content.Intent: float[] getFloatArrayExtra(java.lang.String)>", 14),
    DoubleArr("<android.content.Intent: double[] getDoubleArrayExtra(java.lang.String)>", 15),
    BooleanArr("<android.content.Intent: boolean[] getBooleanArrayExtra(java.lang.String)>", 16),
    ByteArr("<android.content.Intent: byte[] getByteArrayExtra(java.lang.String)>", 17),
    CharArr("<android.content.Intent: char[] getCharArrayExtra(java.lang.String)>", 18),
    StringArr("<android.content.Intent: java.lang.String[] getStringArrayExtra(java.lang.String)>", 19),
    ParcelableArr("<android.content.Intent: android.os.Parcelable[] getParcelableArrayExtra(java.lang.String)>", 20),
    Serializable("<android.content.Intent: java.io.Serializable getSerializableExtra(java.lang.String)>", 21),
    Bundle("<android.content.Intent: android.os.Bundle getBundleExtra(java.lang.String)>", 22),
    CharSequence("<android.content.Intent: java.lang.CharSequence getCharSequenceExtra(java.lang.String)>", 23);

    private String key;
    private int value;
    GetIntentEnum(String key, int value) {
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
