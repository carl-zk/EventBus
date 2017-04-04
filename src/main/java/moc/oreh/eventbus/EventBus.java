package moc.oreh.eventbus;

import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author hero on 17-4-2.
 */
public class EventBus {
    protected Map<Class, LinkedList<Subscriber>> retrieverCache = new ConcurrentHashMap<>(64);
    protected ThreadPoolExecutor syncTaskExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    protected ThreadPoolExecutor asyncTaskExecutor;

    public EventBus() {
        asyncTaskExecutor = new ThreadPoolExecutor(16, 32, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public EventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        // SubscribeMode.ASYNC
        asyncTaskExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * publish an event
     *
     * @param event an Event Object, can any customized class object
     */
    public void publish(Object event) {
        Class eventType = event.getClass();
        LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
        if (subscribers == null)
            return;
        invokeSubscribers(event, subscribers);
    }

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
                    new EventTask(subscriber, event).run();
                    break;
            }
        }
    }

    public void destroy() {
        retrieverCache = null;
        syncTaskExecutor.shutdown();
        asyncTaskExecutor.shutdown();
    }

    public final LinkedList<Subscriber> getSubscribers(Class eventType) {
        return retrieverCache.get(eventType);
    }

    public void addSubscriber(Object subscriber, Method handle, SubscribeMode mode, int priority, Class eventType) {
        LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
        if (subscribers == null) {
            subscribers = new LinkedList<>();
            retrieverCache.put(eventType, subscribers);
        }
        addSubscriber(new Subscriber(subscriber, handle, mode, priority), subscribers);
    }

    protected void addSubscriber(Subscriber subscriber, LinkedList<Subscriber> subscribers) {
        Subscriber guard = null;
        for (Subscriber item : subscribers) {
            if (subscriber.getPriority() <= item.getPriority()) {
                guard = item;
                break;
            }
        }
        int location = guard == null ? subscribers.size() : subscribers.indexOf(guard);
        subscribers.add(location, subscriber);
    }

    public void removeSubscriber() {
        // TODO
    }
}
