package forwardexec;

import base.GlobalStatistics;
import base.ParameterTransferStmt;
import graph.CallGraph;
import graph.DGraph;
import graph.InterfaceGraph;
import order.OrderMethod;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;
import soot.jimple.internal.JCastExpr;
import utility.EntryPointsUtility;
import utility.InstanceUtility;
import utility.Logger;

import java.util.*;

public class SimulateEngine extends AbstractStmtSwitch {
    DGraph dg;
    StmtPath spath;
    ArrayList<String> javaScript = new ArrayList<>();
    HashSet<Value> variable = new HashSet<>();
    HashSet<String> leftString = new HashSet<>();
    ScriptGenerate scriptGenerate = ScriptGenerate.getInstance();

    public SimulateEngine(DGraph dg, StmtPath spath) {
        this.dg = dg;
        this.spath = spath;
    }

    public StmtPath getSpath() {
        return spath;
    }


    public HashSet<Value> getVariable() {
        return variable;
    }

    public HashSet<String> getLeftString() {
        return leftString;
    }

    public ArrayList<String> getJavaScript() {
        return javaScript;
    }

    public void writeJavaScript(Integer hash) {
        ArrayList<String> result = new ArrayList<>();
        result.add("            try {");
        result.add(String.format("                console.log(\"Start ============= %s\")\n", hash));
        for (String s : getJavaScript()) {
            result.add(String.format("                %s\n", s));
        }
        result.add("            } catch (e) {\n" + "                console.log(\"****\"+e+\"****\");\n"
                + "            }");
        if (result.size() == 2)
            return;
        scriptGenerate.addScripts(result);
    }

    public void simulate() {
        Unit lastUnit = getSpath().getStmtPathTail();
        // 这里是指令顺序
        for (Stmt stmt : getSpath().getStmtPath()) {
            if (scriptGenerate.containsStmt(stmt))
                continue;
            scriptGenerate.addStmt(stmt);
            Logger.print("[SIMULATE]" + this.hashCode() + ": " + stmt + " " + stmt.getClass());
            // 此处应该是对遇到的调用进行处理
            if (stmt instanceof ParameterTransferStmt) {
                caseAssignStmt((AssignStmt) stmt);
            } else {
                stmt.apply(this);
            }
            if (stmt.equals((Stmt) lastUnit)) {
                if (stmt instanceof AssignStmt) {
                    Value leftop = ((AssignStmt) stmt).getLeftOp();
                    Value rightop = ((AssignStmt) stmt).getRightOp();
                    String methName = "unknown";
                    if (rightop instanceof VirtualInvokeExpr) {
                        methName = ((VirtualInvokeExpr) rightop).getMethod().getName();
                    } else if (rightop instanceof SpecialInvokeExpr) {
                        methName = ((SpecialInvokeExpr) rightop).getMethod().getName();
                    } else if (rightop instanceof StaticInvokeExpr) {
                        methName = ((StaticInvokeExpr) rightop).getMethod().getName();
                    } else if (rightop instanceof InterfaceInvokeExpr) {
                        methName = ((InterfaceInvokeExpr) rightop).getMethod().getName();
                    }
//                    if (leftop.getType().toString().equals("java.util.List") || leftop.getType().toString().equals("android.util.SparseArray")) {
//                        if (leftop.getType().toString().equals("java.util.List")) {
//                            getJavaScript().add(String.format("s%s = Java.cast(s%s, Java.use(\"java.util.Arrays$ArrayList\"))",
//                                    leftop.hashCode(), leftop.hashCode()));
//                        }
////                        Type instr = InstanceUtility.getListType(stmt);
//                        getJavaScript().add("// only for information collection");
//                        getJavaScript().add(String.format("for (var index = 0; index < s%s.size(); index++) {",
//                                leftop.hashCode()));
//                        getJavaScript().add(String.format("    var info = Java.cast(s%s.get(index), Java.use(s%s.get(index).getClass().getName()));",
//                                leftop.hashCode(), leftop.hashCode()));
//                        getJavaScript().add("    console.log(\"****** The index \"+index+ \" ******\");");
//                        getJavaScript().add("    console.log(\"\\t=================== methods ===================\\n\");");
//                        getJavaScript().add("    printInfoMethods(info);");
//                        getJavaScript().add("    console.log(\"\\t=================== fields ===================\\n\");");
//                        getJavaScript().add("    printInfoFields(info);");
////                        if (instr != null) {
////                            SootClass convert = InstanceUtility.getSootClassByType(instr);
////                            for (SootMethod smthd : convert.getMethods()) {
////                                if (smthd.isConstructor() || smthd.getParameterCount() > 0)
////                                    continue;
////                                getJavaScript().add(String.format("    console.log(\"\\t %s	------>	\",info.%s());",
////                                        smthd.getName(), smthd.getName()));
////                            }
////                        }
//                        getJavaScript().add("}");
//                    }
                    getJavaScript().add(String.format("print(s%s);",
                            leftop.hashCode()));
                    getJavaScript().add(String.format("console.log(\"The results of function %s is	------>	\",s%s);",
                            methName, leftop.hashCode()));

                }
            }
        }
    }

