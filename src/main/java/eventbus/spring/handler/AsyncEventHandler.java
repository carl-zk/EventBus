package eventbus.spring.handler;

import eventbus.annotation.SubscribeMode;
import eventbus.spring.tx.TxSynchronization;
import eventbus.support.Subscriber;
import eventbus.support.Worker;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.ExecutorService;

/**
 * @author carl
 */
public class AsyncEventHandler implements EventHandler {
    private ExecutorService asyncExecutor;

    public AsyncEventHandler(ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void handle(Object event, Subscriber subscriber) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TxSynchronization(event, subscriber, asyncExecutor));
        } else {
            asyncExecutor.execute(new Worker(event, subscriber));
        }
    }

    @Override
    public boolean supportSubscribeMode(SubscribeMode subscribeMode) {
        return SubscribeMode.ASYNC == subscribeMode;
    }
}
