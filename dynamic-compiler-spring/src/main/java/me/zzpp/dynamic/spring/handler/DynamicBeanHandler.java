package me.zzpp.dynamic.spring.handler;

import java.lang.reflect.InvocationTargetException;

public interface DynamicBeanHandler {


    /**
     * @param javaCode java代码
     * @throws Exception
     */
    void loadBean(String javaCode) throws Exception;

    /**
     * @param className beanName（同时也是classname），注意:beanName必须与javaCode中的className保持一致
     * @param javaCode  java代码
     * @throws Exception
     */
    void loadBean(String className, String javaCode) throws Exception;

    /**
     * 无参方法执行
     *
     * @param beanName
     * @param methodName
     * @return
     */
    Object invoke(String beanName, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

    /**
     * 有参方法执行
     *
     * @param beanName
     * @param methodName
     * @param parameterTypes demo : new Class[]{Object.class}
     * @param args           demo : new Object[]{value}
     * @return
     */
    Object invoke(String beanName, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

}

