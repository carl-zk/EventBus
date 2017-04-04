package moc.oreh.eventbus.support;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by hero on 17-4-3.
 */
public class EventTask implements Runnable {
    private Logger logger = LogManager.getLogger(EventTask.class);

    private final Subscriber subscriber;
    private final Object event;

    public void run() {
        try {
            subscriber.onEvent(event);
        } catch (Exception ex) {
            logger.error("[EventTask failed]", ex);
        }
    }

    public EventTask(Subscriber subscriber, Object event) {
        this.subscriber = subscriber;
        this.event = event;
    }
}
