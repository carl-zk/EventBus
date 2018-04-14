package moc.oreh.eventbus.support;

import moc.oreh.eventbus.annotation.SubscribeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by hero on 17-4-3.
 */
public class EventTask implements Runnable {
    private Logger logger = LoggerFactory.getLogger(EventTask.class);

    private final Subscriber subscriber;
    private final Object event;

    public void run() {
        try {
            subscriber.onEvent(event);
        } catch (Exception e) {
            logger.error("EventBus Task Failed: subscriber=" + subscriber + ", event=" + event, e);
            if (subscriber.getMode() == SubscribeMode.SYNC)
                throw new EventBusException("event publish failed.", e);
        }
    }

    public EventTask(Subscriber subscriber, Object event) {
        this.subscriber = subscriber;
        this.event = event;
    }
}
