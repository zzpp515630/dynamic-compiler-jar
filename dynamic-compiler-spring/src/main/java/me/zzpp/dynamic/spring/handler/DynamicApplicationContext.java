package me.zzpp.dynamic.spring.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 动态编译获取spring环境
 * @author zzpp
 * @create 2023/1/19 9:10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DynamicApplicationContext {

    private final ApplicationContext applicationContext;


    public void registerBean(String beanName, Class<?> clz) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(clz).getBeanDefinition();
        load().registerBeanDefinition(beanName, beanDefinition);
    }

    public void remove(String beanName) {
        load().removeBeanDefinition(beanName);
    }

    public Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }


    public boolean hasBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    public Object getBean(Class<?> clz) {
        return applicationContext.getBean(clz);
    }

    private DefaultListableBeanFactory load() {
        return (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }


}
