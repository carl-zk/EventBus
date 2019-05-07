package eventbus.spring.handler;

import eventbus.annotation.SubscribeMode;
import eventbus.support.Subscriber;
import eventbus.support.Worker;

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
