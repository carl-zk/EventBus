package moc.oreh.eventbus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * example:
 * <code>
 *      @Subscribe
 *      public void login(LoginEvent event){
 *          ... process user login event stuff ...
 *      }
 * </code>
 * `LoginEvent` 为你自定义的类,可以包含任何信息,例如 username 等.
 *
 * 在任何地方都可以通过 eventBus.publish(event) 来发布消息, 这样订阅该类消息的 subscriber 就
 * 能够接收并处理事件了.
 *
 * Created by hero on 17-4-2.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {
    SubscribeMode mode() default SubscribeMode.SYNC;
    int priority() default 0;    // 0 at the first
}
