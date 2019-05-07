package eventbus.spring.handler;

import eventbus.annotation.SubscribeMode;
import eventbus.spring.TxSynchronization;
import eventbus.support.Worker;
import eventbus.support.Subscriber;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;

/**
 * @author carl
 */
public class BackgroundEventHandler implements EventHandler {
    private ExecutorService backgroundExecutor;

    public BackgroundEventHandler(ExecutorService backgroundExecutor) {
        this.backgroundExecutor = backgroundExecutor;
    }

    @Override
    public void handle(Object event, Subscriber subscriber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TxSynchronization(event, subscriber, backgroundExecutor));
        } else {
            backgroundExecutor.execute(new Worker(event, subscriber));
        }
    }

    @Override
    public boolean supportSubscribeMode(SubscribeMode subscribeMode) {
        return SubscribeMode.BACKGROUND == subscribeMode;
    }
}
