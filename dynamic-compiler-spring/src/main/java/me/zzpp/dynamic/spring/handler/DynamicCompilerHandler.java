package me.zzpp.dynamic.spring.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.zzpp.dynamic.core.handler.DefaultDynamicClassHandlerImpl;
import me.zzpp.dynamic.core.handler.DynamicClassHandler;
import me.zzpp.dynamic.core.utils.NameConversionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;

/**
 * 动态编译bean接口实现
 * @author zzpp
 * @create 2023/1/18 18:01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicCompilerHandler implements DynamicBeanHandler {

    private static final DynamicClassHandler classHandler = new DefaultDynamicClassHandlerImpl();

    private final DynamicApplicationContext applicationContext;


    @Override
    public void loadBean(String javaCode) throws Exception {
        String className = classHandler.getClassName(javaCode);
        String beanName = NameConversionUtils.upperLowCaseHump(className);
        loadBean(beanName, javaCode);
    }

    @Override
    public void loadBean(String beanName, String javaCode) throws Exception {
        boolean hasBean = applicationContext.hasBean(beanName);
        if (hasBean) {
            log.error("the bean {} is already exists!", beanName);
            return;
        }
        Class<?> aClass = classHandler.loadClass(javaCode);
        log.info("loadBean，inject bean to IOC, {} start", beanName);
        applicationContext.registerBean(beanName, aClass);
        log.info("loadBean，inject bean to IOC, {} end", beanName);
    }

    @Override
    public Object invoke(String beanName, String methodName) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        boolean hasBean = applicationContext.hasBean(beanName);
        if (!hasBean) {
            log.error("the bean {} is not found!", beanName);
            return null;
        }
        try {
            Object bean = applicationContext.getBean(beanName);
            return MethodUtils.invokeExactMethod(bean, methodName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("execute method failed，{}::{}", beanName, methodName);
            log.error("execute method failed errMsg : {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Object invoke(String beanName, String methodName, Class<?>[] parameterTypes, Object[] args) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        boolean hasBean = applicationContext.hasBean(beanName);
        if (!hasBean) {
            log.error("the bean {} is not found!", beanName);
            return null;
        }
        try {
            Object bean = applicationContext.getBean(beanName);
            return MethodUtils.invokeExactMethod(bean, methodName, args, parameterTypes);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("execute method failed，{}::{}", beanName, methodName);
            log.error("execute method errMsg : {}", e.getMessage(), e);
            throw e;
        }
    }
}
