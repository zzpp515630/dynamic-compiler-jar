package me.zzpp.dynamic.core.handler;

import lombok.Builder;
import lombok.Data;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 动态编译实现接口
 *
 * @author zzpp
 */
public interface DynamicClassHandler {


    String REX_NAME = "class\\s*(.*)\\s*\\{";

    String PACKAGE_NAME = "package?(.*)\\;";

    /**
     * 将java源码编译并加载class
     *
     * @param javaCode java代码
     * @throws Exception
     */
    Class<?> loadClass(String javaCode) throws Exception;


    /**
     * 将java源码编译并加载class，自定义lib包路径
     *
     * @param classLibPaths 源码jar地址
     * @param javaCode   java代码
     * @return class
     * @return
     */
    Class<?> loadClass(List<String> classLibPaths, String javaCode);


    /**
     * 将java源码编译并加载class，自定义lib包路径
     *
     * @param classLibFile 依赖包目录
     * @param javaCode  java代码
     * @return class
     * @return
     */
    Class<?> loadClass(File classLibFile, String javaCode);

    /**
     * 将java源码编译并加载class
     *
     * @param className className（同时也是classname），注意:className必须与javaCode中的className保持一致
     * @param javaCode  java代码
     * @return class
     * @throws Exception
     */
    Class<?> loadClass(String className, String javaCode) throws Exception;

    /**
     * 将java源码编译并加载class，自定义lib包路径
     *
     * @param className  className（同时也是classname），注意:className必须与javaCode中的className保持一致
     * @param classLibPaths 源码jar地址
     * @param javaCode   java代码
     * @return class
     */
    Class<?> loadClass(String className, List<String> classLibPaths, String javaCode);

    /**
     * 将java源码编译并加载class，自定义lib包路径
     *
     * @param className     className（同时也是classname），注意:className必须与javaCode中的className保持一致
     * @param classPathFile 源码lib目录
     * @param javaCode      java代码
     * @return class
     */
    Class<?> loadClass(String className, File classPathFile, String javaCode);

    /**
     * 无参方法执行,通过内部class缓存
     *
     * @param className
     * @param methodName
     * @return
     */
    Object invoke(String className, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;


    /**
     * 有参方法执行,通过内部class缓存.自动匹配参数类型，class必须完全相等
     *
     * @param className
     * @param methodName
     * @param args       demo : new Object[]{value}
     * @return
     */
    Object invoke(String className, String methodName, Object... args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;


    /**
     * 有参方法执行,通过内部class缓存.自动匹配参数类型，需要传方法的参数类型，方法的参数，两者必须完全匹配
     *
     * @param className
     * @param methodName
     * @param parameterTypes demo : new Class[]{Object.class}
     * @param args           demo : new Object[]{value}
     * @return
     */
    Object invoke(String className, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;


    /**
     * 无参方法执行，通过class对象
     *
     * @param clz
     * @param methodName
     * @return
     */
    Object invoke(Class<?> clz, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 有参方法执行,通过内部class实例.自动匹配参数类型，class必须完全相等
     *
     * @param clz
     * @param methodName
     * @param args       demo : new Object[]{value}
     * @return
     */
    Object invoke(Class<?> clz, String methodName, Object... args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 有参方法执行,通过内部class对象.自动匹配参数类型，需要传方法的参数类型，方法的参数，两者必须完全匹配
     *
     * @param clz
     * @param methodName
     * @param parameterTypes demo : new Class[]{Object.class}
     * @param args           demo : new Object[]{value}
     * @return
     */
    Object invoke(Class<?> clz, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 通用实力化方法 ，可以通过自定义方法构造，当方法为非静态有效
     *
     * @param clz             class对象
     * @param methodName      方法名
     * @param constructorArgs 构造参数
     * @param methodArgs      方法入参
     * @return
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     */
    Object invoke(Class<?> clz, String methodName, InvokeArgs constructorArgs, InvokeArgs methodArgs) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

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

    /**
     * 获取源码的package名称
     *
     * @param javaCode
     * @return
     */
    default String getPackageName(String javaCode) {
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
    default String getClassName(String packageName, String className) {
        if (null != packageName && !"".equals(packageName)) {
            return packageName + "." + className;
        } else {
            return className;
        }
    }

    enum CompilerType {

        Task,

        Javac,

        Cmd,

        ;
    }


    @Data
    @Builder
    class InvokeArgs {
        private Class<?>[] parameterTypes;

        private Object[] args;
    }
}

