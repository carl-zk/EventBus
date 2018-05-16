package eventbus;

import eventbus.annotation.Subscribe;
import eventbus.support.EventBusException;
import eventbus.support.EventTask;
import eventbus.support.Subscriber;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    protected Map<Class, List<Subscriber>> allSubscribers = new ConcurrentHashMap<>(16);
    protected ThreadPoolExecutor backgroundExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    protected ThreadPoolExecutor asyncExecutor;

    public EventBus() {
        asyncExecutor = new ThreadPoolExecutor(8, 64,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public EventBus(int corePoolSize, int maximumPoolSize, long keepAliveTime) {
        asyncExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * publish an event
     *
     * @param event an Event Object, can any customized class object
     */
    public void publish(final Object event) {
        Class eventType = requireKnownEventType(event.getClass());
        List<Subscriber> subscribers = this.allSubscribers.get(eventType);
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
                asyncExecutor.execute(new EventTask(event, subscriber));
                break;
            case BACKGROUND:
                backgroundExecutor.execute(new EventTask(event, subscriber));
                break;
            case SYNC:
                new EventTask(event, subscriber).run();
                break;
            default:
                throw new EventBusException("wrong event bus mode : " + subscriber.getMode());
        }
    }

    public <T> void addSubscriber(Class<T> eventType, Subscriber subscriber) {
        List<Subscriber> subscribers = this.allSubscribers.get(eventType);
        if (subscribers == null) {
            subscribers = new LinkedList();
            this.allSubscribers.put(eventType, subscribers);
        }
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
            List<Subscriber> subscribers = this.allSubscribers.get(eventType);
            if (subscribers == null) {
                subscribers = new LinkedList<>();
                this.allSubscribers.put(eventType, subscribers);
            }
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
        while (backgroundExecutor.isTerminating()) ;
        while (asyncExecutor.isTerminating()) ;
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
