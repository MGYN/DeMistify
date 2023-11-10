package utility;

public class ScriptUtility {
    String jsTop = "Java.perform(function x() {\n" + "    console.log(\"Inside Java perform\");\n" + "    var path = \"/data/local/tmp/256/1.jpg\";\n" + "    var file = Java.use('java.io.File').$new(path);\n" + "    var FileInputStream = Java.use('java.io.FileInputStream').$new(file);\n" + "    var bitmap = Java.use('android.graphics.BitmapFactory').decodeStream(FileInputStream);\n" + "    var application = Java.use('android.app.ActivityThread').currentApplication();\n" + "    var context = application.getApplicationContext();\n" + "    var WindowManagerImpl = context.getSystemService(Java.use(\"android.content.Context\").WINDOW_SERVICE.value);\n" + "    var display = Java.cast(WindowManagerImpl, Java.use(\"android.view.WindowManager\")).getDefaultDisplay();\n" + "    var assets = context.getAssets();\n" + "    var res = context.getResources();\n";

}
