package moc.oreh.eventbus.spring;

import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventMulticaster;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by hero on 17-4-4.
 */
public class SpringEventMulticaster extends EventMulticaster {
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
            for (Subscriber subscriber : subscribers) {
                if (subscriber.getMode() == SubscribeMode.ASYNC)
                    asyncTaskExecutor.execute(new EventTask(subscriber, event));
                else if (subscriber.getMode() == SubscribeMode.SYNC)
                    syncTaskExecutor.execute(new EventTask(subscriber, event));
                else
                    new EventTask(subscriber, event);
            }
        }
    }

    public SpringEventMulticaster() {
        super();
    }

    public SpringEventMulticaster(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        super(corePoolSize, maximumPoolSize, keepAliveTime);
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
