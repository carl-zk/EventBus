package moc.oreh.eventbus.support;

import java.util.LinkedList;

/**
 * Created by hero on 17-4-4.
 */
public class DefaultEventMulticaster extends EventMulticaster {

    /**
     * 默认不care事务上下文
     *
     * @param event
     * @param subscribers
     */
    protected void invokeSubscribers(Object event, LinkedList<Subscriber> subscribers) {
        for (Subscriber subscriber : subscribers) {
            switch (subscriber.getMode()) {
                case ASYNC:
                    asyncTaskExecutor.execute(new EventTask(subscriber, event));
                    break;
                case SYNC:
                    syncTaskExecutor.execute(new EventTask(subscriber, event));
                    break;
                case FOLLOW:
                    new EventTask(subscriber, event);
                    break;
            }
        }
    }

    public DefaultEventMulticaster() {
    }

    public DefaultEventMulticaster(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        super(corePoolSize, maximumPoolSize, keepAliveTime);
    }
}
