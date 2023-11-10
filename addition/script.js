function main() {
    console.log("Enter the Script!");
    Java.perform(function x() {
        Java.scheduleOnMainThread(function () {
            run();

            async function run() {
                console.log("Inside Java perform");
                var path = "/data/local/tmp/256/1.jpg";
                var file = Java.use('java.io.File').$new(path);
                var FileInputStream = Java.use('java.io.FileInputStream').$new(file);
                var bitmap = Java.use('android.graphics.BitmapFactory').decodeStream(FileInputStream);
                var application = Java.use('android.app.ActivityThread').currentApplication();
                var context = application.getApplicationContext();
                var WindowManagerImpl = context.getSystemService(Java.use("android.content.Context").WINDOW_SERVICE.value);
                var display = Java.cast(WindowManagerImpl, Java.use("android.view.WindowManager")).getDefaultDisplay();
                var assets = context.getAssets();
                var res = context.getResources();
                var save_path = context.getExternalFilesDir(null).getAbsolutePath();
                var point = Java.use("android.graphics.Point").$new(bitmap.getWidth(), bitmap.getHeight());
                let sleep2 = (time) => new Promise((resolve) => {
                    setTimeout(resolve, time)
                })
                var launch = getClass("launchName");
                var activityCls;
                var fragmentCls;

                //script

                console.log("Execute over!");
                function resizeBitmap(targetWidth, targetHeight) {

                    return Java.use("android.graphics.Bitmap").createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                }
                function startFragment(start, end, id) {
                    var transaction = start.getSupportFragmentManager().beginTransaction();
                    var fragment = Java.use(end).$new();
                    transaction.replace(id, fragment);
                    transaction.commit();
                    setTimeout(function () {
                        Java.choose(end, {
                            onMatch: function (instance) {
                                fragmentCls = instance;
                            },
                            onComplete: function () {
                            }
                        });
                    }, 500)
                }

                function startActivity(start, end) {
                    const intentClass = Java.use("android.content.Intent");
                    var intent = intentClass.$new(context, Java.use(end).class);
                    intent.setFlags(intentClass.FLAG_ACTIVITY_NEW_TASK.value);
                    start.startActivity(intent);
                    setTimeout(function () {
                        Java.choose(end, {
                            onMatch: function (instance) {
                                activityCls = instance;
                            },
                            onComplete: function () {
                            }
                        });
                    }, 500)
                }

                function getClass(className) {
                    var cls;
                    Java.choose(className, {
                        onMatch: function (instance) {
                            cls = instance;
                        },
                        onComplete: function () {
                        }
                    });
                    return cls;
                }

                function hookCallback(className, funcName) {
                    try {
                        const FaceTracking = Java.use(className);
                        FaceTracking[funcName].implementation = function () {
                            var numArgs = arguments.length;
                            for (var i = 0; i < numArgs; i++) {
                                console.log('\tArgument ' + i + ': ');
                                if (arguments[i].class===undefined) {
                                    console.log(arguments[i]);
                                }else
                                    print(arguments[i])
                            }
                            this[funcName].apply(this, arguments);
                        };
                    } catch (e) {

                    }

                }

                function copy(a) {
                    var ByteArrayOutputStream = Java.use('java.io.ByteArrayOutputStream').$new();
                    var ObjectOutputStream = Java.use('java.io.ObjectOutputStream').$new(ByteArrayOutputStream);
                    ObjectOutputStream.writeObject(a);
                    var ByteArrayInputStream = Java.use('java.io.ByteArrayInputStream').$new(ByteArrayOutputStream.toByteArray());
                    return Java.use('java.io.ObjectInputStream').$new(ByteArrayInputStream);
                }

                function copyDatToSdcard(src, des) {
                    var file = Java.use("java.io.File").$new(des);
                    if (file.exists())
                        return;
                    var FileOutputStream = Java.use("java.io.FileOutputStream").$new(des);
                    var buffer = Java.array('byte', new Array(4096).fill(0));
                    var InputStream = res.openRawResource(src);
                    for (var n; (n = InputStream.read(buffer)) !== -1;) {
                        FileOutputStream.write(buffer, 0, n);
                    }
                    InputStream.close();
                    FileOutputStream.close();
                }

                function print(element) {
                    if(element.class===undefined) {
                        console.log(element);
                        return;
                    }
                    var className = element.class.getName();
                    if (className === "java.util.List") {
                        try{
                            element = Java.cast(element, Java.use("java.util.Arrays$ArrayList"));
                        }catch (e) {
                            element = Java.cast(element, Java.use("java.util.ArrayList"));
                        }
                    }
                    if (className === "java.util.List" || className === "android.util.SparseArray") {
                        for (var index = 0; index < element.size(); index++) {
                            var info = Java.cast(element.get(index), Java.use(element.get(index).class.getName()));
                            console.log("****** The index " + index + " ******");
                            printInfo(info);
                        }
                    } else {
                        printInfo(element);
                    }

                }

                function printInfo(element) {
                    console.log("\t=================== methods ===================\n");
                    printInfoMethods(element);
                    console.log("\t=================== fields ===================\n");
                    printInfoFields(element);
                }

                function printInfoMethods(element) {
                    var info = Java.cast(element, Java.use(element.class.getName()));

                    var className = element.class.getName();
                    var javaClass = Java.use(className);

                    var methods = javaClass.class.getDeclaredMethods();

                    for (var i = 0; i < methods.length; i++) {
                        var method = methods[i];
                        var parameterTypes = method.getParameterTypes();
                        var parameterCount = parameterTypes.length;
                        var returnType = method.getReturnType();
                        if (parameterCount === 0) {
                            try {
                                var methodName = method.getName();
                                var result = info[methodName]();
                                if(className===returnType.getName() || returnType.getName()==="java.lang.Object") {
                                    console.log("\t" + methodName + ": is Copy or Clone");
                                    continue;
                                }
                                if (returnType.isArray()) {
                                    console.log("\t" + methodName + ": is Array");
                                    continue;
                                }
                                console.log("\t" + methodName + ":\t\t\t\t\t" + result);
                            } catch (e) {

                            }
                        }
                    }
                }

                function printInfoFields(element) {
                    var info = Java.cast(element, Java.use(element.class.getName()));

                    var className = element.class.getName();
                    var javaClass = Java.use(className);

                    var fields = javaClass.class.getDeclaredFields();
                    for (var i = 0; i < fields.length; i++) {
                        var field = fields[i];
                        field.setAccessible(true); // ??????????????
                        var fieldName = field.getName();
                        var fieldType = field.getType();
                        try {
                            if (fieldType.isArray()) {
                                console.log("\t" + fieldName + ": is Array");
                                continue;
                            }
                            var result = field.get(element);
                            console.log("\t" + fieldName + ":\t\t\t\t\t" + result);
                        } catch (e) {

                        }
                    }
                }
                function bitmap2Path(new_bitmap, file_name) {
                    var FileOutputStream = Java.use("java.io.FileOutputStream").$new(save_path + "/" + file_name);
                    var OutputStream = Java.cast(FileOutputStream, Java.use("java.io.OutputStream"));
                    new_bitmap.compress(Java.use("android.graphics.Bitmap$CompressFormat").PNG.value, 100, OutputStream);
                    OutputStream.flush();
                    OutputStream.close();
                }

                function readTxt() {
                    var path = "/data/local/tmp/256_256.yuv";
                    var file = Java.use('java.io.File').$new(path);
                    var buffer = Java.array('byte', new Array(4096).fill(0));
                    var out = Java.use('java.io.ByteArrayOutputStream').$new();
                    var FileInputStream = Java.use('java.io.FileInputStream').$new(file);
                    for (var n; (n = FileInputStream.read(buffer)) !== -1;) {
                        out.write(buffer, 0, n);
                    }
                    FileInputStream.close();
                    return out.toByteArray();
                }
            }
        })
    })
}

setTimeout(main, 500);
