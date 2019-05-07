package eventbus.annotation;

/**
 * subscriber listens on event in different mode.
 *
 * @author carl
 */
public enum SubscribeMode {
    /**
     * when event happens,
     * it will pull a thread from a thread pool to let subscribers to handle this.
     */
    ASYNC,
    /**
     * when event happens,
     * it will let subscribers handle this in current thread.
     */
    SYNC,
    /**
     * when event happens,
     * it will pull a thread from a single thread pool to let subscribers to handle this.
     */
    BACKGROUND
}
