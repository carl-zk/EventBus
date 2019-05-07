package eventbus;

import eventbus.annotation.SubscribeMode;
import eventbus.common.TestConfiguration;
import eventbus.support.Subscriber;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Method;

/**
 * @author carl
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
public class EventBusTest {
    private Logger log = LoggerFactory.getLogger(EventBusTest.class);

    @Autowired
    EventBus eventBus;

    @Autowired
    TestConfiguration.LoginSubscriber loginSubscriber;

    @Test
    public void testPublish() {
        eventBus.publish(new TestConfiguration.LoginEvent("小明"));
    }

    @Test
    public void addNewEventType() throws NoSuchMethodException {
        Method method = EventBusTest.class.getMethod("anotherHandle", AnotherEvent.class);
        EventBusTest eventBusTest = new EventBusTest();

        Subscriber subscriber = new Subscriber(eventBusTest, method, SubscribeMode.ASYNC, 0);

        eventBus.addSubscriber(AnotherEvent.class, subscriber);
        eventBus.publish(new AnotherEvent("dynamic register new subscriber"));

        eventBus.removeSubscriber(AnotherEvent.class, subscriber);
        eventBus.publish(new AnotherEvent("dynamic remove subscriber"));
    }

    public void anotherHandle(AnotherEvent anotherEvent) {
        log.info("anotherHandle: {}", anotherEvent.value);
    }

    class AnotherEvent {
        public String value;

        public AnotherEvent(String value) {
            this.value = value;
        }
    }
}