    @Override
    public void caseInvokeStmt(InvokeStmt stmt) {
        // TODO Auto-generated method stub
        SootMethod msig = stmt.getInvokeExpr().getMethod();
        InvokeExpr vie = stmt.getInvokeExpr();
        Value base = InstanceUtility.getBaseInvoke(stmt);
        String result = setInvokeStmtString(base, vie, msig);
        getJavaScript().add(result);
    }

    public String setInvokeStmtString(Value base, InvokeExpr vie, SootMethod msig) {
        String result = "";
        SootClass sootClass = vie.getMethod().getDeclaringClass();
        boolean isActivity = InstanceUtility.isType(sootClass, "Activity");
        boolean isView = OrderMethod.getViewRec().containsKey(sootClass);
        if ((isView || isActivity) && !(scriptGenerate.getActivity().contains(sootClass))) {
            getJavaScript().add(String.format("startActivity(launch, '%s');", sootClass.getName()));
            getJavaScript().add("await sleep2(1000);");
            getJavaScript().add(String.format("var s%s = activityCls;", sootClass.hashCode()));
            scriptGenerate.addActivity(sootClass);
        }
        if (isActivity && (new EntryPointsUtility().getLifecycleMethods(vie.getMethod().getDeclaringClass()).contains(vie.getMethod()) || vie.getMethod().isConstructor()))
            return "";

        if (vie.getMethod().getSignature().equals("<android.view.View: int getWidth()>")) {
            result = String.format("%s", "640");
            GlobalStatistics.getInstance().countTerminal();
            return result;
        }
        if (vie.getMethod().getSignature().equals("<android.view.View: int getHeight()>")) {
            result = String.format("%s", "480");
            GlobalStatistics.getInstance().countTerminal();
            return result;
        }
        if (vie.getMethod().getSignature().equals("<android.content.ContextWrapper: java.io.File getFilesDir()>")) {
            result = String.format("%s.getFilesDir()", "context");
            GlobalStatistics.getInstance().countTerminal();
            return result;
        }
        if (base != null) {
            if (msig.isConstructor()) {
                result = String.format("s%s = s%s.", base.hashCode(), base.hashCode());
            } else {
                if (isActivity || isView) {
                    result = String.format("s%s.", sootClass.hashCode());
                } else if (base.toString().equals("r0")) {
                    Integer r0 = scriptGenerate.getBaseHash().get(base.getType().toString());
                    if (r0 == null)
                        r0 = base.hashCode();
                    result = String.format("s%s.", r0);
                } else if (base.getType().toString().equals("android.content.Context")) {
                    result = String.format("context.", base.hashCode());
                } else {
                    SootClass baseClass = InstanceUtility.getSootClass(base.getType().getEscapedName());
                    if (!baseClass.declaresMethod(vie.getMethod().getSubSignature())) {
                        String cast = String.format("s%s = Java.cast(s%s,Java.use('%s'));", base.hashCode(), base.hashCode(), baseClass.getSuperclass());
                        getJavaScript().add(cast);
                    }
                    result = String.format("s%s.", base.hashCode());
                }

            }

        } else {
            result = String.format("Java.use('%s').", msig.getDeclaringClass().getName());
        }
        if (msig.isConstructor()) {
            result = result + "$new(";
            scriptGenerate.getBaseHash().remove(msig.getDeclaringClass().getName());
            scriptGenerate.getBaseHash().put(msig.getDeclaringClass().getName(), base.hashCode());
        } else {
            if (msig.getSubSignature().equals("int length()")) {
                return result + "length";
            }
            result = result + msig.getName() + "(";
        }
        if (vie.getArgCount() != 0) {
            int count = 0;
            // To get the callback info
            for (int index = 0; index < vie.getMethod().getParameterCount(); index++) {
                Type type = vie.getMethod().getParameterType(index);
                SootClass sc = InstanceUtility.getSootClassByType(type);
                if (sc != null && sc.isInterface()) {
                    for (SootMethod sootMethod : sc.getMethods()) {
                        String extra = String.format("hookCallback(s%s.class.getName(), \"%s\")",
                                vie.getArg(index).hashCode(),sootMethod.getName());
                        getJavaScript().add(extra);
                    }
                }
            }
            for (Value arg : vie.getArgs()) {
                if (arg instanceof Local) {
                    if (!arg.getType().toString().equals("android.app.Activity")
                            && !arg.getType().toString().equals("android.content.Context")
                            && !InstanceUtility.isType(Scene.v().getSootClassUnsafe(arg.getType().toString()), "Activity")
                            && !InstanceUtility.isType(Scene.v().getSootClassUnsafe(arg.getType().toString()), "Application") && !OrderMethod.getViewRec().containsKey(InstanceUtility.getSootClassByType(arg.getType()))) {
                        if (arg.getType().toString().equals("java.lang.String")) {
                            if (!vie.getMethod().getParameterType(count).toString().equals("java.lang.String")) {
                                result = result + String.format("Java.use('java.lang.String').$new(s%s.toString()),",
                                        arg.hashCode());

                            } else {
                                result = result + String.format("s%s,", arg.hashCode());
                            }

                        } else if (vie.getMethod().getParameterType(count).toString().equals("java.lang.String")
                                && !arg.getType().toString().equals("java.lang.String")) {
                            result = result + String.format("Java.use('java.lang.String').$new(s%s.toString()),",
                                    arg.hashCode());
                        } else {
                            result = result + String.format("s%s,", arg.hashCode());
                        }
                    } else {
                        GlobalStatistics.getInstance().countTerminal();
                        SootClass argCls = InstanceUtility.getSootClassByType(arg.getType());
                        if (arg.getType().toString().equals("android.app.Activity")) {
                            result = result + String.format("%s,", "launch");
                        } else if (((InstanceUtility.isType(argCls, "Activity") && argCls.isApplicationClass()) || OrderMethod.getViewRec().containsKey(argCls)) && !(scriptGenerate.getActivity().contains(sootClass))) {
                            getJavaScript().add("try {");
                            getJavaScript().add(String.format("startActivity(launch, '%s');", OrderMethod.getViewRec().getOrDefault(argCls, argCls)));
                            getJavaScript().add("await sleep2(1000);");
                            sootClass = InstanceUtility.getSootClassByType(arg.getType());
                            getJavaScript().add(String.format("var s%s = activityCls;", sootClass.hashCode()));
                            if (OrderMethod.getViewRec().containsKey(argCls)) {
                                getJavaScript().add(String.format("var s%s = getClass('%s');", sootClass.hashCode(), sootClass.getName()));
                            }
                            // some activity can't be called
                            getJavaScript().add(String.format("} catch (e) {\n" +
                                    "                        s%s = context;\n" +
                                    "                    }",sootClass.hashCode()));
                            result = result + String.format("s%s,", sootClass.hashCode());
                            scriptGenerate.addActivity(sootClass);
                        } else
                            result = result + String.format("%s,", "context");
                    }

                } else if (arg instanceof Constant) {
                    String constant = ConstantSet(vie.getMethod().getParameterType(count), arg);
                    result = result + String.format("%s,", constant);
                } else {
                    result = result + String.format("%s+++++++++,", arg);
                }
                count++;
            }
            result = result.substring(0, result.length() - 1);
        }
        result = result + ");";
        return result;
    }

