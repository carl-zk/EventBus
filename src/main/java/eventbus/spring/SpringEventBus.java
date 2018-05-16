package eventbus.spring;


import eventbus.EventBus;
import eventbus.support.EventTask;
import eventbus.support.Subscriber;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
     * @param subscriber
     */
    protected void handleEvent(Object event, Subscriber subscriber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            switch (subscriber.getMode()) {
                case ASYNC:
                    TransactionSynchronizationManager.registerSynchronization(
                            new SpringTxSynchronization(asyncExecutor, subscriber, event));
                    break;
                case BACKGROUND:
                    TransactionSynchronizationManager.registerSynchronization(
                            new SpringTxSynchronization(backgroundExecutor, subscriber, event));
                    break;
            }
        } else {
            super.handleEvent(event, subscriber);
        }
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        super.processBean(bean);
        return bean;
    }

    protected Class getTargetClass(Object bean) {
        if (AopUtils.isAopProxy(bean) || AopUtils.isCglibProxy(bean) || AopUtils.isJdkDynamicProxy(bean)) {
            return AopUtils.getTargetClass(bean);
        } else {
            return bean.getClass();
        }
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    private class SpringTxSynchronization extends TransactionSynchronizationAdapter {
        private ThreadPoolExecutor executor;
        private Subscriber subscriber;
        private Object event;

        public void afterCommit() {
            executor.execute(new EventTask(event, subscriber));
        }

        public SpringTxSynchronization(ThreadPoolExecutor executor, Subscriber subscriber, Object event) {
            this.executor = executor;
            this.subscriber = subscriber;
            this.event = event;
        }
    }
}
