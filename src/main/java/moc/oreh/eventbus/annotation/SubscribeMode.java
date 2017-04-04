package moc.oreh.eventbus.annotation;

/**
 * Created by hero on 17-4-3.
 */
public enum SubscribeMode {
    FOLLOW,    // publisher直接调subscriber
    SYNC,      // 交给一个单独线程
    ASYNC      // 交给一个线程池
}
