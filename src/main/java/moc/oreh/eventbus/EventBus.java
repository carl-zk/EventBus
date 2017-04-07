package moc.oreh.eventbus;

import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;
import moc.oreh.eventbus.util.Assert;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
    protected static Map<Class, LinkedList<Subscriber>> retrieverCache = new HashMap<>(64);
    protected static ThreadPoolExecutor syncTaskExecutor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    protected static ThreadPoolExecutor asyncTaskExecutor;
    private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();
    private static final Lock READ_LOCK = READ_WRITE_LOCK.readLock();
    private static final Lock WRITE_LOCK = READ_WRITE_LOCK.writeLock();

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
        READ_LOCK.lock();
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
            READ_LOCK.unlock();
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
        WRITE_LOCK.lock();
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
            WRITE_LOCK.unlock();
        }
    }

    protected Class proxyBeanUnwrap(Object bean) {
        // TODO https://www.ibm.com/developerworks/cn/java/j-lo-proxy1/
        return bean.getClass();
    }

    public void destroy() {
        WRITE_LOCK.lock();
        try {
            syncTaskExecutor.shutdown();
            asyncTaskExecutor.shutdown();
            while (syncTaskExecutor.isTerminating()) ;
            while (asyncTaskExecutor.isTerminating()) ;
            retrieverCache.clear();
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    public void addSubscriber(Class eventType, Object subscriber, Method handle, SubscribeMode mode, int priority) {
        Assert.notNull(subscriber, "subscriber must not be null");
        Assert.notNull(handle, "subscriber's handle method must not be null");
        WRITE_LOCK.lock();
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
            WRITE_LOCK.unlock();
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

    public void removeSubscriber(Object subscriber, Method handle, Class eventType) {
        Assert.notNull(subscriber, "subscriber must not be null");
        Assert.notNull(handle, "subscriber's handle method must not be null");
        WRITE_LOCK.lock();
        try {
            LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
            if (subscribers == null || subscribers.isEmpty())
                return;
            Subscriber obj = new Subscriber(subscriber, handle, null, 0);
            for (Subscriber suber : subscribers) {
                if (suber.equals(obj)) {
                    subscribers.remove(suber);
                    return;
                }
            }
        } finally {
            WRITE_LOCK.unlock();
        }
    }

    public void removeSubscriber(Object subscriber, Class eventType) {
        Assert.notNull(subscriber, "subscriber must not be null");
        WRITE_LOCK.lock();
        try {
            LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
            if (subscribers == null || subscribers.isEmpty())
                return;
            for (Subscriber suber = subscribers.getLast(); suber != null; ) {
                if (suber.subscriber.equals(subscriber)) {
                    subscribers.remove(suber);
                }
                suber = subscribers.isEmpty() ? null : subscribers.getLast();
            }
        } finally {
            WRITE_LOCK.unlock();
        }
    }
}
