import junit.framework.Assert;
import moc.oreh.eventbus.EventBus;
import moc.oreh.eventbus.EventBusException;
import moc.oreh.eventbus.annotation.SubscribeMode;
import moc.oreh.eventbus.support.EventTask;
import moc.oreh.eventbus.support.Subscriber;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * Created by hero on 17-4-5.
 */
public class EventBusTest {
    LinkedList<Subscriber> subscribers = new LinkedList<>();

    public void addSubscriber(Subscriber newSubscriber) {
        synchronized (EventBus.class) {

            for (Subscriber suber : subscribers) {
                if (newSubscriber.equals(suber))
                    throw new EventBusException("Subscriber " + newSubscriber.getClass() + " already registered to event");
            }
            System.out.println("done");
        }
    }

    @Test(expected = EventBusException.class)
    public void testLinkListContains() throws NoSuchMethodException {
        Object o = new Object();
        Method m = Object.class.getDeclaredMethod("toString", null);
        Subscriber subscriber = new Subscriber(o, m, SubscribeMode.ASYNC, 0);
        subscribers.addLast(subscriber);
        Subscriber newSub = new Subscriber(o, m, SubscribeMode.ASYNC, 0);
        addSubscriber(newSub);
    }

    @Test
    public void testClazz() {
        LinkedList<Class> list = new LinkedList<>();
        LinkedList<Class<?>> linkedList = new LinkedList<>();

        list.addLast(EventBusTest.class);
        linkedList.addLast(EventBusTest.class);

        Assert.assertEquals(list.getFirst().getName(), linkedList.getFirst().getName());
    }

    @Test
    public void testTrue() {
        String s = null;
        moc.oreh.eventbus.util.Assert.requireNotNull(s);
    }

    @Test
    public void testObj() {
        EventTask eventTask = new EventTask(null, null);
        Object o = eventTask;
        System.out.println(o.getClass());
    }

}
