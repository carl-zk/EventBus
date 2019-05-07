package eventbus.spring;


import eventbus.EventBus;
import eventbus.spring.handler.EventHandler;
import eventbus.support.Subscriber;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.LinkedList;
import java.util.List;

/**
 * @author carl
 */
public class SpringEventBus extends EventBus implements BeanPostProcessor {
    private static List<EventHandler> handlers = new LinkedList<>();

    public SpringEventBus() {
        super();
    }

    public SpringEventBus(int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
        super(corePoolSize, maxPoolSize, keepAliveSeconds);
    }

    @Override
    protected void handleEvent(Object event, Subscriber subscriber) {
        handlers.forEach(handler -> {
            if (handler.supportSubscribeMode(subscriber.getMode())) {
                handler.handle(event, subscriber);
            }
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        super.processBean(bean);
        return bean;
    }

    @Override
    protected Class getTargetClass(Object bean) {
        if (AopUtils.isAopProxy(bean) || AopUtils.isCglibProxy(bean) || AopUtils.isJdkDynamicProxy(bean)) {
            return AopUtils.getTargetClass(bean);
        } else {
            return bean.getClass();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public static void register(EventHandler handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
    }
}
