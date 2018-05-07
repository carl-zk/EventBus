package eventbus.annotation;

/**
 * 监听消息的模式
 * ASYNC: 异步执行,交给后台线程池去执行
 * SYNC: 同步监听,在当前线程执行
 * BACKGROUND: 后台执行,所有BACKGROUND事件都由单独一个线程处理
 */
public enum SubscribeMode {
    ASYNC,
    SYNC,
    BACKGROUND
}
