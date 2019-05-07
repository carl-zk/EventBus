package eventbus.spring;


import eventbus.EventBus;
import eventbus.spring.handler.AsyncEventHandler;
import eventbus.spring.handler.BackgroundEventHandler;
import eventbus.spring.handler.EventHandler;
import eventbus.spring.handler.SyncEventHandler;
import eventbus.support.Subscriber;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

/**
 * @author carl
 */
@Component
public class SpringEventBus extends EventBus implements BeanPostProcessor {
    private List<EventHandler> handlers = new LinkedList<>();

    public SpringEventBus() {
        super();
        registerHandlers();
    }

    public SpringEventBus(int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
        super(corePoolSize, maxPoolSize, keepAliveSeconds);
        registerHandlers();
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

    private void registerHandlers() {
        AsyncEventHandler asyncEventHandler = new AsyncEventHandler(asyncExecutor);
        SyncEventHandler syncEventHandler = new SyncEventHandler();
        BackgroundEventHandler backgroundEventHandler = new BackgroundEventHandler(backgroundExecutor);
        handlers.add(asyncEventHandler);
        handlers.add(syncEventHandler);
        handlers.add(backgroundEventHandler);
    }
}
