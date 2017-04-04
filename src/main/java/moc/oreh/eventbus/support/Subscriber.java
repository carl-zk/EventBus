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

    public Subscriber(Object subscriber, Method handle, SubscribeMode mode) {
        this.subscriber = subscriber;
        this.handle = handle;
        this.mode = mode;
    }

    public SubscribeMode getMode() {
        return mode;
    }

    public void onEvent(Object event) throws InvocationTargetException, IllegalAccessException {
        handle.invoke(subscriber, event);
    }
}
