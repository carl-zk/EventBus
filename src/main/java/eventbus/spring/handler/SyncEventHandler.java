package eventbus.spring.handler;

import eventbus.annotation.SubscribeMode;
import eventbus.spring.handler.EventHandler;
import eventbus.support.Worker;
import eventbus.support.Subscriber;

/**
 * @author carl
 */
public class SyncEventHandler implements EventHandler {
    @Override
    public void handle(Object event, Subscriber subscriber) {
        new Worker(event, subscriber).run();
    }

    @Override
    public boolean supportSubscribeMode(SubscribeMode subscribeMode) {
        return SubscribeMode.SYNC == subscribeMode;
    }
}
