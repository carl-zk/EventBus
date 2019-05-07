package eventbus;

import eventbus.annotation.Subscribe;
import eventbus.support.EventBusException;
import eventbus.support.Worker;
import eventbus.support.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author carl
 */
public class EventBus {
    private static Logger log = LoggerFactory.getLogger(EventBus.class);

    protected Map<Class, List<Subscriber>> allSubscribers;
    protected ExecutorService backgroundExecutor;
    protected ExecutorService asyncExecutor;

    {
        allSubscribers = new ConcurrentHashMap<>(16);

        ThreadPoolExecutorFactoryBean backgroundThreadFactory = new ThreadPoolExecutorFactoryBean();
        backgroundThreadFactory.setThreadNamePrefix("event-bus-background-");
        backgroundThreadFactory.setThreadGroupName("event-bus");

        backgroundExecutor = Executors.newSingleThreadExecutor(backgroundThreadFactory);
    }

    public EventBus() {
        this(4, 8, 60);
    }

    public EventBus(int corePoolSize, int maxPoolSize, int keepAliveSeconds) {
        ThreadPoolExecutorFactoryBean poolFactory = new ThreadPoolExecutorFactoryBean();

        poolFactory.setThreadNamePrefix("event-bus-async-");
        poolFactory.setThreadGroupName("event-bus");
        poolFactory.setCorePoolSize(corePoolSize);
        poolFactory.setMaxPoolSize(maxPoolSize);
        poolFactory.setKeepAliveSeconds(keepAliveSeconds);

        asyncExecutor = Executors.newCachedThreadPool(poolFactory);
    }

    /**
     * publish an event
     *
     * @param event an Event Object, can any customized class object
     */
    public void publish(final Object event) {
        Class eventType = requireKnownEventType(event.getClass());
        List<Subscriber> subscribers = getSubscribersByType(eventType);
        subscribers.stream().forEach(subscriber -> handleEvent(event, subscriber));
    }

    private <T> T requireKnownEventType(T eventType) {
        if (allSubscribers.containsKey(eventType)) {
            return eventType;
        }
        throw new EventBusException("unknown event type : " + eventType);
    }

    protected void handleEvent(Object event, Subscriber subscriber) {
        switch (subscriber.getMode()) {
            case ASYNC:
                asyncExecutor.execute(new Worker(event, subscriber));
                break;
            case BACKGROUND:
                backgroundExecutor.execute(new Worker(event, subscriber));
                break;
            case SYNC:
                new Worker(event, subscriber).run();
                break;
            default:
                throw new EventBusException("wrong event bus mode : " + subscriber.getMode());
        }
    }

    protected List<Subscriber> getSubscribersByType(Class eventType) {
        List<Subscriber> subscribers = this.allSubscribers.get(eventType);
        if (subscribers == null) {
            subscribers = new LinkedList();
            this.allSubscribers.put(eventType, subscribers);
        }
        return subscribers;
    }

    public <T> void addSubscriber(Class<T> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = getSubscribersByType(eventType);
        insertSubscriber(subscriber, subscribers);
    }

    public <T> void removeSubscriber(Class<T> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = this.allSubscribers.get(eventType);
        if (subscribers != null) {
            subscribers.remove(subscriber);
        }
    }

    protected void processBean(final Object bean) {
        Class clazz = getTargetClass(bean);
        List<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(hasSubscriber())
                .collect(Collectors.toList());
        methods.stream().forEach(method -> {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            Class eventType = requireStrictMethod(method);
            List<Subscriber> subscribers = getSubscribersByType(eventType);
            method.setAccessible(true);
            insertSubscriber(new Subscriber(bean, method, subscribe.mode(), subscribe.priority()), subscribers);
        });
    }

    private Class requireStrictMethod(Method method) {
        Class[] eventType = method.getParameterTypes();
        if (eventType == null || eventType.length != 1)
            throw new EventBusException("args are too many : " + method);
        return eventType[0];
    }

    private Predicate<Method> hasSubscriber() {
        return method -> null != method.getAnnotation(Subscribe.class);
    }

    protected Class getTargetClass(Object bean) {
        return bean.getClass();
    }

    public void destroy() {
        backgroundExecutor.shutdown();
        asyncExecutor.shutdown();
        allSubscribers.clear();
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
