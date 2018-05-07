package eventbus.support;

import eventbus.annotation.SubscribeMode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by hero on 17-4-3.
 */
public class Subscriber {
    public Object subscriber;
    public Method handle;
    public SubscribeMode mode;
    public int priority;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subscriber)) return false;

        Subscriber that = (Subscriber) o;

        if (!subscriber.equals(that.subscriber)) return false;
        return handle.equals(that.handle);

    }

    @Override
    public int hashCode() {
        int result = subscriber.hashCode();
        result = 31 * result + handle.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "subscriber=" + subscriber.getClass().getName() +
                ", handle=" + handle.getName() +
                ", mode=" + mode +
                ", priority=" + priority +
                '}';
    }
}