    public void concatenateString(Value leftop, String jsCode, String leftName) {
        getLeftString().add(leftName);
        Value tmp = leftop;
        if (leftop instanceof ArrayRef) {
            leftop = ((ArrayRef) leftop).getBase();
        }
        String result = "";
        if ((getVariable().contains(leftop) || ((leftop instanceof InstanceFieldRef)
                && !((InstanceFieldRef) leftop).getBase().toString().equals("r0")) &&
                !(((InstanceFieldRef) leftop).getBase().toString().equals("$r0")))
//              !(((InstanceFieldRef) leftop).getBase().toString().equals("$r0") && InstanceUtility.getSootClass(((InstanceFieldRef) leftop).getBase().getType().toString()).isInnerClass()))
                || leftop instanceof StaticFieldRef) {
            result = jsCode;
        } else {
            result = String.format("var %s", jsCode);
            getVariable().add(leftop);
        }
        if (getJavaScript().contains("var " + result))
            return;
        // 这里的目的是啥？
        if (leftop.getType() instanceof ArrayType && !(tmp instanceof ArrayRef)) {
            ArrayType arrayType = (ArrayType) leftop.getType();
            result = String.format("%s = Java.array('%s',%s);", result.split(" = ")[0], arrayType.getElementType(),
                    result.split(" = ")[1].split(";")[0]);
        }
        getJavaScript().add(result);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void caseAssignStmt(AssignStmt stmt) {
        // TODO Auto-generated method stub
        int hashCode;
//        if(stmt.toString().equals("r0.<com.ophotovideoapps.facenhancer.beautyeditor.FaceTuneEditor$c: com.ophotovideoapps.facenhancer.beautyeditor.FaceTuneEditor b> = $r3"))
//            System.out.println();
        Value leftop = stmt.getLeftOp();
        Value rightop = stmt.getRightOp();
        // 局部变量、参数类型、数组类型
        String result = "";
        // 9.22 这里有报错？
        if (leftop == null)
            return;
        if (leftop.equals(rightop)) {
            String str = leftop.getType().toString();
            GlobalStatistics.getInstance().countTerminal();
            if (str.equals("android.graphics.Bitmap")) {
                result = String.format("var s%s = bitmap;", leftop.hashCode());
            } else if (str.equals("android.graphics.Point")) {
                result = String.format("var s%s = point;", leftop.hashCode());
            } else if (str.equals("android.content.res.AssetManager")) {
                result = String.format("var s%s = assets;", leftop.hashCode());
            } else if (str.equals("android.view.Display")) {
                result = String.format("var s%s = display;", leftop.hashCode());
            } else if (str.equals("android.content.res.Resources")) {
                result = String.format("var s%s = res;", leftop.hashCode());
            } else if (str.equals("android.app.Application")) {
                result = String.format("var s%s = application;", leftop.hashCode());
            } else if (rightop.getType().toString().equals("byte[]")) {
                result = String.format("var s%s = readTxt();", rightop.hashCode());
            }
            getJavaScript().add(result);
        } else if (leftop instanceof Local || leftop instanceof ParameterRef) {
            String leftName = String.format("s%s", leftop.hashCode());
            if (rightop instanceof InvokeExpr) {
                InvokeExpr vie = (InvokeExpr) rightop;
                SootMethod msig = vie.getMethod();
                Value base = InstanceUtility.getBaseInvoke(stmt);
                String right = setInvokeStmtString(base, vie, msig);
                concatenateString(leftop, String.format("%s = %s", leftName, right), leftName);
            } else if (rightop instanceof NewExpr) {
                String className = rightop.getType().toString();
                concatenateString(leftop, String.format("%s = Java.use('%s');", leftName, className), leftName);
                // 这里要设置r0的情况
            } else if (rightop instanceof FieldRef) {
                FieldRef field = ((FieldRef) rightop);
                // && !((InstanceFieldRef) rightop).getBase().toString().equals("r0")
                if (rightop instanceof InstanceFieldRef) {
                    SootClass cls = Scene.v().getSootClass(((InstanceFieldRef) rightop).getBase().getType().toString());
                    String fieldName = ((InstanceFieldRef) rightop).getField().getName();
                    for (SootMethod smthd : cls.getMethods()) {
                        if (smthd.getName().equals(fieldName)) {
                            fieldName = "_" + fieldName;
                            break;
                        }
                    }
                    if (field.getField().getSignature().equals("<android.hardware.Camera$Size: int width>")) {
                        concatenateString(leftop, String.format("%s = bitmap.getWidth();", leftName), leftName);
                        GlobalStatistics.getInstance().countTerminal();
                    } else if (field.getField().getSignature().equals("<android.hardware.Camera$Size: int height>")) {
                        concatenateString(leftop, String.format("%s = bitmap.getHeight();", leftName), leftName);
                        GlobalStatistics.getInstance().countTerminal();
                    } else if (field.getField().getSignature().equals("<android.graphics.Point: int x>")) {
                        concatenateString(leftop, String.format("%s = point.x.value;", leftName), leftName);
                        GlobalStatistics.getInstance().countTerminal();
                    } else if (field.getField().getSignature().equals("<android.graphics.Point: int y>")) {

                        concatenateString(leftop, String.format("%s = point.y.value;", leftName), leftName);
                        GlobalStatistics.getInstance().countTerminal();
                    } else {
                        if (((InstanceFieldRef) rightop).getBase().toString().equals("r0")
                                || (((InstanceFieldRef) rightop).getBase().toString().equals("$r0"))) {
//                        if (((InstanceFieldRef) rightop).getBase().toString().equals("r0")
//                                || (((InstanceFieldRef) rightop).getBase().toString().equals("$r0") && InstanceUtility.getSootClass(((InstanceFieldRef) rightop).getBase().getType().toString()).isInnerClass())) {
                            concatenateString(leftop, String.format("%s = s%s;", leftName,
                                    Math.abs(rightop.toString().hashCode())), leftName);
                        } else {
                            concatenateString(leftop, String.format("%s = s%s.%s.value;", leftName,
                                    ((InstanceFieldRef) rightop).getBase().hashCode(), fieldName), leftName);
                        }
                        Integer id = CallGraph.getRawResourceIdIfHave(field.getField().toString());
                        if (id != 0) {
                            getJavaScript().add(String.format("copyDatToSdcard(%s,s%s);", id, leftop.hashCode()));
                        }
                    }

                } else if (rightop instanceof StaticFieldRef) {
                    SootClass cls = ((StaticFieldRef) rightop).getField().getDeclaringClass();
                    String fieldName = ((StaticFieldRef) rightop).getField().getName();
                    for (SootMethod smthd : cls.getMethods()) {
                        if (smthd.getName().equals(fieldName)) {
                            fieldName = "_" + fieldName;
                            break;
                        }

                    }
                    concatenateString(leftop, String.format("%s = Java.use('%s').%s.value;", leftName,
                            ((StaticFieldRef) rightop).getField().getDeclaringClass(), fieldName), leftName);
                } else {
                    concatenateString(leftop, String.format("%s = s%s;", leftName,
                            Math.abs(field.getField().getSignature().hashCode())), leftName);
                }
            } else if (rightop instanceof Local) {
                concatenateString(leftop, String.format("%s = s%s;", leftName, rightop.hashCode()), leftName);
                if (leftop.getType() != rightop.getType() && rightop.getType().toString().equals("java.lang.String")) {
                    concatenateString(leftop,
                            String.format("%s = s%s.toString();", leftName, leftop.hashCode()), leftName);
                }

            } else if (rightop instanceof JCastExpr) {
                handleRightOpCast(leftop, rightop, leftName);
            } else if (rightop instanceof NewArrayExpr) {
                NewArrayExpr arr = (NewArrayExpr) rightop;
                String size = arr.getSize().toString();
                if (arr.getSize() instanceof Local)
                    size = String.format("s%s", arr.getSize().hashCode());
                String type = ((ArrayType) arr.getType()).getElementType().toString();
                String fill = "0";
                if (((ArrayType) arr.getType()).getElementType() instanceof RefType) {
                    fill = "null";
                }
                concatenateString(leftop, String.format("%s = Java.array('%s', new Array(%s).fill(%s));",
                        leftName, type, size, fill), leftName);
            } else if (rightop instanceof Constant) {
                String constant = ConstantSet(leftop.getType(), rightop);
                if (!getLeftString().contains(leftName) || !constant.equals("null"))
                    concatenateString(leftop, String.format("%s = %s;", leftName, constant), leftName);
            } else if (rightop instanceof JArrayRef) {
                JArrayRef array = ((JArrayRef) rightop);
                String index = String.format("s%s", array.getIndex().hashCode());
                if (array.getIndex() instanceof Constant)
                    index = String.format("%s", array.getIndex());
                concatenateString(leftop,
                        String.format("%s = s%s[%s];", leftName, array.getBase().hashCode(), index), leftName);
            } else if (rightop instanceof AddExpr) {
                AddExpr add = (AddExpr) rightop;
                calculate(leftop, "+", add.getOp1(), add.getOp2());
            } else if (rightop instanceof DivExpr) {
                DivExpr div = (DivExpr) rightop;
                calculate(leftop, "/", div.getOp1(), div.getOp2());
            } else if (rightop instanceof MulExpr) {
                MulExpr mul = (MulExpr) rightop;
                calculate(leftop, "*", mul.getOp1(), mul.getOp2());
            } else if (rightop instanceof SubExpr) {
                SubExpr sub = (SubExpr) rightop;
                calculate(leftop, "-", sub.getOp1(), sub.getOp2());
            } else if (rightop instanceof ShlExpr) {
                ShlExpr shl = (ShlExpr) rightop;
                calculate(leftop, ">", shl.getOp1(), shl.getOp2());
            } else if (rightop instanceof RemExpr) {
                RemExpr rem = (RemExpr) rightop;
                calculate(leftop, "%", rem.getOp1(), rem.getOp2());
            } else if (rightop instanceof LengthExpr) {
                LengthExpr length = (LengthExpr) rightop;
                calculate(leftop, "L", length.getOp(), null);
            } else if (rightop instanceof OrExpr) {
                OrExpr or = (OrExpr) rightop;
                calculate(leftop, "|", or.getOp1(), or.getOp2());
            } else if (rightop instanceof AndExpr) {
                AndExpr and = (AndExpr) rightop;
                calculate(leftop, "&", and.getOp1(), and.getOp2());
            } else if (rightop instanceof NegExpr) {
                NegExpr neg = (NegExpr) rightop;
                calculate(leftop, "-", neg.getOp(), null);
            } else {
                if (rightop != null)
                    Logger.printW(String.format("[%s] [SIMULATE][right unknown]: %s (%s)", this.hashCode(), stmt,
                            rightop.getClass()));
            }
        } else if (leftop instanceof ArrayRef) {
            JArrayRef array = ((JArrayRef) leftop);
            String leftName = String.format("s%s[%s]", array.getBase().hashCode(), array.getIndex());
            if (rightop instanceof Local) {
                concatenateString(leftop, String.format("%s = s%s;", leftName,
                        rightop.hashCode()), leftName);
                if (array.getBase().getType() != rightop.getType()
                        && rightop.getType().toString().equals("java.lang.String")) {
                    concatenateString(leftop, String.format("%s = s%s[%s].toString();", leftName, array.getBase().hashCode(), array.getIndex()), leftName);
                }
            } else if (rightop instanceof JCastExpr) {
                handleRightOpCast(leftop, rightop, leftName);
            } else if (rightop instanceof Constant) {

                String constant = ConstantSet(leftop.getType(), rightop);
                if (!getLeftString().contains(leftName) || !constant.equals("null"))
                    concatenateString(leftop, String.format("%s = %s;", leftName, constant), leftName);
            }
        } else if (leftop instanceof FieldRef) {
            String leftName = "";
            if (leftop instanceof StaticFieldRef) {
                SootClass cls = ((StaticFieldRef) leftop).getField().getDeclaringClass();
                String fieldName = ((StaticFieldRef) leftop).getField().getName();
                for (SootMethod smthd : cls.getMethods()) {
                    if (smthd.getName().equals(fieldName)) {
                        fieldName = "_" + fieldName;
                        break;
                    }
                }
                leftName = String.format("Java.use('%s').%s.value",
                        ((StaticFieldRef) leftop).getField().getDeclaringClass(), fieldName);
                // && !((InstanceFieldRef) leftop).getBase().toString().equals("r0")
            } else if (leftop instanceof InstanceFieldRef) {
                FieldRef field = (FieldRef) leftop;
                SootClass cls = Scene.v().getSootClass(((InstanceFieldRef) leftop).getBase().getType().toString());
                String fieldName = ((InstanceFieldRef) leftop).getField().getName();
                for (SootMethod smthd : cls.getMethods()) {
                    if (smthd.getName().equals(fieldName)) {
                        fieldName = "_" + fieldName;
                        break;
                    }
                }
//				if (field.getField().getName().equals("this$0")) {
//					leftString = String.format("s%s", Math.abs(leftop.toString().hashCode()));
//				} else {
//					leftString = String.format("s%s.%s.value", ((InstanceFieldRef) leftop).getBase().hashCode(),
//							fieldName);
//				}
//                if (((InstanceFieldRef) leftop).getBase().toString().equals("r0")
//                        || (((InstanceFieldRef) leftop).getBase().toString().equals("$r0") && InstanceUtility.getSootClass(((InstanceFieldRef) leftop).getBase().getType().toString()).isInnerClass())) {
                if (((InstanceFieldRef) leftop).getBase().toString().equals("r0")
                        || (((InstanceFieldRef) leftop).getBase().toString().equals("$r0"))) {
                    leftName = String.format("s%s", Math.abs(leftop.toString().hashCode()));
                } else {
                    leftName = String.format("s%s.%s.value", ((InstanceFieldRef) leftop).getBase().hashCode(),
                            fieldName);
                }

            }
            if (rightop instanceof Constant) {
                String constant = ConstantSet(leftop.getType(), rightop);
                if (!getLeftString().contains(leftName) || !constant.equals("null"))
                    concatenateString(leftop, String.format("%s = %s;", leftName, constant), leftName);

            } else if (rightop instanceof JCastExpr) {
                handleRightOpCast(leftop, rightop, leftName);
            } else if (rightop instanceof FieldRef) {
                concatenateString(leftop, String.format("%s = s%s;", leftName, Math.abs(rightop.toString().hashCode())), leftName);
            } else if (InterfaceGraph.getInterfaceBy(InstanceUtility.getSootClassByType(leftop.getType())) != null && Objects.requireNonNull(InterfaceGraph.getInterfaceBy(InstanceUtility.getSootClassByType(leftop.getType()))).contains(InstanceUtility.getSootClassByType(rightop.getType()))) {
                concatenateString(leftop, String.format("%s = Java.cast(s%s,Java.use('%s'));", leftName, rightop.hashCode(), leftop.getType().toString()), leftName);
            } else {
                SootClass sootClass = InstanceUtility.getSootClassByType(rightop.getType());
                if (sootClass != null) {
                    boolean isActivity = InstanceUtility.isType(sootClass, "Activity");
                    boolean isView = OrderMethod.getViewRec().containsKey(sootClass);
                    if ((isView || isActivity) && !(scriptGenerate.getActivity().contains(sootClass))) {
                        getJavaScript().add(String.format("startActivity(launch, '%s');", sootClass.getName()));
                        getJavaScript().add("await sleep2(1000);");
                        getJavaScript().add(String.format("var s%s = activityCls;", rightop.hashCode()));
                        scriptGenerate.addActivity(sootClass);
                    }
                }
                concatenateString(leftop, String.format("%s = s%s;", leftName, rightop.hashCode()), leftName);

            }
            getJavaScript().add(result);
        } else {

            Logger.printW(
                    String.format("[%s] [SIMULATE][left unknown]: %s (%s)", this.hashCode(), stmt, leftop.getClass()));
        }

    }

    public void calculate(Value leftop, String cal, Value op1, Value op2) {
        String leftName = String.format("s%s", leftop.hashCode());
        if (cal.equals("L")) {
            concatenateString(leftop, String.format("%s = s%s.length;", leftName, op1.hashCode()), leftName);
            return;
        }
        String right = "";
        String left = String.format("s%s", op1.hashCode());
        if (op2 != null)
            right = String.format("s%s", op2.hashCode());
        if (op1 instanceof Constant)
            left = String.format("%s", ConstantSet(leftop.getType(), op1));
        if (op2 instanceof Constant)
            right = String.format("%s", ConstantSet(leftop.getType(), op2));
        concatenateString(leftop, String.format("%s = %s%s%s;", leftName, left, cal, right), leftName);
    }

    public String ConstantSet(Type leftop, Value rightop) {
        Type type;
        if (rightop instanceof FloatConstant) {
            return ((Constant) rightop).toString().replace("F", "");
        } else if (rightop instanceof DoubleConstant) {
            return ((Constant) rightop).toString().replace("D", "");

        } else if (rightop instanceof LongConstant) {
            return ((Constant) rightop).toString().replace("L", "");

        } else if (rightop instanceof IntConstant) {
            if (leftop.toString().equals("boolean")) {
                if (((Constant) rightop).toString().equals("0")) {
                    return "false";
                } else {
                    return "true";
                }
            } else {
                return ((Constant) rightop).toString();
            }
        } else if (rightop instanceof ClassConstant) {
            String className = ((ClassConstant) rightop).getValue().substring(1).replace('/', '.').replace(";", "");
            // 这里为什么要截取字符串的来着？
//            return String.format("Java.use('java.lang.Class').forName(\"%s\")",
//                    className.substring(1, className.length() - 1));
            return String.format("Java.use('java.lang.Class').forName(\"%s\")",
                    className);

        } else if (rightop instanceof StringConstant) {
            // unicode
            return String.format("Java.use('java.lang.String').$new(\"%s\")", ((StringConstant) rightop).value).replace("\n", "\\n");

        } else {
            return ((Constant) rightop).toString();
        }

    }

    @Override
    // 获得参数的值
    public void caseIdentityStmt(IdentityStmt stmt) {
        // TODO Auto-generated method stub
        Value leftop = ((IdentityStmt) stmt).getLeftOp();
        Value rightop = ((IdentityStmt) stmt).getRightOp();
        String result = "";
        int leftHashCode = Math.abs(leftop.hashCode());
        int rightHashCode = Math.abs(rightop.hashCode());
        if (leftop instanceof FieldRef) {
            leftHashCode = ((FieldRef) leftop).getField().getSignature().hashCode();
        }
        if (rightop instanceof FieldRef) {
            rightHashCode = ((FieldRef) rightop).getField().getSignature().hashCode();
        }
        if (rightop instanceof ThisRef) {
            rightHashCode = ((ThisRef) rightop).getType().toString().hashCode();
        }
        String leftName = String.format("s%s", leftHashCode);
        concatenateString(leftop, String.format("%s = s%s;", leftName, rightHashCode), leftName);
    }

    @Override
    public void defaultCase(Object obj) {
        // TODO Auto-generated method stub
        Logger.printW(String.format("[%s] [SIMULATE][Can't Handle]: %s (%s)", this.hashCode(), obj, obj.getClass()));
    }

    public void handleRightOpCast(Value leftop, Value rightop, String leftName) {
        String trans = "";
        switch (((JCastExpr) rightop).getCastType().toString()) {
            case "float":
                trans = "parseFloat";
                break;
            case "long":
                trans = "parseInt";
                break;
            case "int":
                trans = "parseInt";
                break;
            case "double":
                trans = "parseFloat";
                break;
            default:
                if (((JCastExpr) rightop).getType() instanceof ArrayType) {
                    concatenateString(leftop, String.format("s%s = s%s;", leftop.hashCode(), ((JCastExpr) rightop).getOp().hashCode()), leftName);
                    return;
                }
                trans = ((JCastExpr) rightop).getCastType().toString();
                concatenateString(leftop, String.format("%s = Java.cast(s%s, Java.use('%s'));", leftName,
                        ((JCastExpr) rightop).getOp().hashCode(), trans), leftName);
                return;
        }
        concatenateString(leftop,
                String.format("%s = %s(s%s);", leftName, trans, ((JCastExpr) rightop).getOp().hashCode()), leftName);
    }
}
