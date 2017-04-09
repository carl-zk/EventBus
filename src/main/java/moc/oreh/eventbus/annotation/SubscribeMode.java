package moc.oreh.eventbus.annotation;

/**
 * Created by hero on 17-4-3.
 */
public enum SubscribeMode {
    SYNC,    // publisher direct call subscriber
    BACKGROUND,      // deliver task to a single thread
    ASYNC      // deliver task to a thread pool
}
