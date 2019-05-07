package eventbus.spring.tx;

import eventbus.support.Worker;
import eventbus.support.Subscriber;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.concurrent.ExecutorService;

/**
 * @author carl
 */
public class TxSynchronization extends TransactionSynchronizationAdapter {
    private Object event;
    private Subscriber subscriber;
    private ExecutorService executor;

    @Override
    public void afterCommit() {
        executor.execute(new Worker(event, subscriber));
    }

    public TxSynchronization(Object event, Subscriber subscriber, ExecutorService executor) {
        this.event = event;
        this.subscriber = subscriber;
        this.executor = executor;
    }
}
