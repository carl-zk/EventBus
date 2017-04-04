package moc.oreh.eventbus;

import moc.oreh.eventbus.support.DefaultEventMulticaster;
import moc.oreh.eventbus.support.EventMulticaster;

/**
 * @author hero on 17-4-2.
 */
public abstract class EventBus {
    protected EventMulticaster eventMulticaster;

    public EventBus() {
        eventMulticaster = new DefaultEventMulticaster();
    }

    /**
     * publish an event
     *
     * @param event an Event Object, can any customized class object
     */
    public void publish(Object event) {
        eventMulticaster.multicastEvent(event);
    }

    public void destroy() {
        eventMulticaster.destroy();
    }
}
