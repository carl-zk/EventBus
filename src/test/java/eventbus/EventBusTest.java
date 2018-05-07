package eventbus;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

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
    public void test(){
        eventBus.publish(new AppConfig.LoginEvent("发布一个消息"));
    }
}
