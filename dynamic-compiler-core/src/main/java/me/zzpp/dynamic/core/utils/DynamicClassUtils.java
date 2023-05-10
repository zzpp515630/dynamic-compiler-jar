package me.zzpp.dynamic.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态编译工具类
 * @author zhangpeng
 * @create 2023/5/9 23:44
 */
public class DynamicClassUtils {


    static final String REX_NAME = "class[\\s*](.*?)[\\s*].*\\{?";
//    String REX_NAME = "class[\\s*](.*?)[\\s*]";

    static final  String PACKAGE_NAME = "package?(.*?);";

    /**
     * 替换className
     *
     * @param className className
     * @param javaCode  源码
     * @return 替换后的源码
     */
    public static  String replaceClassName(String className, String javaCode) {
        Pattern pattern = Pattern.compile(REX_NAME);
        Matcher matcher = pattern.matcher(javaCode);
        boolean find = matcher.find();
        if (find) {
            StringBuilder buffer = new StringBuilder();
            buffer.append(javaCode, 0, matcher.start(1));
            buffer.append(className);
            buffer.append(javaCode.substring(matcher.end(1)));
            return buffer.toString();
        }
        return javaCode;
    }

    /**
     * 获取源码的class名称
     *
     * @param javaCode
     * @return
     */
    public static String getClassName(String javaCode) {
        Pattern pattern = Pattern.compile(REX_NAME);
        Matcher matcher = pattern.matcher(javaCode);
        boolean find = matcher.find(0);
        if (find) {
            return matcher.group().replaceAll(REX_NAME, "$1").trim();
        }
        throw new RuntimeException("java code 不合法");
    }

    /**
     * 获取源码的package名称
     *
     * @param javaCode
     * @return
     */
    public static  String getPackageName(String javaCode) {
        Pattern pattern = Pattern.compile(PACKAGE_NAME);
        Matcher matcher = pattern.matcher(javaCode);
        boolean find = matcher.find(0);
        if (find) {
            return matcher.group().replaceAll(PACKAGE_NAME, "$1").trim();
        }
        return "";
    }


    /**
     * 获取className;
     *
     * @param packageName
     * @param className
     * @return
     */
    public static  String getClassName(String packageName, String className) {
        if (null != packageName && !"".equals(packageName)) {
            return packageName + "." + className;
        } else {
            return className;
        }
    }
}
