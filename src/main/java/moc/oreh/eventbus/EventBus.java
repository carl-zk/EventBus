package moc.oreh.eventbus;

import moc.oreh.eventbus.annotation.Subscribe;
import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventBusException;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author hero on 17-4-2.
 */
public class EventBus {
    protected Map<Class, LinkedList<Subscriber>> retrieverCache = new ConcurrentHashMap<>(64);
    protected ThreadPoolExecutor backgroundExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    protected ThreadPoolExecutor asyncExecutor;

    public EventBus() {
        asyncExecutor = new ThreadPoolExecutor(8, 64, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public EventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        // SubscribeMode.ASYNC
        asyncExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
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
            throw new EventBusException(event.getClass() + " unknown event");
        for (Subscriber subscriber : subscribers) {
            if (subscriber.getMode() == SubscribeMode.SYNC) {
                new EventTask(subscriber, event).run();
            } else {
                invokeSubscriber(event, subscriber);
            }
        }
    }

    protected void invokeSubscriber(Object event, Subscriber subscriber) {
        switch (subscriber.getMode()) {
            case ASYNC:
                asyncExecutor.execute(new EventTask(subscriber, event));
                break;
            case BACKGROUND:
                backgroundExecutor.execute(new EventTask(subscriber, event));
                break;
        }
    }

    protected void processBean(Object bean) {
        Class clazz = proxyBeanUnwrap(bean);
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Subscribe.class)) {
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                Class[] event = method.getParameterTypes();
                if (event == null || event.length != 1)
                    throw new IllegalArgumentException("method's args must one and only one ---> the event object only; " + method.getName() + " in " + clazz.getName());
                Class eventType = event[0];
                LinkedList<Subscriber> subscribers = retrieverCache.get(eventType);
                if (subscribers == null) {
                    subscribers = new LinkedList<>();
                    retrieverCache.put(eventType, subscribers);
                }
                insertSubscriber(new Subscriber(bean, method, subscribe.mode(), subscribe.priority()), subscribers);
            }
        }
    }

    protected Class proxyBeanUnwrap(Object bean) {
        return bean.getClass();
    }

    public void destroy() {
        backgroundExecutor.shutdown();
        asyncExecutor.shutdown();
        while (backgroundExecutor.isTerminating()) ;
        while (asyncExecutor.isTerminating()) ;
        retrieverCache.clear();
    }

    private void insertSubscriber(Subscriber subscriber, LinkedList<Subscriber> subscribers) {
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
}
