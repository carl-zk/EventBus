package moc.oreh.eventbus.spring;

import moc.oreh.eventbus.EventBus;
import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.support.Subscriber;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * Created by hero on 17-4-4.
 */
public class SpringEventBus extends EventBus implements BeanPostProcessor, ApplicationContextAware {
    private static ApplicationContext context;

    public SpringEventBus() {
        eventMulticaster = new SpringEventMulticaster();
    }

    public SpringEventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        eventMulticaster = new SpringEventMulticaster(corePoolSize, maximumPoolSize, keepAliveTime);
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class clazz = bean.getClass();
        if (AopUtils.isAopProxy(bean) || AopUtils.isCglibProxy(bean) || AopUtils.isJdkDynamicProxy(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                Class[] event = method.getParameterTypes();
                if (event == null || event.length != 1)
                    throw new IllegalArgumentException("event object must one and only one: " + method.getName() + " in " + clazz.getName());
                Class eventType = event[0];
                LinkedList<Subscriber> subscribers = eventMulticaster.getSubscribers(eventType);
                if (subscribers == null) {
                    subscribers = new LinkedList<Subscriber>();
                    eventMulticaster.setSubscribers(eventType, subscribers);
                }
                subscribers.addLast(new Subscriber(bean, method, subscribe.mode()));
            }
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
