package moc.oreh.eventbus.support;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * publish helper
 * <p>
 * Created by hero on 17-4-3.
 */
public abstract class EventMulticaster {
    private Map<Class, LinkedList<Subscriber>> retrieverCache = new ConcurrentHashMap<>(64);
    protected ThreadPoolExecutor syncTaskExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    protected ThreadPoolExecutor asyncTaskExecutor;

    public EventMulticaster() {
        // SubscribeMode.ASYNC
        asyncTaskExecutor = new ThreadPoolExecutor(16, 32, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public EventMulticaster(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        // SubscribeMode.ASYNC
        asyncTaskExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public void multicastEvent(Object event) {
        Class eventType = event.getClass();
        LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
        if (subscribers == null)
            return;
        invokeSubscribers(event, subscribers);
    }

    protected abstract void invokeSubscribers(Object event, LinkedList<Subscriber> subscribers);

    public LinkedList<Subscriber> getSubscribers(Class eventType) {
        return retrieverCache.get(eventType);
    }

    public void setSubscribers(Class eventType, LinkedList<Subscriber> subscribers) {
        retrieverCache.put(eventType, subscribers);
    }

    public void destroy() {
        retrieverCache = null;
        syncTaskExecutor.shutdown();
        asyncTaskExecutor.shutdown();
    }

    public void addSubscriber(Subscriber subscriber) {

    }

    public void removeSubscriber(Subscriber subscriber) {

    }

    public void cleanSubscribers() {

    }
}
