package eventbus;


import eventbus.annotation.Subscribe;
import eventbus.annotation.SubscribeMode;
import eventbus.support.EventBusException;
import eventbus.support.EventTask;
import eventbus.support.Subscriber;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * example:
 * <code>
 *
 * @Service class LogService {
 * @Subscribe(mode=SubscribeMode.ASYNC) public void log(LoginEvent event) {
 * log(event.getUsername());
 * }
 * }
 * </code>
 * `LoginEvent` 为你自定义的类,可以包含任何信息,例如 username 等.
 * <p>
 * 在 UserService 中通过 eventBus.publish(new LoginEvent("Joker")) 来发布消息, 这样订阅该类消息的 logService 就
 * 能够异步处理事件了.
 */
public class EventBus {
    protected Map<Class, List<Subscriber>> SUBSCRIBERS = new ConcurrentHashMap<>(16);
    protected ThreadPoolExecutor backgroundExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    protected ThreadPoolExecutor asyncExecutor;

    public EventBus() {
        asyncExecutor = new ThreadPoolExecutor(8, 64, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public EventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        // SubscribeMode.ASYNC
        asyncExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * publish an event
     *
     * @param event an Event Object, can any customized class object
     */
    public void publish(Object event) {
        Class eventType = event.getClass();
        List<Subscriber> subscribers = SUBSCRIBERS.get(eventType);
        if (subscribers == null)
            throw new EventBusException("unknown event : " + event.getClass());
        for (Subscriber subscriber : subscribers) {
            if (subscriber.getMode() == SubscribeMode.SYNC) {
                new EventTask(subscriber, event).run();
            } else {
                invokeSubscriber(event, subscriber);
            }
        }
    }

    public <T> void addSubscriber(Class<T> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = SUBSCRIBERS.get(eventType);
        if (subscribers == null) {
            subscribers = new LinkedList();
            SUBSCRIBERS.put(eventType, subscribers);
        }
        insertSubscriber(subscriber, subscribers);
    }

    public <T> void removeSubscriber(Class<T> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = SUBSCRIBERS.get(eventType);
        if (subscribers != null) {
            subscribers.remove(subscriber);
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
                    throw new EventBusException("method's args must one and only one ---> the event object only; " + method.getName() + " in " + clazz.getName());
                Class eventType = event[0];
                List<Subscriber> subscribers = SUBSCRIBERS.get(eventType);
                if (subscribers == null) {
                    subscribers = new LinkedList<>();
                    SUBSCRIBERS.put(eventType, subscribers);
                }
                method.setAccessible(true);
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
        SUBSCRIBERS.clear();
    }

    private void insertSubscriber(Subscriber subscriber, List<Subscriber> subscribers) {
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
