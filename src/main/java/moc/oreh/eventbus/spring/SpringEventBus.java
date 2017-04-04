package moc.oreh.eventbus.spring;

import moc.oreh.eventbus.EventBus;
import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by hero on 17-4-4.
 */
public class SpringEventBus extends EventBus implements BeanPostProcessor {

    public SpringEventBus() {
        super();
    }

    public SpringEventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        super(corePoolSize, maximumPoolSize, keepAliveTime);
    }

    /**
     * 加入事务判断逻辑
     * 若是异步事件且当前存在事务上下文,必须等当前事务commit成功才invokeSubscribers,
     * 否则就不invoke
     *
     * @param event
     * @param subscribers
     */
    protected void invokeSubscribers(Object event, LinkedList<Subscriber> subscribers) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            LinkedList<Subscriber> asyncSubscribers = new LinkedList<>();
            LinkedList<Subscriber> syncSubscribers = new LinkedList<>();
            TransactionSynchronizationManager.registerSynchronization(
                    new SpringTxSynchronization(asyncTaskExecutor, asyncSubscribers, event));
            TransactionSynchronizationManager.registerSynchronization(
                    new SpringTxSynchronization(syncTaskExecutor, syncSubscribers, event));
            for (Subscriber subscriber : subscribers) {
                if (subscriber.getMode() == SubscribeMode.ASYNC)
                    asyncSubscribers.addLast(subscriber);
                else if (subscriber.getMode() == SubscribeMode.SYNC)
                    syncSubscribers.addLast(subscriber);
                else
                    new EventTask(subscriber, event).run();
            }
        } else {
            super.invokeSubscribers(event, subscribers);
        }
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
                LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
                if (subscribers == null) {
                    subscribers = new LinkedList<Subscriber>();
                    retrieverCache.put(eventType, subscribers);
                }
                addSubscriber(new Subscriber(bean, method, subscribe.mode(), subscribe.priority()), subscribers);
            }
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private class SpringTxSynchronization extends TransactionSynchronizationAdapter {
        private ThreadPoolExecutor taskExecutor;
        private LinkedList<Subscriber> subscribers;
        private Object event;

        public void afterCommit() {
            for (Subscriber subscriber : subscribers) {
                taskExecutor.execute(new EventTask(subscriber, event));
            }
        }

        public SpringTxSynchronization(ThreadPoolExecutor taskExecutor, LinkedList<Subscriber> subscribers, Object event) {
            this.taskExecutor = taskExecutor;
            this.subscribers = subscribers;
            this.event = event;
        }
    }
}
