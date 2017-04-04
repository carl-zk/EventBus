package moc.oreh.eventbus.support;

import moc.oreh.eventbus.annotation.SubscribeMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hero on 17-4-3.
 */
public class Subscriber {
    private Object subscriber;
    private Method handle;
    private SubscribeMode mode;
    private int priority;

    public Subscriber(Object subscriber, Method handle, SubscribeMode mode, int priority) {
        this.subscriber = subscriber;
        this.handle = handle;
        this.mode = mode;
        this.priority = priority;
    }

    public SubscribeMode getMode() {
        return mode;
    }

    public int getPriority() {
        return priority;
    }

    public void onEvent(Object event) throws InvocationTargetException, IllegalAccessException {
        handle.invoke(subscriber, event);
    }
}
