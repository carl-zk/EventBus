package eventbus;

import eventbus.annotation.SubscribeMode;
import eventbus.support.Subscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Method;

/**
 * Created by hero on 14/04/2018.
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class EventBusTest {

    @Autowired
    EventBus eventBus;

    @Autowired
    AppConfig.SubUserLoginEvent subUserLoginEvent;

    @Test
    public void test() {
        eventBus.publish(new AppConfig.LoginEvent("小明"));
    }

    @Test
    public void addNewEventType() throws NoSuchMethodException {
        Method method = EventBusTest.class.getMethod("processE", E.class);
        EventBusTest eventBusTest = new EventBusTest();

        eventBus.addSubscriber(E.class, new Subscriber(eventBusTest, method, SubscribeMode.ASYNC, 0));
        eventBus.publish(new E("hello world"));

        eventBus.removeSubscriber(E.class, new Subscriber(eventBusTest, method, SubscribeMode.ASYNC, 0));
        eventBus.publish(new E("hello world"));
    }

    public void processE(E e) {
        System.out.println("catch new event E " + e.value);
    }

    class E {
        public String value;

        public E(String value) {
            this.value = value;
        }
    }
}
