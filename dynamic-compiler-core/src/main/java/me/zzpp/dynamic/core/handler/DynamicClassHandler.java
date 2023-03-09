package me.zzpp.dynamic.core.handler;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface DynamicClassHandler {

    String REX_NAME = "class\\s*(.*)\\s*\\{";

    /**
     * @param javaCode java代码
     * @throws Exception
     */
    Class<?> loadClass(String javaCode) throws Exception;



    /**
     * @param classPaths 源码jar地址
     * @param javaCode  java代码
     * @return
     */
    Class<?> loadClass(List<String> classPaths, String javaCode);

    /**
     * @param className beanName（同时也是classname），注意:beanName必须与javaCode中的className保持一致
     * @param javaCode  java代码
     * @throws Exception
     */
    Class<?> loadClass(String className, String javaCode) throws Exception;

    /**
     * @param className beanName（同时也是classname），注意:beanName必须与javaCode中的className保持一致
     * @param classPaths 源码jar地址
     * @param javaCode  java代码
     * @return
     */
    Class<?> loadClass(String className, List<String> classPaths, String javaCode);

    /**
     * 无参方法执行
     *
     * @param className
     * @param methodName
     * @return
     */
    Object invoke(String className, String methodName);

    /**
     * 有参方法执行
     *
     * @param className
     * @param methodName
     * @param parameterTypes demo : new Class[]{Object.class}
     * @param args           demo : new Object[]{value}
     * @return
     */
    Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;


    /**
     * 无参方法执行
     *
     * @param clz
     * @param methodName
     * @return
     */
    Object invoke(Class<?> clz, String methodName);

    /**
     * 有参方法执行
     *
     * @param clz
     * @param methodName
     * @param parameterTypes demo : new Class[]{Object.class}
     * @param args           demo : new Object[]{value}
     * @return
     */
    Object invoke(Class<?> clz, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;


    /**
     * 获取源码的class名称
     *
     * @param javaCode
     * @return
     */
    default String getClassName(String javaCode) {
        Pattern pattern = Pattern.compile(REX_NAME);
        Matcher matcher = pattern.matcher(javaCode);
        boolean find = matcher.find(0);
        if (find) {
            return matcher.group().replaceAll(REX_NAME, "$1").trim();
        }
        throw new RuntimeException("java code 不合法");
    }
}

