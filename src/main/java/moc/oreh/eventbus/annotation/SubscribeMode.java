package moc.oreh.eventbus.annotation;

/**
 * 监听消息的模式
 * SYNC: 同步监听,即在同一个线程顺序执行
 * BACKGROUND: 后台执行,即交给后台唯一一个线程执行
 * ASYNC: 异步执行,即交给后台一个线程池去执行
 *
 * Created by hero on 17-4-3.
 */
public enum SubscribeMode {
    SYNC,    // publisher direct call subscriber
    BACKGROUND,      // deliver task to a single thread
    ASYNC      // deliver task to a thread pool
}
