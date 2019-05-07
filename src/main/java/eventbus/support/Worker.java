package eventbus.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author carl
 */
public class Worker implements Runnable {
    private static Logger log = LoggerFactory.getLogger(Worker.class);

    private final Subscriber subscriber;
    private final Object event;

    @Override
    public void run() {
        try {
            subscriber.handle(event);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("EventBus worker failed: subscriber={}, event={}.", subscriber, event.getClass().getName(), e);
            throw new EventBusException("EventBus worker failed");
        }
    }

    public Worker(Object event, Subscriber subscriber) {
        this.subscriber = subscriber;
        this.event = event;
    }
}
