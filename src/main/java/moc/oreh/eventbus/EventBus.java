package moc.oreh.eventbus;

import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;
import moc.oreh.eventbus.util.Assert;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author hero on 17-4-2.
 */
public class EventBus {
    // TODO ConcurrentHashMap原理
    protected static Map<Class, LinkedList<Subscriber>> retrieverCache = new ConcurrentHashMap<>(64);
    protected static ThreadPoolExecutor syncTaskExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    protected static ThreadPoolExecutor asyncTaskExecutor;
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();
    private static final Lock r = lock.readLock();
    private static final Lock w = lock.writeLock();

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
        r.lock();
        try {
            Class eventType = event.getClass();
            LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
            if (subscribers == null)
                return;
            for (Subscriber subscriber : subscribers) {
                if (subscriber.getMode() == SubscribeMode.FOLLOW) {
                    new EventTask(subscriber, event).run();
                } else {
                    invokeSubscriber(event, subscriber);
                }
            }
        } finally {
            r.unlock();
        }
    }

    protected void invokeSubscriber(Object event, Subscriber subscriber) {
        switch (subscriber.getMode()) {
            case ASYNC:
                asyncTaskExecutor.execute(new EventTask(subscriber, event));
                break;
            case SYNC:
                syncTaskExecutor.execute(new EventTask(subscriber, event));
                break;
        }
    }

    /**
     * process a bean if a subscriber
     *
     * @param bean
     */
    public void processBean(Object bean) {
        Assert.notNull(bean, "bean must not be null");
        w.lock();
        try {
            Class clazz = proxyBeanUnwrap(bean);
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
                    addSubscriberInOrder(new Subscriber(bean, method, subscribe.mode(), subscribe.priority()), subscribers);
                }
            }
        } finally {
            w.unlock();
        }
    }

    protected Class proxyBeanUnwrap(Object bean) {
        // TODO https://www.ibm.com/developerworks/cn/java/j-lo-proxy1/
        return bean.getClass();
    }

    public void destroy() {
        w.lock();
        try {
            syncTaskExecutor.shutdown();
            asyncTaskExecutor.shutdown();
            while (syncTaskExecutor.isTerminating()) ;
            while (asyncTaskExecutor.isTerminating()) ;
            retrieverCache.clear();
        } finally {
            w.unlock();
        }
    }

    public void addSubscriber(Class eventType, Object subscriber, Method handle, SubscribeMode mode, int priority) {
        Assert.notNull(subscriber, "subscriber must not be null");
        Assert.notNull(handle, "subscriber's handle method must not be null");
        w.lock();
        try {
            LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
            if (subscribers == null) {
                subscribers = new LinkedList<>();
                retrieverCache.put(eventType, subscribers);
            }
            Subscriber candidate = new Subscriber(subscriber, handle, mode, priority);
            for (Subscriber listener : subscribers) {
                if (candidate.equals(listener))
                    throw new EventBusException("Subscriber " + subscriber.getClass() + " already subscribed to event "
                            + eventType);
            }
            addSubscriberInOrder(candidate, subscribers);
        } finally {
            w.unlock();
        }
    }

    protected void addSubscriberInOrder(Subscriber subscriber, LinkedList<Subscriber> subscribers) {
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
