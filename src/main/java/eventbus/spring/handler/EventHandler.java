package eventbus.spring.handler;

import eventbus.annotation.SubscribeMode;
import eventbus.support.Subscriber;

/**
 * @author carl
 */
public interface EventHandler {
    /**
     * handle event
     *
     * @param event
     * @param subscriber
     */
    void handle(Object event, Subscriber subscriber);

    /**
     * support which mode
     *
     * @param subscribeMode
     * @return
     */
    boolean supportSubscribeMode(SubscribeMode subscribeMode);
}
