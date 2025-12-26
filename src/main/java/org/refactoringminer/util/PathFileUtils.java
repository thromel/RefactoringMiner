package org.refactoringminer.util;

import gr.uom.java.xmi.Constants;

public class PathFileUtils {
    public static boolean isSupportedFile(String path){
        return path.endsWith(".java") || path.endsWith(".py") || path.endsWith(".kt") || path.endsWith(".cs") || path.endsWith(".ts");
    }

    public static boolean isJavaFile(String path){
        return path.endsWith(".java");
    }

    public static boolean isPythonFile(String path){
        return path.endsWith(".py");
    }

    public static boolean isKotlinFile(String path){
        return path.endsWith(".kt");
    }

    public static boolean isCSharpFile(String path){
        return path.endsWith(".cs");
    }

    public static boolean isTypeScriptFile(String path){
        return path.endsWith(".ts");
    }

    public static boolean isLangSupportedFile(String path){
        return isPythonFile(path) || isCSharpFile(path) || isTypeScriptFile(path);
    }

    public static Constants getLang(String path) {
        if (isJavaFile(path))
            return Constants.JAVA;
        else if (isPythonFile(path))
            return Constants.PYTHON;
        else if (isKotlinFile(path))
            return Constants.KOTLIN;
        else if (isCSharpFile(path))
            return Constants.CSHARP;
        else if (isTypeScriptFile(path))
            return Constants.TYPESCRIPT;
        return Constants.JAVA;
    }
}
